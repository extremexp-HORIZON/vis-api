package gr.imsi.athenarc.xtremexpvisapi.service.dataService.v2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.extern.java.Log;
import tagbio.umap.Umap;
import gr.imsi.athenarc.xtremexpvisapi.domain.Metadata.DatasetType;
import gr.imsi.athenarc.xtremexpvisapi.domain.Metadata.MetadataMapResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.Metadata.MetadataResponseV2;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.DataRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.DataResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.MapDataRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.MapDataResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.TimeSeriesDataRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.TimeSeriesDataResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.Column;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.DataSource;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.FileType;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.UnivariateDataPoint;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
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

    private String buildCountQuery(String originalQuery) {
        // Remove existing LIMIT and OFFSET
        String cleanedQuery = originalQuery
                .replaceAll("(?i)LIMIT\\s+\\d+", "")
                .replaceAll("(?i)OFFSET\\s+\\d+", "");

        // Wrap in a subquery to count total rows
        return "SELECT COUNT(*) FROM (" + cleanedQuery + ") AS total_count_subquery";
    }

    @Async
    public CompletableFuture<DataResponse> executeDataRequest(DataRequest request, String authorization)
            throws SQLException, Exception {

        // Check if the request is for a map view or time series view
        log.info("Request: " + request);

        if (request instanceof MapDataRequest) {
            final MetadataResponseV2 metadataResponse = ((MapDataRequest) request).getMapMetadata() == null
                    ? getFileMetadata(request.getDataSource(), authorization).get()
                    : ((MapDataRequest) request).getMapMetadata();
            // Build the SQL query for the map view
            return dataQueryHelper
                    .buildMapQuery((MapDataRequest) request, authorization, (MetadataMapResponse) metadataResponse)
                    .thenCompose(sql -> {
                        try {
                            Statement statement = duckdbConnection.createStatement();
                            ResultSet resultSet = statement.executeQuery(sql);

                            MapDataResponse response = new MapDataResponse();

                            dataQueryHelper.processMapQueryResults(resultSet, (MapDataRequest) request,
                                    (MetadataMapResponse) metadataResponse, response, 9);

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
        } else if (request instanceof TimeSeriesDataRequest) {
            final MetadataResponseV2 metadataResponse = ((TimeSeriesDataRequest) request).getMapMetadata() == null
                    ? getFileMetadata(request.getDataSource(), authorization).get()
                    : ((TimeSeriesDataRequest) request).getMapMetadata();
            // Build the SQL query for the time series view
            return dataQueryHelper
                    .buildTimeSeriesQuery((TimeSeriesDataRequest) request, authorization,
                            (MetadataMapResponse) metadataResponse)
                    .thenCompose(sql -> {
                        try {
                            Statement statement = duckdbConnection.createStatement();
                            ResultSet resultSet = statement.executeQuery(sql);

                            TimeSeriesDataResponse response = new TimeSeriesDataResponse();
                            List<UnivariateDataPoint> timeSeriesPoints = new ArrayList<>();

                            while (resultSet.next()) {
                                long bucketEpoch = resultSet.getLong("time_bucket")
                                        * ((TimeSeriesDataRequest) request).getFrequency();
                                Double avgValue = resultSet.getObject("average_value") != null
                                        ? resultSet.getDouble("average_value")
                                        : null;
                                Timestamp timestamp = new Timestamp(bucketEpoch * 1000);
                                if (avgValue != null) {
                                    timeSeriesPoints.add(new UnivariateDataPoint(timestamp.getTime(), avgValue));
                                }
                            }

                            response.setTimeSeriesPoints(timeSeriesPoints);

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

        // Build the SQL query for the tabular view
        return dataQueryHelper.buildQuery(request, authorization).thenCompose(sql -> {
            try {
                Statement statement = duckdbConnection.createStatement();
                String countQuery = buildCountQuery(sql);
                ResultSet countResultSet = statement.executeQuery(countQuery);
                int totalItems = 0;
                if (countResultSet.next()) {
                    totalItems = countResultSet.getInt(1);
                }
                countResultSet.close();

                ResultSet resultSet = statement.executeQuery(sql);

                DataResponse response = dataQueryHelper.convertResultSetToTabularResponse(resultSet, sql);
                response.setTotalItems(totalItems);

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

                Statement countStatement = duckdbConnection.createStatement();
                ResultSet countResult = countStatement.executeQuery(countSql);
                int totalItems = 0;
                if (countResult.next()) {
                    totalItems = countResult.getInt("total");
                }

                // Check for lat/lon columns
                // Set metadata response fields

                // Detect dataset type based on time columns and data ordering
                DatasetType datasetType = dataQueryHelper.detectDatasetType(resultSet, timeColumns, statement, sql);

                // RawVis specific metadata

                // Set metadata response fields
                metadataResponse.setOriginalColumns(convertedColumns);
                metadataResponse.setTotalItems(totalItems);
                metadataResponse.setDatasetType(datasetType);
                metadataResponse.setHasLatLonColumns(dataQueryHelper.hasLatLonColumns(convertedColumns));

                if (!timeColumns.isEmpty()) {
                    metadataResponse.setTimeColumn(timeColumns);
                }

                Boolean ifRawVis = "rawvis".equalsIgnoreCase(dataSource.getFormat());
                MetadataMapResponse metadataMapResponse = new MetadataMapResponse(metadataResponse);
                if (ifRawVis) {
                    dataQueryHelper.populateRawvisMeta(duckdbConnection, filePath, dataSource,
                            convertedColumns, metadataMapResponse);
                }
                try {
                    sql = sql.replace(" LIMIT 10", ""); // Remove limit for summarization

                    String summarizeSql = "SUMMARIZE " + sql.substring(sql.indexOf("FROM")); // ensures it matches file
                                                                                             // loading
                    Statement summarizeStatement = duckdbConnection.createStatement();
                    ResultSet summarizeResult = summarizeStatement.executeQuery(summarizeSql);
                    List<Map<String, Object>> summaryList = new ArrayList<>();
                    int colCount = summarizeResult.getMetaData().getColumnCount();
                    while (summarizeResult.next()) {
                        Map<String, Object> summaryRow = new java.util.HashMap<>();
                        for (int i = 1; i <= colCount; i++) {
                            String colName = summarizeResult.getMetaData().getColumnName(i);
                            Object value = summarizeResult.getObject(i);
                            summaryRow.put(colName, value);
                        }
                        summaryList.add(summaryRow);
                    }
                    metadataResponse.setSummary(summaryList);
                    summarizeResult.close();
                    summarizeStatement.close();
                } catch (SQLException summarizeEx) {
                    log.warning("Could not summarize file contents: " + summarizeEx.getMessage());
                }

                // Close resources
                resultSet.close();
                countResult.close();
                statement.close();
                countStatement.close();

                return ifRawVis ? metadataMapResponse : metadataResponse;

            } catch (SQLException e) {
                throw new RuntimeException("Failed to get file metadata", e);
            }
        });
    }

    public String[] fetchRow(String datasetId, String objectId) throws Exception, SQLException {

        try {

            String csvPath = String.format("/opt/experiments/%1$s/dataset/%1$s.csv", datasetId);

            String sql = String.format("SELECT * FROM read_csv('%s') WHERE id = '%s'", csvPath, objectId);
            Statement statement = duckdbConnection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql);
            int columnCount = resultSet.getMetaData().getColumnCount();
            while (resultSet.next()) {
                String[] object = new String[columnCount];
                for (int i = 0; i < columnCount; i++) {
                    object[i] = resultSet.getString(i + 1);
                }
                return object;
            }

            resultSet.close();
            statement.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch row", e);
        }
        return null;

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
