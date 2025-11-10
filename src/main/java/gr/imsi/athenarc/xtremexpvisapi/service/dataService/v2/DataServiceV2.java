package gr.imsi.athenarc.xtremexpvisapi.service.dataService.v2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
// ...existing imports...
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.Rectangle;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.UnivariateDataPoint;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Log
public class DataServiceV2 {

    @Autowired
    private javax.sql.DataSource dataSource;

    private DataHelperV2 dataQueryHelper;

    private final Executor dataProcessingExecutor;

    private final Semaphore duckDbSemaphore;

    @Autowired
    public DataServiceV2(DataHelperV2 dataQueryHelper,
            @Qualifier("dataProcessingExecutor") Executor dataProcessingExecutor,
            @Qualifier("duckDbSemaphore") Semaphore duckDbSemaphore) {
        this.dataQueryHelper = dataQueryHelper;
        this.dataProcessingExecutor = dataProcessingExecutor;
        this.duckDbSemaphore = duckDbSemaphore;
    }

    private String buildCountQuery(String originalQuery) {
        // Remove existing LIMIT and OFFSET
        String cleanedQuery = originalQuery
                .replaceAll("(?i)LIMIT\\s+\\d+", "")
                .replaceAll("(?i)OFFSET\\s+\\d+", "");

        // Wrap in a subquery to count total rows
        return "SELECT COUNT(*) FROM (" + cleanedQuery + ") AS total_count_subquery";
    }

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
            try {
                // Submit DB work to bounded executor to avoid exhausting request threads
                return CompletableFuture.supplyAsync(() -> {
                    boolean permitAcquired = false;
                    try {
                        try {
                            // try to acquire permit quickly; tune timeout as needed
                            permitAcquired = duckDbSemaphore.tryAcquire(10, TimeUnit.SECONDS);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Interrupted while waiting for DB permit", ie);
                        }

                        if (!permitAcquired) {
                            throw new RuntimeException("Server overloaded: too many concurrent DB queries");
                        }

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

                            return response;
                        }

                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    } finally {
                        if (permitAcquired) {
                            duckDbSemaphore.release();
                        }
                    }
                }, dataProcessingExecutor);
            } catch (RejectedExecutionException rej) {
                CompletableFuture<DataResponse> failed = new CompletableFuture<>();
                failed.completeExceptionally(new RuntimeException("Server busy: too many concurrent requests", rej));
                return failed;
            }
        });
    }

    // NEW NEW
    public CompletableFuture<Map<String, Object>> getImageMetadata(DataSource dataSource, String authorization)
            throws Exception, SQLException {
        return dataQueryHelper.getFilePathForDataset(dataSource, authorization).thenApply(localFilePath -> {
            log.info("Image file downloaded and cached at: " + localFilePath);

            Map<String, Object> imageMetadata = Map.of(
                    "datasetType", "IMAGE",
                    // "isImage", true,
                    "imageUrl", dataSource.getSource(),
                    "localPath", localFilePath,
                    "fileNames", localFilePath,
                    "contentType", getContentTypeFromUrl(dataSource.getSource()),
                    "totalItems", 1,
                    "originalColumns", java.util.Collections.emptyList(),
                    "hasLatLonColumns", false);

            log.info("Created image metadata for: " + dataSource.getSource());
            return imageMetadata;
        });
    }
    // Till here

    public CompletableFuture<MetadataResponseV2> getFileMetadata(DataSource dataSource, String authorization)
            throws Exception, SQLException {
        return dataQueryHelper.getFilePathForDataset(dataSource, authorization).thenCompose(filePath -> {
            try {
                return CompletableFuture.supplyAsync(() -> {
                    boolean permitAcquired = false;
                    try {
                        try {
                            permitAcquired = duckDbSemaphore.tryAcquire(10, TimeUnit.SECONDS);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Interrupted while waiting for DB permit", ie);
                        }

                        if (!permitAcquired) {
                            throw new RuntimeException("Server overloaded: too many concurrent DB queries");
                        }

                        try (
                                Connection connection = this.dataSource.getConnection();
                                Statement statement = connection.createStatement()) {
                            // System.out.println("File path for metadata: " + filePath.toString());
                            // Use a mutable local copy because lambda capture requires effectively final
                            String resolvedPath = filePath;

                            FileType fileType = dataQueryHelper.detectFileType(resolvedPath);
                            if (fileType == FileType.JSON) {
                                try {
                                    resolvedPath = preprocessJsonIfNeeded(Paths.get(resolvedPath)).toString();
                                } catch (IOException e) {
                                    throw new RuntimeException("Failed to preprocess JSON file: " + resolvedPath, e);
                                }
                            }

                            String sql = "SELECT * FROM " + dataQueryHelper.getFileTypeSQL(fileType, resolvedPath)
                                    + " LIMIT 10";
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

                            String countSql = "SELECT COUNT(*) as total FROM "
                                    + dataQueryHelper.getFileTypeSQL(fileType, resolvedPath);
                            ResultSet countResult = statement.executeQuery(countSql);
                            int totalItems = 0;
                            if (countResult.next()) {
                                totalItems = countResult.getInt("total");
                            }

                            // Check for lat/lon columns
                            // Set metadata response fields

                            // Detect dataset type based on time columns and data ordering
                            DatasetType datasetType = dataQueryHelper.detectDatasetType(resultSet, timeColumns,
                                    statement, sql);

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

                                String summarizeSql = "SUMMARIZE " + sql.substring(sql.indexOf("FROM")); // ensures it
                                                                                                         // matches file
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

                    } finally {
                        if (permitAcquired) {
                            duckDbSemaphore.release();
                        }
                    }
                }, dataProcessingExecutor);
            } catch (RejectedExecutionException rej) {
                CompletableFuture<MetadataResponseV2> failed = new CompletableFuture<>();
                failed.completeExceptionally(new RuntimeException("Server busy: too many concurrent requests", rej));
                return failed;
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

    public Map<String, Object[]> fetchColumnsValues(
            DataSource dataSource,
            Rectangle rectangle,
            String latCol,
            String lonCol,
            String[] columnNames) throws Exception, SQLException {
        try (
                Connection connection = this.dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            String sql;
            if (rectangle != null && rectangle.getLat() != null && rectangle.getLon() != null) {
                sql = String.format(
                        "SELECT id, %s FROM read_csv('%s') WHERE %s BETWEEN %s AND %s AND %s BETWEEN %s AND %s",
                        String.join(", ", columnNames),
                        dataSource.getSource(),
                        latCol,
                        rectangle.getLat().lowerEndpoint(),
                        rectangle.getLat().upperEndpoint(),
                        lonCol,
                        rectangle.getLon().lowerEndpoint(),
                        rectangle.getLon().upperEndpoint());
            } else {
                sql = String.format(
                        "SELECT id, %s FROM read_csv('%s')",
                        String.join(", ", columnNames),
                        dataSource.getSource());
            }
            log.info("SQL: " + sql);
            ResultSet resultSet = statement.executeQuery(sql);
            int columnCount = resultSet.getMetaData().getColumnCount();
            log.info("Metadata: " + resultSet.getMetaData());

            // Get column names from metadata
            String[] allColumnNames = new String[columnCount];
            for (int i = 0; i < columnCount; i++) {
                allColumnNames[i] = resultSet.getMetaData().getColumnName(i + 1);
            }

            // Initialize the map with empty arrays for each column
            Map<String, Object[]> columnMap = new HashMap<>();
            for (String columnName : allColumnNames) {
                columnMap.put(columnName, new Object[0]);
            }

            // Collect all rows
            List<Object[]> resultsList = new ArrayList<>();
            while (resultSet.next()) {
                Object[] row = new Object[columnCount];
                for (int i = 0; i < columnCount; i++) {
                    row[i] = resultSet.getObject(i + 1);
                }
                resultsList.add(row);
            }

            // Convert to map structure: column name -> array of values
            int rowCount = resultsList.size();
            for (int colIndex = 0; colIndex < columnCount; colIndex++) {
                String columnName = allColumnNames[colIndex];
                Object[] columnValues = new Object[rowCount];

                for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
                    columnValues[rowIndex] = resultsList.get(rowIndex)[colIndex];
                }

                columnMap.put(columnName, columnValues);
            }

            resultSet.close();
            statement.close();
            return columnMap;
        }
    }

    public boolean deleteFileMetadata(String fileName) throws Exception, SQLException {
        String tableName = fileName.replace("-", "_");
        String metaTableName = "rawvis_meta";
        log.info("Deleting file metadata for " + tableName + " from " + metaTableName);

        try (Connection connection = this.dataSource.getConnection();
                Statement statement = connection.createStatement()) {

            int rowsDeleted = statement
                    .executeUpdate("DELETE FROM " + metaTableName + " WHERE table_name = '" + tableName + "'");

            statement.close();
            connection.close();

            log.info("File metadata deleted successfully, " + rowsDeleted + " rows deleted");
            return rowsDeleted > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete file metadata for " + tableName + " from " + metaTableName, e);
        }
    }

    public float[][] getUmap(float[][] data) {
        Umap umap = new Umap();
        umap.setNumberComponents(2);
        umap.setNumberNearestNeighbours(15);
        umap.setThreads(1);
        return umap.fitTransform(data);
    }

    private Path preprocessJsonIfNeeded(Path datasetPath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(datasetPath.toFile());

        // Detect dict-of-lists (object with array values)
        if (root.isObject()) {
            ObjectNode obj = (ObjectNode) root;
            if (obj.size() > 0 && obj.elements().next().isArray()) {
                int epochs = obj.elements().next().size(); // assume all arrays same length
                ArrayNode array = mapper.createArrayNode();

                for (int i = 0; i < epochs; i++) {
                    final int idx = i;
                    ObjectNode row = mapper.createObjectNode();
                    row.put("epoch", idx + 1);
                    obj.fieldNames().forEachRemaining(field -> {
                        JsonNode arr = obj.get(field);
                        row.set(field, arr.get(idx));
                    });
                    array.add(row);
                }

                // Write reshaped JSON to a temp file
                Path tmpFile = Files.createTempFile("reshaped_metrics", ".json");
                mapper.writerWithDefaultPrettyPrinter().writeValue(tmpFile.toFile(), array);
                return tmpFile;
            }
        }

        // Return unchanged if already row-oriented
        return datasetPath;
    }

    // NEW
    private String extractFileName(String url) {
        if (url == null)
            return "unknown";

        // Remove query parameters if any
        String cleanUrl = url.split("\\?")[0];

        // Extract filename from URL
        int lastSlash = cleanUrl.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < cleanUrl.length() - 1) {
            return cleanUrl.substring(lastSlash + 1);
        }

        return "image";
    }

    private String getContentTypeFromUrl(String url) {
        if (url == null)
            return "application/octet-stream";

        String lowerUrl = url.toLowerCase();
        if (lowerUrl.contains(".png"))
            return "image/png";
        if (lowerUrl.contains(".jpg") || lowerUrl.contains(".jpeg"))
            return "image/jpeg";
        if (lowerUrl.contains(".gif"))
            return "image/gif";
        if (lowerUrl.contains(".webp"))
            return "image/webp";
        if (lowerUrl.contains(".bmp"))
            return "image/bmp";
        if (lowerUrl.contains(".svg"))
            return "image/svg+xml";

        return "image/*";
    }
    // Till here
}
