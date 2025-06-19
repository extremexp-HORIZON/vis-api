package gr.imsi.athenarc.xtremexpvisapi.service.dataquery;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.Column;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.DatasetMeta;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.FileType;
import lombok.extern.java.Log;
import gr.imsi.athenarc.xtremexpvisapi.domain.Metadata.DatasetType;
import gr.imsi.athenarc.xtremexpvisapi.domain.Metadata.MetadataResponse2;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.DataRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.DataResponse;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Log
public class DataQueryService {

    @Autowired
    private Connection duckdbConnection;
    private DataQueryHelper dataQueryHelper;

    @Autowired
    public DataQueryService(DataQueryHelper dataQueryHelper) {
        this.dataQueryHelper = dataQueryHelper;
    }

    @Async
    public CompletableFuture<DataResponse> executeDataRequest(DataRequest request) throws SQLException, Exception {
        return dataQueryHelper.buildQuery(request).thenCompose(sql -> {
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
    public CompletableFuture<MetadataResponse2> getFileMetadata(DatasetMeta fileMeta) throws Exception, SQLException {
        return dataQueryHelper.getFilePathForDataset(fileMeta).thenApply(filePath -> {
            try {
                // Build a simple SELECT query to get column information
                String sql = "SELECT * FROM ";

                // Detect file type and build appropriate query
                FileType fileType = dataQueryHelper.detectFileType(filePath);

                sql += dataQueryHelper.getFileTypeSQL(fileType, filePath);
                sql += " LIMIT 10"; // Just get a few rows for metadata analysis

                Statement statement = duckdbConnection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql);

                MetadataResponse2 metadataResponse = new MetadataResponse2();

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

}
