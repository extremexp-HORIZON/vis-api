package gr.imsi.athenarc.xtremexpvisapi.service.dataService.v2;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.davidmoten.geo.GeoHash;
import com.github.davidmoten.geo.LatLong;
import com.google.common.math.PairedStatsAccumulator;
import com.google.common.math.StatsAccumulator;

import gr.imsi.athenarc.xtremexpvisapi.domain.Metadata.DatasetType;
import gr.imsi.athenarc.xtremexpvisapi.domain.Metadata.MetadataMapResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.DataRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.DataResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.MapDataRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.MapDataResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.TimeSeriesDataRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.Column;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.DataSource;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.FileType;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.GroupedStats;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.RectStats;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.SourceType;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.aggregation.Aggregation;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.filter.AbstractFilter;
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
        return input.contains(" ") ? "\"" + input + "\"" : input;
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

        boolean hasLat = lowerColNames.contains("lat") || lowerColNames.contains("latitude");
        boolean hasLon = lowerColNames.contains("lon") || lowerColNames.contains("long")
                || lowerColNames.contains("longitude");

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
     * Determines if SQL type is numerical
     * 
     * @param sqlType
     * @return
     */
    protected boolean isNumerical(Column column) {
        if (column.getName().equals("longitude") || column.getName().equals("latitude")) {
            return false;
        }
        return column.getType().equals("INTEGER") || column.getType().equals("BIGINT")
                || column.getType().equals("DOUBLE") || column.getType().equals("FLOAT")
                || column.getType().equals("DECIMAL") || column.getType().equals("NUMERIC");
    }

    /**
     * Determines if SQL type is categorical
     * 
     * @param sqlType
     * @return
     */
    protected boolean isCategorical(Column column) {
        if (column.getName().equals("id") || column.getName().equals("h3")
                || column.getName().equals("geohash")) {
            return false;
        }
        return column.getType().equals("STRING");
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
                sql = buildAggregationQuery(request, sql);
            } else {
                // Only add GROUP BY if there are no aggregations (regular grouping)
                if (request.getGroupBy() != null && !request.getGroupBy().isEmpty()) {
                    sql.append(" GROUP BY ");
                    sql.append(String.join(", ", request.getGroupBy()));
                }
            }

            // LIMIT and OFFSET
            if (request.getLimit() != null) {
                sql.append(" LIMIT ").append(request.getLimit());
            }

            if (request.getOffset() != null) {
                sql.append(" OFFSET ").append(request.getOffset());
            }

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
    private StringBuilder buildAggregationQuery(DataRequest request, StringBuilder baseQuery) {
        StringBuilder aggQuery = new StringBuilder("SELECT ");

        // Add group by columns first
        if (request.getGroupBy() != null && !request.getGroupBy().isEmpty()) {
            aggQuery.append(String.join(", ", request.getGroupBy())).append(", ");
        }

        // Add aggregation functions
        List<String> aggSqls = request.getAggregations().stream()
                .map(Aggregation::toSql)
                .collect(Collectors.toList());
        aggQuery.append(String.join(", ", aggSqls));

        // Add FROM clause as subquery (base query without GROUP BY)
        aggQuery.append(" FROM (").append(baseQuery).append(") as subquery");

        // Add GROUP BY if present
        if (request.getGroupBy() != null && !request.getGroupBy().isEmpty()) {
            aggQuery.append(" GROUP BY ").append(String.join(", ", request.getGroupBy()));
        }

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
            return CompletableFuture.completedFuture(fileService.downloadAndCacheDataAsset(dataSource, authorization));
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

    /**
     * Populates the metadata for RawVis datasets.
     * 
     * @param duckdbConnection
     * @param filePath
     * @param dataSource
     * @param convertedColumns
     * @param metadataResponse
     * @throws SQLException
     */
    public void populateRawvisMeta(
            Connection duckdbConnection,
            String filePath,
            DataSource dataSource,
            List<Column> convertedColumns,
            MetadataMapResponse metadataResponse) throws SQLException {

        String tableName = dataSource.getFileName().replace("-", "_");
        String metaTableName = dataSource.getFormat().toLowerCase() + "_meta";
        Statement stmt = duckdbConnection.createStatement();

        // 1. Ensure meta table exists
        String createMetaTableSql = String.format(
                "CREATE TABLE IF NOT EXISTS %s (" +
                        "table_name VARCHAR PRIMARY KEY, " +
                        "xMin DOUBLE, " +
                        "xMax DOUBLE, " +
                        "yMin DOUBLE, " +
                        "yMax DOUBLE, " +
                        "min_time BIGINT, " +
                        "max_time BIGINT, " +
                        "queryXMin DOUBLE, " +
                        "queryXMax DOUBLE, " +
                        "queryYMin DOUBLE, " +
                        "queryYMax DOUBLE, " +
                        "dimensions VARCHAR, " +
                        "measures VARCHAR, " +
                        "measure0 VARCHAR, " +
                        "measure1 VARCHAR" +
                        ")",
                metaTableName);
        stmt.execute(createMetaTableSql);

        // 2. Check if row for fileName exists
        String checkRowSql = String.format(
                "SELECT * FROM %s WHERE table_name = ?", metaTableName);
        PreparedStatement checkRowStmt = duckdbConnection.prepareStatement(checkRowSql);
        checkRowStmt.setString(1, tableName);
        ResultSet rowRs = checkRowStmt.executeQuery();

        boolean foundInMeta = false;
        if (rowRs.next()) {
            log.info("Found in meta table");
            // Populate metadataResponse from meta table
            metadataResponse.setXMin(rowRs.getDouble("xMin"));
            metadataResponse.setXMax(rowRs.getDouble("xMax"));
            metadataResponse.setYMin(rowRs.getDouble("yMin"));
            metadataResponse.setYMax(rowRs.getDouble("yMax"));
            metadataResponse.setTimeMin(rowRs.getLong("min_time"));
            metadataResponse.setTimeMax(rowRs.getLong("max_time"));
            metadataResponse.setQueryXMin(rowRs.getDouble("queryXMin"));
            metadataResponse.setQueryXMax(rowRs.getDouble("queryXMax"));
            metadataResponse.setQueryYMin(rowRs.getDouble("queryYMin"));
            metadataResponse.setQueryYMax(rowRs.getDouble("queryYMax"));
            String dimensionsStr = rowRs.getString("dimensions");
            if (dimensionsStr != null && !dimensionsStr.isEmpty()) {
                metadataResponse.getDimensions().addAll(java.util.Arrays.asList(dimensionsStr.split(",")));
            }
            String measuresStr = rowRs.getString("measures");
            if (measuresStr != null && !measuresStr.isEmpty()) {
                metadataResponse.getMeasures().addAll(java.util.Arrays.asList(measuresStr.split(",")));
            }
            metadataResponse.setMeasure0(rowRs.getString("measure0"));
            metadataResponse.setMeasure1(rowRs.getString("measure1"));
            foundInMeta = true;
        }
        rowRs.close();
        checkRowStmt.close();

        if (!foundInMeta) {
            // 3. If not found, read CSV and compute metadata, then insert into meta table
            // Detect delimiter
            String delimiter;
            try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
                String firstLine = br.readLine();
                delimiter = firstLine.contains("\t") ? "\t" : ",";
            } catch (IOException e) {
                throw new RuntimeException("Failed to read file", e);
            }

            String rawVisSql = String.format(
                    "SELECT " +
                            "MIN(radio_timestamp) AS min_time, " +
                            "MAX(radio_timestamp) AS max_time, " +
                            "MIN(longitude) AS queryXMin, " +
                            "MAX(longitude) AS queryXMax, " +
                            "MIN(latitude) AS queryYMin, " +
                            "MAX(latitude) AS queryYMax " +
                            "FROM read_csv_auto('%s', delim='%s', DATEFORMAT='auto', HEADER=TRUE)",
                    filePath.replace("\\", "\\\\"), delimiter);
            ResultSet rs = stmt.executeQuery(rawVisSql);
            Timestamp minTs = null;
            Timestamp maxTs = null;
            double queryXMin = 0, queryXMax = 0, queryYMin = 0, queryYMax = 0;
            double xMin = -157.9287779, xMax = 119.819558, yMin = 8.91667, yMax = 61.231374;
            if (rs.next()) {
                minTs = rs.getTimestamp("min_time");
                maxTs = rs.getTimestamp("max_time");
                queryXMin = rs.getDouble("queryXMin");
                queryXMax = rs.getDouble("queryXMax");
                queryYMin = rs.getDouble("queryYMin");
                queryYMax = rs.getDouble("queryYMax");
                metadataResponse.setTimeMin(minTs != null ? minTs.getTime() : 0);
                metadataResponse.setTimeMax(maxTs != null ? maxTs.getTime() : 0);
                metadataResponse.setXMin(xMin);
                metadataResponse.setXMax(xMax);
                metadataResponse.setYMin(yMin);
                metadataResponse.setYMax(yMax);
                metadataResponse.setQueryXMin(queryXMin);
                metadataResponse.setQueryXMax(queryXMax);
                metadataResponse.setQueryYMin(queryYMin);
                metadataResponse.setQueryYMax(queryYMax);
                // Populate dimensions and measures depending on the columns. (hardcoded ?)
                metadataResponse.getDimensions()
                        .addAll(Arrays.asList("net_type", "mcc_nr", "mnc_nr", "provider", "eci_cid"));
                // metadataResponse.getDimensions()
                // .addAll(convertedColumns.stream()
                // .filter(c -> isCategorical(c))
                // .map(Column::getName).collect(Collectors.toList()));
                metadataResponse.getMeasures().addAll(convertedColumns.stream()
                        .filter(c -> isNumerical(c))
                        .map(Column::getName).collect(Collectors.toList()));
                metadataResponse.setMeasure0("rsrp_rscp_rssi");
                metadataResponse.setMeasure1(dataSource.getFileName().equals("patra") ? "latency" : "cqi");
            }
            rs.close();

            // Insert into meta table
            String upsertMetaSql = String.format(
                    "INSERT OR REPLACE INTO %s (table_name, xMin, xMax, yMin, yMax, min_time, max_time, queryXMin, queryXMax, queryYMin, queryYMax, dimensions, measures, measure0, measure1) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    metaTableName);
            PreparedStatement upsertStmt = duckdbConnection.prepareStatement(upsertMetaSql);
            upsertStmt.setString(1, tableName);
            upsertStmt.setDouble(2, xMin);
            upsertStmt.setDouble(3, xMax);
            upsertStmt.setDouble(4, yMin);
            upsertStmt.setDouble(5, yMax);
            upsertStmt.setLong(6, minTs != null ? minTs.getTime() : 0L);
            upsertStmt.setLong(7, maxTs != null ? maxTs.getTime() : 0L);
            upsertStmt.setDouble(8, queryXMin);
            upsertStmt.setDouble(9, queryXMax);
            upsertStmt.setDouble(10, queryYMin);
            upsertStmt.setDouble(11, queryYMax);
            upsertStmt.setString(12, String.join(",", metadataResponse.getDimensions()));
            upsertStmt.setString(13, String.join(",", metadataResponse.getMeasures()));
            upsertStmt.setString(14, metadataResponse.getMeasure0());
            upsertStmt.setString(15, metadataResponse.getMeasure1());
            upsertStmt.executeUpdate();
            upsertStmt.close();
        }
        // 4. Populate facets
        Map<String, List<String>> facets = new HashMap<>();
        for (String dimension : metadataResponse.getDimensions()) {
            String facetsSql = String.format(
                    "SELECT DISTINCT %s FROM read_csv('%s')",
                    dimension,
                    filePath.replace("\\", "\\\\"));
            ResultSet facetsRs = stmt.executeQuery(facetsSql);
            StringBuilder sb = new StringBuilder();
            List<String> values = new ArrayList<>();
            while (facetsRs.next()) {
                sb.append(dimension).append(": ")
                        .append(facetsRs.getString(1)).append(System.lineSeparator());
                values.add(facetsRs.getString(1));
            }
            // log.info("Facet counts for " + dimension + ":\n" + sb.toString());
            facets.put(dimension, values);
            facetsRs.close();
        }
        metadataResponse.setFacets(facets);
        stmt.close();
    }

    /**
     * Builds the SQL query for the map view.
     * 
     * @param request
     * @param authorization
     * @param metadataResponse
     * @return
     */
    @Async
    protected CompletableFuture<String> buildMapQuery(MapDataRequest request, String authorization,
            MetadataMapResponse metadataResponse) throws Exception {

        // TODO: Think of merging this with the buildQuery method
        return getFilePathForDataset(request.getDataSource(), authorization).thenApply(datasetPath -> {
            StringBuilder sql = new StringBuilder();
            String latCol = "latitude";
            String lonCol = "longitude";
            String timestampCol = "radio_timestamp";

            // SELECT clause
            sql.append("SELECT ");
            sql.append(String.join(", ", "id", latCol, lonCol,
                    metadataResponse.getMeasure0(), metadataResponse.getMeasure1(),
                    String.join(", ", metadataResponse.getDimensions())));

            // FROM clause - use detected or specified file type
            FileType fileType = detectFileType(datasetPath);
            sql.append(" FROM ");
            switch (fileType) {
                case CSV:
                    sql.append("read_csv('").append(datasetPath).append("', nullstr='NULL')");
                    break;
                case PARQUET:
                    sql.append("read_parquet('").append(datasetPath).append("')");
                    break;
                case JSON:
                    sql.append("read_json_auto('").append(datasetPath).append("')");
                    break;
            }

            // WHERE clause
            sql.append(" WHERE ");
            sql.append(latCol + " BETWEEN " + request.getRect().getLat().lowerEndpoint() + " AND "
                    + request.getRect().getLat().upperEndpoint());
            sql.append(" AND " + lonCol + " BETWEEN " + request.getRect().getLon().lowerEndpoint() + " AND "
                    + request.getRect().getLon().upperEndpoint());

            if (request.getCategoricalFilters() != null && !request.getCategoricalFilters().isEmpty()) {
                sql.append(request.getCategoricalFilters().entrySet().stream()
                        .map(entry -> " AND " + entry.getKey() + " = '" + entry.getValue() + "'")
                        .collect(Collectors.joining()));
            }

            if (request.getFrom() != null && request.getTo() != null) {
                sql.append(" AND " + timestampCol + " BETWEEN '" + new Timestamp(request.getFrom()) + "' AND '"
                        + new Timestamp(request.getTo()) + "'");
            } else if (request.getFrom() != null) {
                sql.append(" AND " + timestampCol + " >= '" + new Timestamp(request.getFrom()) + "'");
            } else if (request.getTo() != null) {
                sql.append(" AND " + timestampCol + " <= '" + new Timestamp(request.getTo()) + "'");
            }
            return sql.toString();
        });
    }

    /**
     * Processes the results of the map query.
     * 
     * @param resultSet
     * @param request
     * @param metadataResponse
     * @param response
     * @param geohashLength
     * @throws SQLException
     */
    protected void processMapQueryResults(ResultSet resultSet, MapDataRequest request,
            MetadataMapResponse metadataResponse,
            MapDataResponse response, int geohashLength) throws SQLException {
        Map<String, List<Object[]>> geohashGroups = new HashMap<>();
        Map<String, StatsAccumulator> groupStatsMap = new HashMap<>();
        PairedStatsAccumulator pairedStatsAccumulator = new PairedStatsAccumulator();
        int rawPointCount = 0; // Initialize a counter for raw data points

        while (resultSet.next()) {
            rawPointCount++;

            double lat = resultSet.getDouble("latitude");
            double lon = resultSet.getDouble("longitude");
            String id = resultSet.getString("id");

            // get measures and handle nulls and empty strings
            String measure0Str = resultSet.getString(metadataResponse.getMeasure1());
            Double measure0 = (measure0Str == null || measure0Str.trim().isEmpty())
                    ? null
                    : Double.parseDouble(measure0Str);
            String measure1Str = resultSet.getString(metadataResponse.getMeasure1());
            Double measure1 = (measure1Str == null || measure1Str.trim().isEmpty())
                    ? null
                    : Double.parseDouble(measure1Str);
            String geohash = GeoHash.encodeHash(lat, lon, geohashLength);
            List<String> groupValues = new ArrayList<>();
            for (String colName : request.getGroupByCols()) {
                groupValues.add(resultSet.getString(colName));
            }
            List<String> categoricalValues = new ArrayList<>();
            for (String colName : metadataResponse.getDimensions()) {
                categoricalValues.add(resultSet.getString(colName));
            }
            Object[] point = new Object[] { lat, lon, id, measure0, measure1, categoricalValues };
            geohashGroups.computeIfAbsent(geohash, k -> new ArrayList<>()).add(point);

            String groupKey = String.join(",", groupValues);

            Double measureValue = resultSet.getObject(request.getMeasureCol()) != null
                    ? resultSet.getDouble(request.getMeasureCol())
                    : null;
            if (measureValue != null)
                groupStatsMap.computeIfAbsent(groupKey, k -> new StatsAccumulator()).add(measureValue);

            // For paired statistical analysis
            if (measure0 != null && measure1 != null)
                pairedStatsAccumulator.add(measure0, measure1);

        }
        log.info("Raw Point count: " + rawPointCount);

        List<Object[]> points = new ArrayList<>();
        for (Map.Entry<String, List<Object[]>> entry : geohashGroups.entrySet()) {
            List<Object[]> groupedPoints = entry.getValue();
            String geohash = entry.getKey();
            LatLong geohashCenter = GeoHash.decodeHash(geohash);

            if (groupedPoints.size() == 1) {
                Object[] singlePoint = groupedPoints.get(0);
                points.add(new Object[] { geohashCenter.getLat(), geohashCenter.getLon(), 1, singlePoint[2],
                        singlePoint[3], singlePoint[4], singlePoint[5] });
            } else {
                OptionalDouble measure0AvgOpt = groupedPoints.stream().map(point -> point[3])
                        .filter(value -> value != null).mapToDouble(value -> (double) value).average();
                OptionalDouble measure1AvgOpt = groupedPoints.stream().map(point -> point[4])
                        .filter(value -> value != null).mapToDouble(value -> (double) value).average();

                Double measure0Avg = measure0AvgOpt.isPresent() ? measure0AvgOpt.getAsDouble() : null;
                Double measure1Avg = measure1AvgOpt.isPresent() ? measure1AvgOpt.getAsDouble() : null;
                // Process groupedPoints using streams
                @SuppressWarnings("unchecked")
                List<List<String>> groupedGroupByValues = processStrings(groupedPoints.stream()
                        .map(allPoints -> (List<String>) allPoints[5])
                        .collect(Collectors.toList()));

                points.add(new Object[] { geohashCenter.getLat(), geohashCenter.getLon(), groupedPoints.size(), null,
                        measure0Avg, measure1Avg, groupedGroupByValues });
            }
        }

        List<GroupedStats> series = groupStatsMap.entrySet().stream()
                .map(entry -> {
                    String key = entry.getKey();
                    StatsAccumulator accumulator = entry.getValue();
                    List<String> group = Arrays.asList(key.split(","));
                    double value = 0;
                    switch (request.getAggType()) {
                        case AVG:
                            value = accumulator.mean();
                            break;
                        case SUM:
                            value = accumulator.sum();
                            break;
                        case MIN:
                            value = accumulator.min();
                            break;
                        case MAX:
                            value = accumulator.max();
                            break;
                        case COUNT:
                            value = accumulator.count();
                            break;
                        default:
                            break;
                    }
                    return new GroupedStats(group, value);
                })
                .collect(Collectors.toList());

        // TODO: Figure out which fields are needed for the response
        response.setSeries(series);
        response.setFacets(metadataResponse.getFacets());
        response.setPoints(points);
        response.setPointCount(rawPointCount);
        response.setTotalItems(rawPointCount);
        response.setQuerySize(points.size());
        response.setRectStats(new RectStats(pairedStatsAccumulator.snapshot()));
    }

    /**
     * Builds the SQL query for the time series view.
     * 
     * @param request
     * @param authorization
     * @param metadataResponse
     * @return
     */
    @Async
    protected CompletableFuture<String> buildTimeSeriesQuery(TimeSeriesDataRequest request, String authorization,
            MetadataMapResponse metadataResponse) throws Exception {
        return getFilePathForDataset(request.getDataSource(), authorization).thenApply(datasetPath -> {

            String latCol = "latitude";
            String lonCol = "longitude";
            String timestampCol = "radio_timestamp";

            long intervalSeconds = request.getFrequency();
            String sql = String.format(
                    "SELECT floor(extract(epoch from %1$s)/(%2$s)) as time_bucket, avg(%3$s) as average_value " +
                            "FROM read_csv('%4$s') " +
                            "WHERE %1$s BETWEEN '%5$s' AND '%6$s' " +
                            "AND %7$s BETWEEN '%8$s' AND '%9$s' " +
                            "AND %10$s BETWEEN '%11$s' AND '%12$s' ",
                    timestampCol,
                    intervalSeconds,
                    request.getMeasureCol(),
                    datasetPath,
                    new Timestamp(request.getFrom()),
                    new Timestamp(request.getTo()),
                    latCol,
                    request.getRect().getLat().lowerEndpoint(),
                    request.getRect().getLat().upperEndpoint(),
                    lonCol,
                    request.getRect().getLon().lowerEndpoint(),
                    request.getRect().getLon().upperEndpoint());

            StringBuilder filterBuilder = new StringBuilder();
            if (request.getCategoricalFilters() != null && !request.getCategoricalFilters().isEmpty()) {
                request.getCategoricalFilters().forEach((key, value) -> {
                    filterBuilder.append(String.format(" AND %s = '%s'", key, value));
                });
                sql += filterBuilder.toString();
            }

            sql += " GROUP BY time_bucket ORDER BY time_bucket";
            return sql;
        });
    }

    /**
     * Processes the strings in the list of lists.
     * 
     * @param listOfLists
     * @return
     */
    private List<List<String>> processStrings(List<List<String>> listOfLists) {
        // List to store the list of unique strings for each column
        List<List<String>> processedStrings = new ArrayList<>();

        // Ensure there's at least one row to avoid IndexOutOfBoundsException
        if (listOfLists.isEmpty())
            return processedStrings;

        // Iterate over each column
        for (int i = 0; i < listOfLists.get(0).size(); i++) {
            Set<String> uniqueStrings = new HashSet<>();

            // Collect all unique strings in the current column
            for (int j = 0; j < listOfLists.size(); j++) {
                String currentString = listOfLists.get(j).get(i);
                if (currentString != null) {
                    uniqueStrings.add(currentString);
                }
            }

            // Convert the set of unique strings to a list and add it to the results
            processedStrings.add(new ArrayList<>(uniqueStrings));
        }

        return processedStrings;
    }
}
