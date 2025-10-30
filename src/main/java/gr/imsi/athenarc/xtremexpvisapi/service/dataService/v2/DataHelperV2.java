package gr.imsi.athenarc.xtremexpvisapi.service.dataService.v2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gr.imsi.athenarc.xtremexpvisapi.domain.metadata.DatasetType;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryv2.DataRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryv2.DataResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryv2.params.Column;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryv2.params.DataSource;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryv2.params.FileType;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryv2.params.SourceType;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryv2.params.aggregation.Aggregation;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryv2.params.filter.AbstractFilter;
import gr.imsi.athenarc.xtremexpvisapi.service.files.FileService;
import lombok.extern.java.Log;

@Component
@Log
public class DataHelperV2 {

    private final ObjectMapper objectMapper;
    private final FileService fileService;

    @Autowired
    public DataHelperV2(ObjectMapper objectMapper, FileService fileService) {
        this.objectMapper = objectMapper;
        this.fileService = fileService;
    }

    /**
     * Detects the file type based on the dataset ID's file extension.
     *
     * @param datasetId the dataset identifier containing the file extension
     * @return the detected FileType, defaults to CSV if no match is found
     */
    protected FileType detectFileType(String datasetId) {
        for (FileType fileType : FileType.values()) {
            if (datasetId.toLowerCase().endsWith(fileType.getExtension())) {
                return fileType;
            }
        }
        return FileType.CSV; // Default fallback
    }

    /**
     * Helper function to correct the string representation of a column name.
     * If the column name contains spaces, it will be wrapped in double quotes.
     *
     * @param input the input string to correct
     * @return the corrected string
     */
    protected String getCorrectedString(String input) {
        // Implement your correction logic here
        if (input.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            return input; // safe to use as-is
        } else {
            return "\"" + input.replace("\"", "\"\"") + "\""; // quote and escape
        }
    }

    /**
     * Determines if a column type represents a time-based data type.
     *
     * @param columnType the SQL column type as a string
     * @return true if the column type is DATE, TIMESTAMP, or TIME; false otherwise
     */
    protected boolean isTimeColumn(String columnType) {
        return columnType.equals("DATE") ||
                columnType.equals("TIMESTAMP") ||
                columnType.equals("TIME");
    }

    protected boolean hasLatLonColumns(List<Column> columns) {
        Set<String> lowerColNames = columns.stream()
                .map(col -> col.getName().toLowerCase())
                .collect(Collectors.toSet());

        boolean hasLat = lowerColNames.contains("lat") || lowerColNames.contains("latitude")
                || lowerColNames.contains("lat_wgs84")
                || lowerColNames.contains("latitude_wgs84");
        boolean hasLon = lowerColNames.contains("lon") || lowerColNames.contains("long")
                || lowerColNames.contains("longitude") || lowerColNames.contains("lon_wgs84")
                || lowerColNames.contains("longitude_wgs84");

        return hasLat && hasLon;
    }

    /**
     * Detects the dataset type based on the presence and characteristics of time
     * columns.
     *
     * @param resultSet   the ResultSet containing the data
     * @param timeColumns the list of time column names
     * @param statement   the SQL Statement for executing queries
     * @param baseSql     the base SQL query string
     * @return DatasetType.timeseries if time data is ordered, DatasetType.tabular
     *         otherwise
     * @throws SQLException if a database access error occurs
     */
    protected DatasetType detectDatasetType(ResultSet resultSet, List<String> timeColumns, Statement statement,
            String baseSql) throws SQLException {
        if (timeColumns.isEmpty()) {
            return DatasetType.tabular;
        }

        // Check if time data is ordered (indicating time series)
        String timeColumn = timeColumns.get(0);
        String orderCheckSql = baseSql.replace("LIMIT 10",
                "ORDER BY " + timeColumn + " LIMIT 10");

        try {
            ResultSet orderedResult = statement.executeQuery(orderCheckSql);

            // If we can order by time column without error, it's likely time series data
            if (orderedResult.next()) {
                orderedResult.close();
                return DatasetType.timeseries;
            }
            orderedResult.close();
        } catch (SQLException e) {
            // If ordering fails, it's probably tabular
            log.info("Time column ordering failed, treating as tabular data");
        }

        return DatasetType.tabular;
    }

    /**
     * Converts a ResultSet to a DataResponse object containing tabular data.
     *
     * @param resultSet the ResultSet to convert
     * @param query     the SQL query that generated the ResultSet
     * @return a DataResponse containing the converted data, columns, and metadata
     * @throws SQLException if a database access error occurs
     */
    protected DataResponse convertResultSetToTabularResponse(ResultSet resultSet, String query) throws SQLException {
        List<Column> columns = new ArrayList<>();
        List<Map<String, Object>> dataRows = new ArrayList<>();

        // Get column metadata
        var metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();

        // Extract column information
        for (int i = 1; i <= columnCount; i++) {
            String columnName = metaData.getColumnName(i);
            String columnType = mapSqlTypeToString(metaData.getColumnType(i));
            columns.add(new Column(columnName, columnType));
        }

        // Extract data rows
        while (resultSet.next()) {
            Map<String, Object> row = new HashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                Object value = resultSet.getObject(i);
                row.put(columnName, value);
            }
            dataRows.add(row);
        }

        // Create TabularResponse
        DataResponse response = new DataResponse();
        response.setData(convertDataToJson(dataRows));
        response.setColumns(columns);
        response.setTotalItems(dataRows.size());
        response.setQuerySize(dataRows.size());

        return response;
    }

    protected String convertDataToJson(List<Map<String, Object>> dataRows) {
        try {
            return objectMapper.writeValueAsString(dataRows);
        } catch (Exception e) {
            // Log the error and return a fallback
            System.err.println("Error converting data to JSON: " + e.getMessage());
            return "[]"; // Return empty JSON array as fallback
        }
    }

    /**
     * Maps SQL data types to readable string representations.
     *
     * @param sqlType the SQL type constant from java.sql.Types
     * @return a string representation of the SQL type
     */
    protected String mapSqlTypeToString(int sqlType) {
        switch (sqlType) {
            case java.sql.Types.VARCHAR:
            case java.sql.Types.CHAR:
            case java.sql.Types.LONGVARCHAR:
                return "STRING";
            case java.sql.Types.INTEGER:
                return "INTEGER";
            case java.sql.Types.BIGINT:
                return "BIGINT";
            case java.sql.Types.DOUBLE:
            case java.sql.Types.FLOAT:
                return "DOUBLE";
            case java.sql.Types.DECIMAL:
            case java.sql.Types.NUMERIC:
                return "DECIMAL";
            case java.sql.Types.BOOLEAN:
                return "BOOLEAN";
            case java.sql.Types.DATE:
                return "DATE";
            case java.sql.Types.TIMESTAMP:
                return "TIMESTAMP";
            case java.sql.Types.TIME:
                return "TIME";
            default:
                return "UNKNOWN";
        }
    }

    /**
     * Asynchronously builds a SQL query based on the provided data request.
     *
     * @param request the DataRequest containing query parameters
     * @return a CompletableFuture containing the built SQL query string
     * @throws Exception if query building fails
     */
    @Async
    protected CompletableFuture<String> buildQuery(DataRequest request, String authorization) throws Exception {

        return getFilePathForDataset(request.getDataSource(), authorization).thenApply(datasetPath -> {
            StringBuilder sql = new StringBuilder();

            // SELECT clause
            sql.append("SELECT ");
            if (request.getColumns() == null || request.getColumns().isEmpty()) {
                sql.append("*");
            } else {
                sql.append(String.join(", ", request.getColumns().stream()
                        .map(this::getCorrectedString).collect(Collectors.toList())));
            }

            // FROM clause - use detected or specified file type
            FileType fileType = detectFileType(datasetPath);
            if (fileType == FileType.JSON) {
                try {
                    datasetPath = preprocessJsonIfNeeded(Paths.get(datasetPath)).toString();
                } catch (IOException e) {
                    throw new RuntimeException("Failed to preprocess JSON file: " + datasetPath, e);
                }
            }
            sql.append(" FROM ");
            switch (fileType) {
                case CSV:
                    sql.append("read_csv('").append(datasetPath).append("')");
                    break;
                case PARQUET:
                    sql.append("read_parquet('").append(datasetPath).append("')");
                    break;
                case JSON:
                    sql.append("read_json_auto('").append(datasetPath).append("')");
                    break;
            }

            // WHERE clause (filters)
            if (request.getFilters() != null && !request.getFilters().isEmpty()) {
                sql.append(" WHERE ");
                sql.append(buildFiltersClause((List<AbstractFilter>) request.getFilters()));
            }

            // Handle aggregation - if aggregations are present, build aggregation query
            if (request.getAggregations() != null && !request.getAggregations().isEmpty()) {
                sql = buildAggregationQuery(request, datasetPath);
            } else {
                // Only add GROUP BY if there are no aggregations (regular grouping)
                if (request.getGroupBy() != null && !request.getGroupBy().isEmpty()) {
                    sql.append(" GROUP BY ");
                    sql.append(request.getGroupBy().stream()
                            .map(this::getCorrectedString)
                            .collect(Collectors.joining(", ")));
                }

            }

            // LIMIT and OFFSET
            if (request.getLimit() != null) {
                sql.append(" LIMIT ").append(request.getLimit());
            }

            if (request.getOffset() != null) {
                sql.append(" OFFSET ").append(request.getOffset());
            }
            // System.out.println("Built SQL query: " + sql.toString());

            return sql.toString();
        });
    }

    /**
     * Builds the WHERE clause for SQL queries from a list of filters.
     *
     * @param filters the list of filters to convert to SQL
     * @return a string containing the WHERE clause conditions joined by AND
     */
    private String buildFiltersClause(List<AbstractFilter> filters) {
        return filters.stream()
                .map(AbstractFilter::toSql)
                .filter(sql -> !sql.equals("1=1")) // Remove no-op filters
                .collect(Collectors.joining(" AND "));
    }

    /**
     * Builds an aggregation query by wrapping the base query with aggregation
     * functions.
     *
     * @param request   the DataRequest containing aggregation parameters
     * @param baseQuery the base SQL query to wrap
     * @return a StringBuilder containing the aggregation query
     */
    private StringBuilder buildAggregationQuery(DataRequest request, String datasetPath) {
        StringBuilder aggQuery = new StringBuilder("SELECT ");

        // Detect file type
        FileType fileType = detectFileType(datasetPath);
        String fileTable = getFileTypeSQL(fileType, datasetPath);
        // System.out.println("Detected file type: " + fileType + ", SQL: " + fileTable);

        // Quoted group-by columns
        if (request.getGroupBy() != null && !request.getGroupBy().isEmpty()) {
            aggQuery.append(request.getGroupBy().stream()
                    .map(this::getCorrectedString)
                    .collect(Collectors.joining(", ")))
                    .append(", ");
        }

        // Aggregation functions (should already quote inside toSql())
        List<String> aggSqls = request.getAggregations().stream()
                .map(Aggregation::toSql)
                .collect(Collectors.toList());
        aggQuery.append(String.join(", ", aggSqls));

        // Flat FROM clause — no subquery
        aggQuery.append(" FROM ").append(fileTable);

        // WHERE clause if filters exist
        if (request.getFilters() != null && !request.getFilters().isEmpty()) {
            aggQuery.append(" WHERE ")
                    .append(buildFiltersClause((List<AbstractFilter>) request.getFilters()));
        }

        // GROUP BY
        if (request.getGroupBy() != null && !request.getGroupBy().isEmpty()) {
            aggQuery.append(" GROUP BY ")
                    .append(request.getGroupBy().stream()
                            .map(this::getCorrectedString)
                            .collect(Collectors.joining(", ")));
        }

        // LIMIT
        // if (request.getLimit() != null) {
        // aggQuery.append(" LIMIT ").append(request.getLimit());
        // }

        return aggQuery;
    }

    /**
     * Helper function to get the file path for both local and external datasets.
     * For local datasets, returns the source path directly.
     * For external datasets, checks cache and downloads if necessary.
     *
     * @param meta the dataset metadata containing source information
     * @return a CompletableFuture containing the file path (local path for external
     *         files, source path for internal files)
     * @throws Exception if download fails
     */
    @Async
    protected CompletableFuture<String> getFilePathForDataset(DataSource dataSource, String authorization)
            throws Exception {
        // Assuming request has a method to get DatasetMeta and file type
        String targetPath;
        SourceType fileType = dataSource.getSourceType(); // Assuming this method exists

        if (fileType.equals(SourceType.local)) {
            targetPath = dataSource.getSource();
            log.info("Internal file detected, using source path: " + targetPath);
            return CompletableFuture.completedFuture(targetPath);
        } else {
            return CompletableFuture.completedFuture(
                    fileService.downloadAndCacheDataAsset(dataSource, authorization));
        }
    }

    /**
     * Generates the appropriate SQL function call for reading files based on file
     * type.
     *
     * @param fileType the type of file to read
     * @param filePath the path to the file
     * @return SQL function call string for reading the specified file type
     * @throws IllegalArgumentException if the file type is unknown
     */
    protected String getFileTypeSQL(FileType fileType, String filePath) {
        switch (fileType) {
            case CSV:
                return "read_csv('" + filePath + "')";
            case PARQUET:
                return "read_parquet('" + filePath + "')";
            case JSON:
                return "read_json_auto('" + filePath + "')";
            default:
                throw new IllegalArgumentException("Unknown file type: " + fileType);
        }
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

}
