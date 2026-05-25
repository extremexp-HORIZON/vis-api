package gr.imsi.athenarc.xtremexpvisapi.service.dataService.v2;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
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

    @Value("${experiment.engine:extremeXP}")
    private String experimentEngine;
    
    @Value("${app.working.directory.mlflow}")
    private String mlflowWorkingDirectory;
    
    private static final Logger LOG = LoggerFactory.getLogger(DataHelperV2.class);

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
     * Streams rows directly into a JsonGenerator instead of buffering them as a
     * Java List first. Peak memory is roughly halved on large result sets because
     * we no longer hold both the List<Map> and the serialized JSON String at the
     * same time.
     */
    protected DataResponse convertResultSetToTabularResponse(ResultSet resultSet, String query) throws SQLException {
        List<Column> columns = new ArrayList<>();

        var metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();

        String[] columnNames = new String[columnCount + 1];
        for (int i = 1; i <= columnCount; i++) {
            String columnName = metaData.getColumnName(i);
            String columnType = mapSqlTypeToString(metaData.getColumnType(i));
            columns.add(new Column(columnName, columnType));
            columnNames[i] = columnName;
        }

        StringWriter writer = new StringWriter();
        int rowCount = 0;
        JsonFactory factory = objectMapper.getFactory();
        try (JsonGenerator gen = factory.createGenerator(writer)) {
            gen.writeStartArray();
            while (resultSet.next()) {
                gen.writeStartObject();
                for (int i = 1; i <= columnCount; i++) {
                    gen.writeFieldName(columnNames[i]);
                    Object value = resultSet.getObject(i);
                    objectMapper.writeValue(gen, value);
                }
                gen.writeEndObject();
                rowCount++;
            }
            gen.writeEndArray();
        } catch (IOException ioe) {
            log.warning("Failed to stream JSON: " + ioe.getMessage());
            writer.getBuffer().setLength(0);
            writer.write("[]");
        }

        DataResponse response = new DataResponse();
        response.setData(writer.toString());
        response.setColumns(columns);
        response.setTotalItems(rowCount);
        response.setQuerySize(rowCount);

        return response;
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
    @Async("dataProcessingExecutor")
    protected CompletableFuture<String> buildQuery(DataRequest request, String authorization) throws Exception {
        CompletableFuture<String> datasetPathFuture;

        if ("mlflow".equalsIgnoreCase(experimentEngine)) {
            Path p = Paths.get(request.getDataSource().getSource());

            if (!p.isAbsolute()) {
                p = Paths.get(mlflowWorkingDirectory).resolve(p).normalize();
            }

            if (Files.exists(p)) {
                LOG.info("MLflow local file exists: {}", p);
                datasetPathFuture = CompletableFuture.completedFuture(p.toString());
            } else {
                LOG.info("MLflow file missing, downloading and caching to: {}", p);
                String downloadedPath = fileService.downloadMlflowArtifact(request.getDataSource(), p, authorization);
                datasetPathFuture = CompletableFuture.completedFuture(downloadedPath);
            }
        } else {
            datasetPathFuture = getFilePathForDataset(request.getDataSource(), authorization);
        }

        return datasetPathFuture.thenApply(datasetPath -> {
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
    @Async("dataProcessingExecutor")
    protected CompletableFuture<String> getFilePathForDataset(DataSource dataSource, String authorization)
            throws Exception {
        // Assuming request has a method to get DatasetMeta and file type
        SourceType fileType = dataSource.getSourceType(); // Assuming this method exists

        if (fileType == SourceType.local) {
            String sourcePath = dataSource.getSource();

            if ("mlflow".equalsIgnoreCase(experimentEngine)) {
                Path p = Paths.get(sourcePath);
                if (!p.isAbsolute()) {
                    p = Paths.get(mlflowWorkingDirectory).resolve(p).normalize();
                }

                if (Files.exists(p)) {
                    LOG.info("MLflow local file exists for metadata: {}", p);
                    return CompletableFuture.completedFuture(p.toString());
                }

                LOG.info("MLflow file missing for metadata, downloading and caching to: {}", p);
                String downloadedPath = fileService.downloadMlflowArtifact(dataSource, p, authorization);
                return CompletableFuture.completedFuture(downloadedPath);
            }

            log.info("Internal file detected, using source path: " + sourcePath);
            return CompletableFuture.completedFuture(sourcePath);
        }

        return CompletableFuture.completedFuture(
                fileService.downloadAndCacheDataAsset(dataSource, authorization));
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
     * Resolves a JSON dataset to its preprocessed (row-oriented) form when needed.
     * For non-JSON files this is a no-op. Surfaces IOExceptions as RuntimeException
     * so callers running inside a CompletableFuture don't need checked-exception
     * gymnastics.
     */
    protected String resolveJsonIfNeeded(String filePath) {
        FileType ft = detectFileType(filePath);
        if (ft != FileType.JSON) return filePath;
        try {
            return preprocessJsonIfNeeded(Paths.get(filePath)).toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to preprocess JSON file: " + filePath, e);
        }
    }

    private String filtersToWhereClause(List<AbstractFilter> filters) {
        if (filters == null || filters.isEmpty()) return "";
        String inner = filters.stream()
                .map(AbstractFilter::toSql)
                .filter(s -> !s.equals("1=1"))
                .collect(Collectors.joining(" AND "));
        return inner.isEmpty() ? "" : " WHERE " + inner;
    }

    /**
     * Builds a GROUP BY aggregation query. groupBy is the only thing that drives
     * the cardinality of the result, so the response stays small regardless of
     * the source row count.
     */
    protected String buildAggregateSql(String fromClause, List<String> groupBy, List<Aggregation> aggregations,
            List<AbstractFilter> filters, Integer limit) {
        StringBuilder sql = new StringBuilder("SELECT ");
        boolean hasGroupBy = groupBy != null && !groupBy.isEmpty();

        if (hasGroupBy) {
            sql.append(groupBy.stream()
                    .map(this::getCorrectedString)
                    .collect(Collectors.joining(", ")));
        }

        if (aggregations != null && !aggregations.isEmpty()) {
            if (hasGroupBy) sql.append(", ");
            sql.append(aggregations.stream()
                    .map(Aggregation::toSql)
                    .collect(Collectors.joining(", ")));
        } else if (!hasGroupBy) {
            // Sensible default: COUNT(*) when no projection given.
            sql.append("COUNT(*) AS count_all");
        }

        sql.append(" FROM ").append(fromClause);
        sql.append(filtersToWhereClause(filters));

        if (hasGroupBy) {
            sql.append(" GROUP BY ").append(groupBy.stream()
                    .map(this::getCorrectedString)
                    .collect(Collectors.joining(", ")));
        }
        if (limit != null && limit > 0) {
            sql.append(" LIMIT ").append(limit);
        }
        return sql.toString();
    }

    /** COUNT of non-null x values matching the filters — used to decide whether downsampling is needed. */
    protected String buildCountSql(String fromClause, String xColumn, List<AbstractFilter> filters) {
        String where = filtersToWhereClause(filters);
        String conn = where.isEmpty() ? " WHERE " : " AND ";
        return "SELECT COUNT(*) FROM " + fromClause + where + conn
                + getCorrectedString(xColumn) + " IS NOT NULL";
    }

    /**
     * M4 SQL: split rows into `buckets` equal-width buckets over xColumn's sort order,
     * then for each Y column emit y_min / y_max / y_first / y_last and the matching x.
     * Data is sorted by x within each bucket, so MIN(x)/MAX(x) give x_first/x_last directly.
     */
    protected String buildM4Sql(String fromClause, String xColumn, List<String> yColumns,
            List<AbstractFilter> filters, int buckets) {
        String x = getCorrectedString(xColumn);
        String where = filtersToWhereClause(filters);
        String xNotNull = (where.isEmpty() ? " WHERE " : " AND ") + x + " IS NOT NULL";

        // Project the columns we need into a CTE; reference them by their original
        // (quoted) names throughout so SELECT * propagation stays consistent.
        String yProjection = yColumns.stream()
                .map(this::getCorrectedString)
                .collect(Collectors.joining(", "));

        StringBuilder ySelects = new StringBuilder();
        for (String y : yColumns) {
            String yq = getCorrectedString(y);
            String safe = sanitizeIdent(y);
            // M4 stores 4 points per bucket per y: (x_first,y_first), (x_at_min,y_min),
            // (x_at_max,y_max), (x_last,y_last). arg_min/arg_max give the x at the y extremes.
            ySelects.append(", MIN(").append(yq).append(") AS ").append(safe).append("_min")
                    .append(", MAX(").append(yq).append(") AS ").append(safe).append("_max")
                    .append(", arg_min(").append(yq).append(", ").append(x).append(") AS ").append(safe).append("_first")
                    .append(", arg_max(").append(yq).append(", ").append(x).append(") AS ").append(safe).append("_last")
                    .append(", arg_min(").append(x).append(", ").append(yq).append(") AS x_at_").append(safe).append("_min")
                    .append(", arg_max(").append(x).append(", ").append(yq).append(") AS x_at_").append(safe).append("_max");
        }

        return "WITH numbered AS ("
                + "  SELECT " + x + ", " + yProjection
                + ", row_number() OVER (ORDER BY " + x + ") - 1 AS rn,"
                + " count(*) OVER () AS total"
                + " FROM " + fromClause + where + xNotNull
                + "), bucketed AS ("
                + "  SELECT *, CAST(rn * " + buckets + " / total AS INTEGER) AS bucket FROM numbered"
                + ") SELECT bucket,"
                + " MIN(" + x + ") AS x_first, MAX(" + x + ") AS x_last"
                + ySelects.toString()
                + " FROM bucketed GROUP BY bucket ORDER BY bucket";
    }

    /** Stats query used to derive bin edges before binning. */
    protected String buildHistogramStatsSql(String fromClause, String column, List<AbstractFilter> filters) {
        String c = getCorrectedString(column);
        return "SELECT MIN(" + c + ") AS min_val, MAX(" + c + ") AS max_val,"
                + " COUNT(*) AS total_count,"
                + " SUM(CASE WHEN " + c + " IS NULL THEN 1 ELSE 0 END) AS null_count"
                + " FROM " + fromClause + filtersToWhereClause(filters);
    }

    /**
     * Equi-width histogram. We compute bins client-friendly: each row carries
     * bin index, the lower/upper edge and the count. Empty bins are included so
     * the chart can render them as zero bars without extra logic.
     */
    protected String buildHistogramBinsSql(String fromClause, String column, List<AbstractFilter> filters,
            int buckets, double minVal, double maxVal) {
        String c = getCorrectedString(column);
        double range = maxVal - minVal;
        String where = filtersToWhereClause(filters);
        String notNull = (where.isEmpty() ? " WHERE " : " AND ") + c + " IS NOT NULL";

        return "WITH bins AS ("
                + "  SELECT LEAST(CAST(((" + c + " - " + minVal + ") / " + range + ") * " + buckets
                + " AS INTEGER), " + (buckets - 1) + ") AS bucket"
                + "  FROM " + fromClause + where + notNull
                + "), counts AS (SELECT bucket, COUNT(*) AS cnt FROM bins GROUP BY bucket),"
                + " axis AS (SELECT UNNEST(range(0, " + buckets + ")) AS bucket)"
                + " SELECT axis.bucket AS bucket,"
                + " " + minVal + " + axis.bucket * " + range + " / " + buckets + " AS bin_lo,"
                + " " + minVal + " + (axis.bucket + 1) * " + range + " / " + buckets + " AS bin_hi,"
                + " COALESCE(counts.cnt, 0) AS count"
                + " FROM axis LEFT JOIN counts ON axis.bucket = counts.bucket"
                + " ORDER BY axis.bucket";
    }

    private String sanitizeIdent(String name) {
        if (name == null) return "col";
        return name.toLowerCase().replaceAll("[^a-zA-Z0-9_]", "_");
    }

    /**
     * Reservoir-sample N rows. DuckDB's `USING SAMPLE n ROWS (RESERVOIR)` is the
     * idiomatic way; it's a true random sample, not the first N rows.
     */
    protected String buildScatterSampleSql(String fromClause, String xColumn, String yColumn, String colorColumn,
            List<AbstractFilter> filters, int sampleSize) {
        String x = getCorrectedString(xColumn);
        String y = getCorrectedString(yColumn);
        StringBuilder cols = new StringBuilder(x).append(", ").append(y);
        if (colorColumn != null && !colorColumn.isBlank()) {
            cols.append(", ").append(getCorrectedString(colorColumn));
        }

        String where = filtersToWhereClause(filters);
        String conn = where.isEmpty() ? " WHERE " : " AND ";
        String notNull = conn + x + " IS NOT NULL AND " + y + " IS NOT NULL";

        return "SELECT " + cols + " FROM " + fromClause + where + notNull
                + " USING SAMPLE " + sampleSize + " ROWS (RESERVOIR)";
    }

    /** Min/max stats for a column with the given filter set. Used to derive bin edges. */
    protected String buildXYStatsSql(String fromClause, String xColumn, String yColumn,
            List<AbstractFilter> filters) {
        String x = getCorrectedString(xColumn);
        String y = getCorrectedString(yColumn);
        String where = filtersToWhereClause(filters);
        String conn = where.isEmpty() ? " WHERE " : " AND ";
        return "SELECT MIN(" + x + ") AS x_min, MAX(" + x + ") AS x_max,"
                + " MIN(" + y + ") AS y_min, MAX(" + y + ") AS y_max,"
                + " COUNT(*) AS total_count"
                + " FROM " + fromClause + where + conn
                + x + " IS NOT NULL AND " + y + " IS NOT NULL";
    }

    /**
     * 2D rectangular binning. Returns one row per non-empty bin with its edges
     * and count. Empty bins are skipped — heatmap-style rendering doesn't need them.
     */
    protected String buildScatterBinSql(String fromClause, String xColumn, String yColumn,
            List<AbstractFilter> filters, int xBuckets, int yBuckets,
            double xMin, double xMax, double yMin, double yMax) {
        String x = getCorrectedString(xColumn);
        String y = getCorrectedString(yColumn);
        double xRange = xMax - xMin;
        double yRange = yMax - yMin;
        String where = filtersToWhereClause(filters);
        String conn = where.isEmpty() ? " WHERE " : " AND ";

        return "WITH binned AS ("
                + "  SELECT"
                + "    LEAST(CAST(((" + x + " - " + xMin + ") / " + xRange + ") * " + xBuckets
                + " AS INTEGER), " + (xBuckets - 1) + ") AS x_bin,"
                + "    LEAST(CAST(((" + y + " - " + yMin + ") / " + yRange + ") * " + yBuckets
                + " AS INTEGER), " + (yBuckets - 1) + ") AS y_bin"
                + "  FROM " + fromClause + where + conn
                + x + " IS NOT NULL AND " + y + " IS NOT NULL"
                + ") SELECT x_bin, y_bin,"
                + " " + xMin + " + x_bin * " + xRange + " / " + xBuckets + " AS x_lo,"
                + " " + xMin + " + (x_bin + 1) * " + xRange + " / " + xBuckets + " AS x_hi,"
                + " " + yMin + " + y_bin * " + yRange + " / " + yBuckets + " AS y_lo,"
                + " " + yMin + " + (y_bin + 1) * " + yRange + " / " + yBuckets + " AS y_hi,"
                + " COUNT(*) AS count"
                + " FROM binned GROUP BY x_bin, y_bin";
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
