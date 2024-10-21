package gr.imsi.athenarc.xtremexpvisapi.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gr.imsi.athenarc.xtremexpvisapi.service.DataService;
import gr.imsi.athenarc.xtremexpvisapi.controller.VisualizationController;
import gr.imsi.athenarc.xtremexpvisapi.domain.*;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TabularQuery;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TimeSeriesQuery;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.VisualQuery;
import tech.tablesaw.api.*;
import tech.tablesaw.columns.*;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

@Component
public class CsvDataSource implements DataSource {
    private static final Logger LOG = LoggerFactory.getLogger(CsvDataSource.class);

    private final CsvQueryExecutor csvQueryExecutor;
    private final DataService dataService;
    private final TabularQueryExecutor tabularQueryExecutor;
    private final TimeSeriesQueryExecutor timeSeriesQueryExecutor;

    @Value("${app.working.directory}")
    private String workingDirectory;

    @Autowired
    public CsvDataSource(DataService dataService) {
        this.csvQueryExecutor = new CsvQueryExecutor();
        this.dataService = dataService;
        this.tabularQueryExecutor = new TabularQueryExecutor();
        this.timeSeriesQueryExecutor = new TimeSeriesQueryExecutor();
    }

    @Override
    public String getSource() {
        throw new UnsupportedOperationException("getSource() is not used.");
    }

    @Override
    public VisualizationResults fetchData(VisualQuery visualQuery) {

        VisualizationResults visualizationResults = new VisualizationResults();
        int totalItemCount = 0; 

        if (visualQuery.getDatasetId().startsWith("folder://")) {
            String source = normalizeSource(visualQuery.getDatasetId());
            Path path = Paths.get(source);
            List<Table> tables = getTablesFromPath(path);
            List<String> jsonDataList = new ArrayList<>();
            List<VisualColumn> columns = new ArrayList<>();
            String timestampColumn = "";

            for (Table table : tables) {
                
                Table resultsTable = csvQueryExecutor.queryTable(table, visualQuery);
                jsonDataList.add(getJsonDataFromTableSawTable(resultsTable));
                if (columns.isEmpty()) {
                    columns.addAll(
                        resultsTable.columns().stream().map(this::getVisualColumnFromTableSawColumn).toList());
                }
                if (timestampColumn.isEmpty()) {
                    timestampColumn = getTimestampColumn(resultsTable);
                }
            }
            LOG.debug("{}", tables.stream().map(table -> table.name()).toList());
            visualizationResults.setFileNames(tables.stream().map(table -> table.name()).toList());
            visualizationResults.setData("[" + String.join(",", jsonDataList) + "]");
            visualizationResults.setColumns(columns);
            visualizationResults.setTimestampColumn(timestampColumn);
        } else if (visualQuery.getDatasetId().startsWith("file://")) {
            if (visualQuery.getDatasetId().endsWith(".json")) {
                // String source = normalizeSource(visualQuery.getDatasetId());
                // Path path = Paths.get(source);
                // Map<String, List<Object>> jsonData = readJsonData(path);
                // LOG.info("jsonData {}", jsonData);
                // List<JsonNode> jsonDataList = convertMapToJsonNodeList(jsonData);  // You will implement this method
                // JsonQueryExecutor jsonQueryExecutor = new JsonQueryExecutor();
                // List<JsonNode> filteredData = jsonQueryExecutor.queryJson(jsonDataList, visualQuery);
                // String jsonString = filteredData.stream()
                // .map(JsonNode::toString)  // Convert each JsonNode to its string representation
                // .collect(Collectors.joining(",", "[", "]")); 
                // List<String> columnNames = new ArrayList<>(jsonData.keySet());
                // visualizationResults.setFileNames(Collections.singletonList(path.getFileName().toString()));
                // visualizationResults.setData(jsonString);
                // visualizationResults.setColumns(columnNames.stream()
                // .map(col -> new VisualColumn(col, "string"))  // Set appropriate data types
                // .toList());
                visualizationResults.setTimestampColumn(""); 
            } else {
                String source = normalizeSource(visualQuery.getDatasetId());
                Path path = Paths.get(source);
                Table table = readCsvFromFile(path);
                Table resultsTable = csvQueryExecutor.queryTable(table, visualQuery);
                totalItemCount = resultsTable.rowCount();
                visualizationResults.setFileNames(Arrays.asList(new String[]{table.name()}));
                visualizationResults.setData(getJsonDataFromTableSawTable(resultsTable));
                visualizationResults.setColumns(
                    resultsTable.columns().stream().map(this::getVisualColumnFromTableSawColumn).toList()
                    );
                visualizationResults.setTimestampColumn(getTimestampColumn(resultsTable));
                visualizationResults.setTotalItems(totalItemCount); // Add this line to return total items

            }
        }
        return visualizationResults;
    }


    @Override
    public TimeSeriesResponse fetchTimeSeriesData(TimeSeriesQuery timeSeriesQuery) {
        TimeSeriesResponse timeSeriesResponse = new TimeSeriesResponse();

        if (timeSeriesQuery.getDatasetId().startsWith("folder://")) {
            LOG.info("prepei na ftiaksw to FOLDER ");
           
        } else if (timeSeriesQuery.getDatasetId().startsWith("file://")) {
            if (timeSeriesQuery.getDatasetId().endsWith(".json")) {
                LOG.info("Prepei  na ftiaksw to JSON");

            } else {
                String source = normalizeSource(timeSeriesQuery.getDatasetId());
                Path path = Paths.get(source);
                Table table = readCsvFromFile(path);
                LOG.info("table {}",table);
                Table resultsTable = timeSeriesQueryExecutor.queryTabularData(table, timeSeriesQuery);
                // timeSeriesResponse.setFileNames(Arrays.asList(new String[]{table.name()}));
                timeSeriesResponse.setData(getJsonDataFromTableSawTable(resultsTable));
                timeSeriesResponse.setColumns(
                    table.columns().stream().map(this::getVisualColumnFromTableSawColumn).toList()
                    );
                // // tabularResults.setTimestampColumn(getTimestampColumn(resultsTable));
                // timeSeriesResponse.setTotalItems(resultsTable.rowCount()); // Add this line to return total items

            }
        }
        return timeSeriesResponse;
        
    }
   
   
    @Override
    public TabularResults fetchTabularData(TabularQuery tabularQuery) {
        TabularResults tabularResults = new TabularResults();

        if (tabularQuery.getDatasetId().startsWith("folder://")) {
            // String source = normalizeSource(tabularQuery.getDatasetId());
            // Path path = Paths.get(source);
            // List<Table> tables = getTablesFromPath(path);
            // List<String> jsonDataList = new ArrayList<>();
            // List<VisualColumn> columns = new ArrayList<>();
            // String timestampColumn = "";

            // for (Table table : tables) {
                
            //     Table resultsTable = csvQueryExecutor.queryTable(table, visualQuery);
            //     jsonDataList.add(getJsonDataFromTableSawTable(resultsTable));
            //     if (columns.isEmpty()) {
            //         columns.addAll(
            //             resultsTable.columns().stream().map(this::getVisualColumnFromTableSawColumn).toList());
            //     }
            //     if (timestampColumn.isEmpty()) {
            //         timestampColumn = getTimestampColumn(resultsTable);
            //     }
            // }
            // LOG.debug("{}", tables.stream().map(table -> table.name()).toList());
            // tabularResults.setFileNames(tables.stream().map(table -> table.name()).toList());
            // tabularResults.setData("[" + String.join(",", jsonDataList) + "]");
            // tabularResults.setColumns(columns);
            // tabularResults.setTimestampColumn(timestampColumn);
            } else if (tabularQuery.getDatasetId().startsWith("file://")) {
                if (tabularQuery.getDatasetId().endsWith(".json")) {
                    LOG.info("Processing JSON file...");

                    String source = normalizeSource(tabularQuery.getDatasetId());
                    Path path = Paths.get(source);
                    Map<String, List<Object>> jsonData = readJsonData(path);
                    List<String> originalColumnNames = new ArrayList<>(jsonData.keySet());

                    tabularQuery.populateAllColumnsIfEmpty(jsonData);

                    // LOG.info("jsonData {}", jsonData);
                    List<JsonNode> jsonDataList = convertMapToJsonNodeList(jsonData);  // You will implement this method
                    JsonQueryExecutor jsonQueryExecutor = new JsonQueryExecutor();
                    List<JsonNode> filteredData = jsonQueryExecutor.queryJson(jsonDataList, tabularQuery);
                    String jsonString = filteredData.stream()
                    .map(JsonNode::toString)  // Convert each JsonNode to its string representation
                    .collect(Collectors.joining(",", "[", "]")); 
                    // List<String> columnNames = new ArrayList<>(jsonData.keySet());
                    List<String> columnNames = tabularQuery.getColumns();  // Use the columns from the query (populated if empty)

                    tabularResults.setFileNames(Collections.singletonList(path.getFileName().toString()));
                    tabularResults.setData(jsonString);
                    List<VisualColumn> originalColumns = originalColumnNames.stream()
                    .map(col -> new VisualColumn(col, "string"))  // Assuming all columns are of type 'string'
                    .toList();
                    tabularResults.setOriginalColumns(originalColumns); 

                    tabularResults.setColumns(columnNames.stream()
                    .map(col -> new VisualColumn(col, "string"))  // Set appropriate data types
                    .toList());
                    } else {
                        String source = normalizeSource(tabularQuery.getDatasetId());
                        Path path = Paths.get(source);
                        Table table = readCsvFromFile(path);
                        // Table resultsTable = tabularQueryExecutor.queryTabularData(table, tabularQuery);
                        QueryResult queryResult = tabularQueryExecutor.queryTabularData(table, tabularQuery);
                        Table resultsTable = queryResult.getResultTable();
                        Map<String, List<Object>>uniqueColumnValues = getUniqueValuesForColumns(table, table.columns().stream().map(this::getVisualColumnFromTableSawColumn).toList());

                        tabularResults.setFileNames(Arrays.asList(new String[]{table.name()}));
                        tabularResults.setData(getJsonDataFromTableSawTable(resultsTable));
                        tabularResults.setColumns(resultsTable.columns().stream().map(this::getVisualColumnFromTableSawColumn).toList());
                        tabularResults.setOriginalColumns(table.columns().stream().map(this::getVisualColumnFromTableSawColumn).toList());
                        tabularResults.setTotalItems(table.rowCount()); // Add this line to return total items
                        tabularResults.setQuerySize(queryResult.getRowCount()); // Set the filtered row count here
                        tabularResults.setUniqueColumnValues(uniqueColumnValues);
                    }
                }
            return tabularResults;
            }


    @Override
    public String getTimestampColumn(Table table) {
        boolean hasHeader = table.columnNames().size() > 0;
        if (!hasHeader)
            return "";
        for (int i = 0; i < table.columnCount(); i++) {
            ColumnType columnType = table.column(i).type();
            if (columnType == ColumnType.LOCAL_DATE_TIME || columnType == ColumnType.INSTANT) {
                return table.column(i).name();
            }
        }
        return null;
    }

    @Override
    public String getColumn(String source, String columnName) {
        Table table = getTablesFromPath(Paths.get(normalizeSource(source))).get(0); // Assuming the first table for
                                                                                    // simplicity
        return getJsonDataFromTableSawTable(table.selectColumns(new String[] { columnName }));
    }

    @Override
    public List<VisualColumn> getColumns(String source) {
        Table table = getTablesFromPath(Paths.get(normalizeSource(source))).get(0); // Assuming the first table for
                                                                                    // simplicity
        return table.columns().stream().map(this::getVisualColumnFromTableSawColumn).toList();
    }


    private String normalizeSource(String source) {
        if (source.startsWith("file://")) {
            return Path.of(workingDirectory, source.replace("file://", "")).toString();
        } else if (source.startsWith("folder://")) {
            return Path.of(workingDirectory, source.replace("folder://", "")).toString();
        } else if (source.startsWith("zenoh://")) {
            return source.replace("zenoh://", "");
        }
        return source;
    }

    private List<Table> getTablesFromPath(Path path) {
        if (Files.isDirectory(path)) {
            return readCsvFromDirectory(path);
        } else {
            return List.of(readCsvFromFile(path));
        }
    }

    private List<Table> readCsvFromDirectory(Path directoryPath) {
        try (Stream<Path> paths = Files.walk(directoryPath)) {
            return paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".csv"))
                    .map(this::readCsvFromFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read CSVs from directory", e);
        }
    }

    private String readJsonFromFile(Path filePath) {
        byte[] jsonData;
        try {
            jsonData = Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JSON from file", e);
        }
        return new String(jsonData);
    }

    private Table readCsvFromFile(Path filePath) {
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            CsvReadOptions csvReadOptions = createCsvReadOptions(inputStream);
            return Table.read().usingOptions(csvReadOptions).setName(filePath.getFileName().toString());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read CSV from file", e);
        }
    }

    private Table readCsvFromZenoh(String apiUrl) {
        try {
            String[] parts = apiUrl.split("/");
            String useCase = parts[0];
            String folder = parts[1];
            String subfolder = parts[2];
            String filename = parts[3];
            String csvData = dataService.fetchZenohData(useCase, folder, subfolder, filename);
            try (InputStream inputStream = new ByteArrayInputStream(csvData.getBytes())) {
                CsvReadOptions csvReadOptions = createCsvReadOptions(inputStream);
                return Table.read().usingOptions(csvReadOptions);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read CSV from Zenoh", e);
        }
    }

    private CsvReadOptions createCsvReadOptions(InputStream inputStream) {
        return CsvReadOptions.builder(inputStream).build();
    }

    private VisualColumn getVisualColumnFromTableSawColumn(Column tableSawColumn) {
        String name = tableSawColumn.name();
        ColumnType type = tableSawColumn.type();
        return new VisualColumn(name, type.name());
    }

    private String getJsonDataFromTableSawTable(Table table) {
        List<String> columnNames = table.columnNames();
        Spliterator<Row> spliterator = Spliterators.spliteratorUnknownSize(table.iterator(), 0);
        Stream<Row> rowStream = StreamSupport.stream(spliterator, false);

        String rowsJsonArray = rowStream.map(row -> {
            String jsonKeyValues = columnNames.stream().map(columnName -> {
                Object columnValue = row.getObject(columnName);
                ColumnType colType = table.column(columnName).type();
                String propertyVal = colType == ColumnType.STRING || colType == ColumnType.LOCAL_DATE_TIME
                        ? "\"" + columnName + "\":\"" + columnValue + "\""
                        : "\"" + columnName + "\":" + columnValue;
                return propertyVal;
            }).collect(Collectors.joining(","));
            return "{" + jsonKeyValues + "}";
        }).collect(Collectors.joining(","));

        return "[" + rowsJsonArray + "]";
    }

    private Map<String, List<Object>> readJsonData(Path jsonFilePath) {
        Map<String, List<Object>> jsonDataMap = new HashMap<>();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(jsonFilePath.toFile());

            if (root.isArray()) {
                for (JsonNode row : root) {
                    Iterator<Map.Entry<String, JsonNode>> fields = row.fields();
                    while (fields.hasNext()) {
                        Map.Entry<String, JsonNode> field = fields.next();
                        String fieldName = field.getKey();
                        JsonNode fieldValue = field.getValue();
                        
                        jsonDataMap.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(fieldValue.asText());
                    }
                }
            } else {
                throw new RuntimeException("Expected JSON array format.");
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to read JSON from file", e);
        }

        return jsonDataMap;
    }

    // Convert JSON data map into a list of rows in JSON format (for each row in the dataset)
    private List<String> convertJsonDataToTable(Map<String, List<Object>> jsonDataMap, List<String> columnNames) {
        List<String> rows = new ArrayList<>();
        int rowCount = jsonDataMap.values().iterator().next().size(); // Assuming all columns have same row count

        for (int i = 0; i < rowCount; i++) {
            StringBuilder rowBuilder = new StringBuilder("{");
            for (int j = 0; j < columnNames.size(); j++) {
                String columnName = columnNames.get(j);
                List<Object> columnData = jsonDataMap.get(columnName);
                rowBuilder.append("\"").append(columnName).append("\": \"").append(columnData.get(i)).append("\"");
                if (j < columnNames.size() - 1) {
                    rowBuilder.append(", ");
                }
            }
            rowBuilder.append("}");
            rows.add(rowBuilder.toString());
        }

        return rows;
    }


    private List<JsonNode> convertMapToJsonNodeList(Map<String, List<Object>> jsonData) {
        ObjectMapper objectMapper = new ObjectMapper();
        List<JsonNode> jsonNodeList = new ArrayList<>();
        
        // Assuming that each entry in the map corresponds to a "column" of data,
        // you can loop through and construct JSON objects for each "row".
        int numRows = jsonData.values().stream().findFirst().get().size();  // Assuming all lists are of equal size.
        
        for (int i = 0; i < numRows; i++) {
            ObjectNode rowNode = objectMapper.createObjectNode();
            
            for (Map.Entry<String, List<Object>> entry : jsonData.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue().get(i);
                rowNode.set(key, objectMapper.valueToTree(value));  // Convert each value to JsonNode.
            }
            
            jsonNodeList.add(rowNode);
        }
        
        return jsonNodeList;
    }
    
private Map<String, List<Object>> getUniqueValuesForColumns(Table table, List<VisualColumn> visualColumns) {
    Map<String, List<Object>> uniqueValues = new HashMap<>();

    // Iterate through all the visual columns passed
    for (VisualColumn visualColumn : visualColumns) {
        String columnName = visualColumn.getName();
        Column<?> column = table.column(columnName);

        // Fetch the unique values for each column and store them in the map
        List<Object> uniqueColumnValues = (List<Object>) column.unique().asList();
            uniqueValues.put(columnName, uniqueColumnValues);
    }
    
    return uniqueValues;
}

    

}



