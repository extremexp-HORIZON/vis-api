package gr.imsi.athenarc.xtremexpvisapi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.Column;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.DatasetMeta;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.FileType;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.aggregation.Aggregation;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.filter.AbstractFilter;
import lombok.extern.java.Log;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.SourceType;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.DataRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.DataResponse;

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Log
public class DataQueryService {

    @Autowired
    private Connection duckdbConnection;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.file.cache.directory}")
    private String fileCacheDirectory;

    private FileService fileService;
    private DataManagementService dataManagementService;

    @Autowired
    public DataQueryService(FileService fileService, DataManagementService dataManagementService) {
        this.fileService = fileService;
        this.dataManagementService = dataManagementService;
    }

    private FileType detectFileType(String datasetId) {
        for (FileType fileType : FileType.values()) {
            if (datasetId.toLowerCase().endsWith(fileType.getExtension())) {
                return fileType;
            }
        }
        return FileType.CSV; // Default fallback
    }

    @Async
    public CompletableFuture<DataResponse> executeDataRequest(DataRequest request) throws SQLException, Exception {
        return buildQuery(request).thenCompose(sql -> {
            try {
                Statement statement = duckdbConnection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql);

                DataResponse response = convertResultSetToTabularResponse(resultSet, sql);

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

    // Convert ResultSet to TabularResponse
    private DataResponse convertResultSetToTabularResponse(ResultSet resultSet, String query) throws SQLException {
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

    private String convertDataToJson(List<Map<String, Object>> dataRows) {
        try {
            return objectMapper.writeValueAsString(dataRows);
        } catch (Exception e) {
            // Log the error and return a fallback
            System.err.println("Error converting data to JSON: " + e.getMessage());
            return "[]"; // Return empty JSON array as fallback
        }
    }

    // Helper method to map SQL types to readable strings
    private String mapSqlTypeToString(int sqlType) {
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

    @Async
    public CompletableFuture<String> buildQuery(DataRequest request) throws Exception {
        return getFilePathForDataset(request).thenApply(datasetPath -> {
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

            // GROUP BY clause
            if (request.getGroupBy() != null && !request.getGroupBy().isEmpty()) {
                sql.append(" GROUP BY ");
                sql.append(String.join(", ", request.getGroupBy()));
            }

            // Handle aggregation
            if (request.getAggregations() != null && !request.getAggregations().isEmpty()) {
                sql = buildAggregationQuery(request, sql);
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

    private String buildFiltersClause(List<AbstractFilter> filters) {
        return filters.stream()
                .map(AbstractFilter::toSql)
                .filter(sql -> !sql.equals("1=1")) // Remove no-op filters
                .collect(Collectors.joining(" AND "));
    }

    private StringBuilder buildAggregationQuery(DataRequest request, StringBuilder baseQuery) {
        StringBuilder aggQuery = new StringBuilder("SELECT ");

        // Add group by columns first
        if (request.getGroupBy() != null && !request.getGroupBy().isEmpty()) {
            aggQuery.append(String.join(", ", request.getGroupBy())).append(", ");
        }

        // Add aggregation functions
        if (request.getAggregations() != null && !request.getAggregations().isEmpty()) {
            List<String> aggSqls = request.getAggregations().stream()
                    .map(Aggregation::toSql)
                    .collect(Collectors.toList());
            aggQuery.append(String.join(", ", aggSqls));
        } else {
            // If no aggregations specified but we're in aggregation mode, default to
            // COUNT(*)
            aggQuery.append("COUNT(*)");
        }

        // Add FROM clause as subquery
        aggQuery.append(" FROM (").append(baseQuery).append(") as subquery");

        // Add GROUP BY if present
        if (request.getGroupBy() != null && !request.getGroupBy().isEmpty()) {
            aggQuery.append(" GROUP BY ").append(String.join(", ", request.getGroupBy()));
        }

        return aggQuery;
    }

    /**
     * Helper function to correct the string representation of a column name.
     * If the column name contains spaces, it will be wrapped in double quotes.
     *
     * @param input the input string to correct
     * @return the corrected string
     */
    private String getCorrectedString(String input) {
        // Implement your correction logic here
        return input.contains(" ") ? "\"" + input + "\"" : input;
    }

    /**
     * Helper function to get the file path for both INTERNAL and EXTERNAL datasets.
     * For INTERNAL datasets, returns the source path directly.
     * For EXTERNAL datasets, checks cache and downloads if necessary.
     *
     * @param request the request containing dataset metadata
     * @return the file path (local path for external files, source path for
     *         internal files)
     * @throws Exception if download fails
     */
    @Async
    private CompletableFuture<String> getFilePathForDataset(DataRequest request) throws Exception {
        // Assuming request has a method to get DatasetMeta and file type
        String targetPath;
        DatasetMeta datasetMeta = request.getDatasetMeta(); // Assuming this method exists
        SourceType fileType = datasetMeta.getType(); // Assuming this method exists

        if (fileType.equals(SourceType.INTERNAL)) {
            targetPath = datasetMeta.getSource();
            log.info("Internal file detected, using source path: " + targetPath);
            return CompletableFuture.completedFuture(targetPath);
        } else if (fileType.equals(SourceType.EXTERNAL)) {
            String filePath = Paths.get(fileCacheDirectory, datasetMeta.getProjectId(),
                    datasetMeta.getFileName()).toString();
            log.info("External file detected, checking cache for: " + filePath);

            if (fileService.isFileCached(filePath)) {
                log.info("File found in cache: " + filePath);
                return CompletableFuture.completedFuture(filePath);
            } else {
                log.info("File not in cache, downloading: " + datasetMeta.getFileName());
                return dataManagementService.downloadFile(datasetMeta);
            }
        } else {
            throw new IllegalArgumentException("Unknown file type: " + fileType);
        }
    }
}
