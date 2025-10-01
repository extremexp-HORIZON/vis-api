package gr.imsi.athenarc.xtremexpvisapi.service.dataService.v2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.java.Log;
import tagbio.umap.Umap;
import gr.imsi.athenarc.xtremexpvisapi.domain.metadata.DatasetType;
import gr.imsi.athenarc.xtremexpvisapi.domain.metadata.MetadataResponseV2;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryv2.DataRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryv2.DataResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryv2.params.Column;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryv2.params.DataSource;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryv2.params.FileType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
                // System.out.println("File path for metadata: " + filePath.toString());

                FileType fileType = dataQueryHelper.detectFileType(filePath);
                if (fileType == FileType.JSON) {
                    try {
                        filePath = preprocessJsonIfNeeded(Paths.get(filePath)).toString();
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to preprocess JSON file: " + filePath, e);
                    }
                }

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

                DatasetType datasetType = dataQueryHelper.detectDatasetType(resultSet, timeColumns, statement, sql);

                metadataResponse.setOriginalColumns(convertedColumns);
                metadataResponse.setTotalItems(totalItems);
                metadataResponse.setDatasetType(datasetType);
                metadataResponse.setHasLatLonColumns(dataQueryHelper.hasLatLonColumns(convertedColumns));

                if (!timeColumns.isEmpty()) {
                    metadataResponse.setTimeColumn(timeColumns);
                }

                try {
                    sql = sql.replace(" LIMIT 10", "");
                    String summarizeSql = "SUMMARIZE " + sql.substring(sql.indexOf("FROM"));
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

                return metadataResponse;

            } catch (SQLException e) {
                throw new RuntimeException("Failed to get file metadata", e);
            }
        });

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

}
