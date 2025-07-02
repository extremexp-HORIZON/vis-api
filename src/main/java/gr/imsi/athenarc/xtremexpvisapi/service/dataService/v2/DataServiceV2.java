package gr.imsi.athenarc.xtremexpvisapi.service.dataService.v2;

import org.checkerframework.checker.units.qual.s;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.extern.java.Log;
import tagbio.umap.Umap;
import gr.imsi.athenarc.xtremexpvisapi.domain.metadata.DatasetType;
import gr.imsi.athenarc.xtremexpvisapi.domain.metadata.MetadataResponseV2;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryv2.DataRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryv2.DataResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryv2.params.Column;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryv2.params.DataSource;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryv2.params.FileType;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Log
public class DataServiceV2 {

    @Autowired
    private Connection duckdbConnection;
    private DataHelperV2 dataQueryHelper;

    @Autowired
    public DataServiceV2(DataHelperV2 dataQueryHelper) {
        this.dataQueryHelper = dataQueryHelper;
    }

    @Async
    public CompletableFuture<DataResponse> executeDataRequest(DataRequest request, String authorization) throws SQLException, Exception {
        return dataQueryHelper.buildQuery(request, authorization).thenCompose(sql -> {
            try {
                Statement statement = duckdbConnection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql);

                DataResponse response = dataQueryHelper.convertResultSetToTabularResponse(resultSet, sql);

                // Close resources
                resultSet.close();
                statement.close();

                return CompletableFuture.completedFuture(response);
            } catch (SQLException e) {
                CompletableFuture<DataResponse> failedFuture = new CompletableFuture<>();
                failedFuture.completeExceptionally(e);
                return failedFuture;
            }
        });
    }

    @Async
    public CompletableFuture<MetadataResponseV2> getFileMetadata(DataSource dataSource, String authorization) throws Exception, SQLException {
        return dataQueryHelper.getFilePathForDataset(dataSource, authorization).thenApply(filePath -> {
            try {
                log.info("Getting metadata for file: " + filePath);
                // Build a simple SELECT query to get column information
                String sql = "SELECT * FROM ";
                // Detect file type and build appropriate query
                FileType fileType = dataQueryHelper.detectFileType(filePath);
                log.info("Detected file type: " + fileType);

                sql += dataQueryHelper.getFileTypeSQL(fileType, filePath);
                sql += " LIMIT 0"; // Just get a few rows for metadata analysis

                Statement statement = duckdbConnection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql);

                MetadataResponseV2 metadataResponse = new MetadataResponseV2();

                // Get column metadata
                var metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();
                List<String> timeColumns = new ArrayList<>();

                // Convert TabularColumn to Column for the metadata response
                List<Column> convertedColumns = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    String columnType = dataQueryHelper.mapSqlTypeToString(metaData.getColumnType(i));
                    convertedColumns.add(new Column(columnName, columnType));

                    // Check if it's a time column
                    if (dataQueryHelper.isTimeColumn(columnType)) {
                        timeColumns.add(columnName);
                    }
                }

                // Count total rows
                String countSql = "SELECT COUNT(*) as total FROM ";
                countSql += dataQueryHelper.getFileTypeSQL(fileType, filePath);

                ResultSet countResult = statement.executeQuery(countSql);
                int totalItems = 0;
                if (countResult.next()) {
                    totalItems = countResult.getInt("total");
                }

                // Check for lat/lon columns
                // Set metadata response fields
                
                // Detect dataset type based on time columns and data ordering
                DatasetType datasetType = dataQueryHelper.detectDatasetType(resultSet, timeColumns, statement, sql);

                // Set metadata response fields
                metadataResponse.setOriginalColumns(convertedColumns);
                metadataResponse.setTotalItems(totalItems);
                metadataResponse.setDatasetType(datasetType);
                metadataResponse.setHasLatLonColumns(dataQueryHelper.hasLatLonColumns(convertedColumns));

                if (!timeColumns.isEmpty()) {
                    metadataResponse.setTimeColumn(timeColumns);
                }

                try {
                    sql = sql.replace(" LIMIT 0", ""); // Remove limit for summarization
            
                    String summarizeSql = "SUMMARIZE " + sql.substring(sql.indexOf("FROM")); // ensures it matches file loading
                    ResultSet summarizeResult = statement.executeQuery(summarizeSql);
                    List<Map<String, Object>> summaryList = new ArrayList<>();
                    int colCount = summarizeResult.getMetaData().getColumnCount();
                    while (summarizeResult.next()) {
                        Map<String, Object> summaryRow = new java.util.HashMap<>();
                        for (int i = 1; i <= colCount; i++) {
                            String colName = summarizeResult.getMetaData().getColumnName(i);
                            Object value = summarizeResult.getObject(i);
                            summaryRow.put(colName, value);
                        }summaryList.add(summaryRow);
                        // log.info("Summary Row: " + summaryRow.toString());
                    }
                    metadataResponse.setSummary(summaryList);
                    summarizeResult.close();
                } catch (SQLException summarizeEx) {
                    log.warning("Could not summarize file contents: " + summarizeEx.getMessage());}

                // Close resources
                resultSet.close();
                countResult.close();
                statement.close();

                return metadataResponse;

            } catch (SQLException e) {
                throw new RuntimeException("Failed to get file metadata", e);
            }
        });
    }

    public float[][] getUmap(float[][] data) {
        log.info("Performing dimensionality reduction");
        Umap umap = new Umap();
        umap.setNumberComponents(2);
        umap.setNumberNearestNeighbours(15);
        umap.setThreads(1);
        return umap.fitTransform(data);
    }

}
