package gr.imsi.athenarc.xtremexpvisapi.service.dataService.v2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.extern.java.Log;
import tagbio.umap.Umap;
import gr.imsi.athenarc.xtremexpvisapi.domain.Metadata.DatasetType;
import gr.imsi.athenarc.xtremexpvisapi.domain.Metadata.MetadataResponseV2;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.DataRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.DataResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.Column;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.DataSource;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.FileType;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
    public CompletableFuture<DataResponse> executeDataRequest(DataRequest request, String authorization)
            throws SQLException, Exception {
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
    public CompletableFuture<MetadataResponseV2> getFileMetadata(DataSource dataSource, String authorization)
            throws Exception, SQLException {
        return dataQueryHelper.getFilePathForDataset(dataSource, authorization).thenApply(filePath -> {
            try {
                // Build a simple SELECT query to get column information
                String sql = "SELECT * FROM ";

                // Detect file type and build appropriate query
                FileType fileType = dataQueryHelper.detectFileType(filePath);

                sql += dataQueryHelper.getFileTypeSQL(fileType, filePath);
                sql += " LIMIT 10"; // Just get a few rows for metadata analysis

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

                // Temporary code for rawvis metadata. Figure out how to cache this.
                if ("rawvis".equalsIgnoreCase(dataSource.getFormat())) {
                    // 1. Detect delimiter
                    String delimiter;
                    try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
                        String firstLine = br.readLine();
                        delimiter = firstLine.contains("\t") ? "\t" : ",";
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to read file", e);
                    }

                    // 2. Read CSV into DuckDB
                    String createTable = String.format(
                            "CREATE TABLE data AS SELECT * FROM read_csv_auto('%s', delim='%s', DATEFORMAT='auto', HEADER=TRUE);",
                            filePath.replace("\\", "\\\\"), delimiter.equals("\t") ? "\\t" : delimiter);
                    log.info("Create Table: " + createTable);
                    Statement stmt = duckdbConnection.createStatement();
                    stmt.execute(createTable);

                    // 3. Get metadata
                    String rawVisSql = "SELECT " +
                            "MIN(radio_timestamp) AS min_time, " +
                            "MAX(radio_timestamp) AS max_time, " +
                            "MIN(longitude) AS queryXMin, " +
                            "MAX(longitude) AS queryXMax, " +
                            "MIN(latitude) AS queryYMin, " +
                            "MAX(latitude) AS queryYMax, " +
                            "FROM data";
                    ResultSet rs = stmt.executeQuery(rawVisSql);
                    if (rs.next()) {
                        log.info("RawVis SQL: " + rawVisSql);
                        Timestamp minTs = rs.getTimestamp("min_time");
                        Timestamp maxTs = rs.getTimestamp("max_time");
                        metadataResponse.setTimeMin(minTs != null ? minTs.getTime() : 0);
                        metadataResponse.setTimeMax(maxTs != null ? maxTs.getTime() : 0);
                        metadataResponse.setQueryXMin(rs.getDouble("queryXMin"));
                        metadataResponse.setQueryXMax(rs.getDouble("queryXMax"));
                        metadataResponse.setQueryYMin(rs.getDouble("queryYMin"));
                        metadataResponse.setQueryYMax(rs.getDouble("queryYMax"));
                        // TODO: Get dimensions, measure0 and measure1 either from other source.
                        metadataResponse
                                .setDimensions(Arrays.asList("net_type", "mcc_nr", "mnc_nr", "provider", "eci_cid"));
                    }
                    rs.close();
                    stmt.close();
                }

                // Set metadata response fields
                metadataResponse.setOriginalColumns(convertedColumns);
                metadataResponse.setTotalItems(totalItems);
                metadataResponse.setDatasetType(datasetType);
                metadataResponse.setHasLatLonColumns(dataQueryHelper.hasLatLonColumns(convertedColumns));

                if (!timeColumns.isEmpty()) {
                    metadataResponse.setTimeColumn(timeColumns);
                }

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
