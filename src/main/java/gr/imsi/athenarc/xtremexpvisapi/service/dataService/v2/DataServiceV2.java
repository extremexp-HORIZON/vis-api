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

import java.io.IOException;
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
    private javax.sql.DataSource dataSource;

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
                        try (Connection connection = dataSource.getConnection();
                                Statement statement = connection.createStatement()) {
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
                        try (Connection connection = dataSource.getConnection();
                                Statement statement = connection.createStatement()) {
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
            try (Connection connection = dataSource.getConnection();
                    Statement statement = connection.createStatement()) {

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
            try (
                    Connection connection = this.dataSource.getConnection();
                    Statement statement = connection.createStatement()) {
                FileType fileType = dataQueryHelper.detectFileType(filePath);

                String sql = "SELECT * FROM " + dataQueryHelper.getFileTypeSQL(fileType, filePath) + " LIMIT 10";
                ResultSet resultSet = statement.executeQuery(sql);

                MetadataResponseV2 metadataResponse = new MetadataResponseV2();

                var metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();
                List<String> timeColumns = new ArrayList<>();
                List<Column> convertedColumns = new ArrayList<>();

                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    String columnType = dataQueryHelper.mapSqlTypeToString(metaData.getColumnType(i));
                    convertedColumns.add(new Column(columnName, columnType));

                    if (dataQueryHelper.isTimeColumn(columnType)) {
                        timeColumns.add(columnName);
                    }
                }

                String countSql = "SELECT COUNT(*) as total FROM " + dataQueryHelper.getFileTypeSQL(fileType, filePath);
                ResultSet countResult = statement.executeQuery(countSql);
                int totalItems = 0;
                if (countResult.next()) {
                    totalItems = countResult.getInt("total");
                }

                // Check for lat/lon columns
                // Set metadata response fields

                // Detect dataset type based on time columns and data ordering
                DatasetType datasetType = dataQueryHelper.detectDatasetType(resultSet, timeColumns, statement, sql);

                // RawVis specific metadata

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
                    dataQueryHelper.populateRawvisMeta(connection, filePath, dataSource,
                            convertedColumns, metadataMapResponse);
                }
                try {
                    sql = sql.replace(" LIMIT 10", ""); // Remove limit for summarization

                    String summarizeSql = "SUMMARIZE " + sql.substring(sql.indexOf("FROM")); // ensures it matches file
                                                                                             // loading
                    ResultSet summarizeResult = statement.executeQuery(summarizeSql);
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
                } catch (SQLException summarizeEx) {
                    log.warning("Could not summarize file contents: " + summarizeEx.getMessage());
                }

                resultSet.close();
                countResult.close();

                return ifRawVis ? metadataMapResponse : metadataResponse;

            } catch (SQLException e) {
                throw new RuntimeException("Failed to get file metadata", e);
            } catch (IOException e) {
                throw new RuntimeException("Failed to get file metadata due to I/O error", e);
            }
        });
    }

    public String[] fetchRow(DataSource dataSource, String objectId) throws Exception, SQLException {

        try (Connection connection = this.dataSource.getConnection();
                Statement statement = connection.createStatement()) {

            String sql = String.format("SELECT * FROM read_csv('%s') WHERE id = '%s'", dataSource.getSource(),
                    objectId);
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
        Umap umap = new Umap();
        umap.setNumberComponents(2);
        umap.setNumberNearestNeighbours(15);
        umap.setThreads(1);
        return umap.fitTransform(data);
    }

}
