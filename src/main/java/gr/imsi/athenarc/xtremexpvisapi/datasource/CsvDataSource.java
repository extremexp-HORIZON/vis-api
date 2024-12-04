package gr.imsi.athenarc.xtremexpvisapi.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import gr.imsi.athenarc.visual.middleware.cache.MinMaxCache;
import gr.imsi.athenarc.visual.middleware.datasource.QueryExecutor.CsvQueryExecutor;
import gr.imsi.athenarc.visual.middleware.domain.Dataset.CsvDataset;
import gr.imsi.athenarc.visual.middleware.domain.Query.Query;
import gr.imsi.athenarc.visual.middleware.util.DateTimeUtil;
import gr.imsi.athenarc.xtremexpvisapi.domain.*;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TabularQuery;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TimeSeriesQuery;
import jakarta.annotation.PostConstruct;
import tech.tablesaw.api.*;
import tech.tablesaw.columns.*;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.util.concurrent.ConcurrentHashMap;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

@Component
public class CsvDataSource implements DataSource {
    private static final Logger LOG = LoggerFactory.getLogger(CsvDataSource.class);

    private final TabularQueryExecutor tabularQueryExecutor;
    private final TimeSeriesQueryExecutor timeSeriesQueryExecutor;
    private final ConcurrentHashMap<Path, Table> tableCache = new ConcurrentHashMap<>();
    private String source;
    
    // Map to hold the minmaxcache of each dataset, for time series exploration
    private final ConcurrentHashMap<String, MinMaxCache> cacheMap = new ConcurrentHashMap<>();
    @Autowired
    @Value("${app.working.directory}")
    private String workingDirectory;

    public CsvDataSource() {
        this.tabularQueryExecutor = new TabularQueryExecutor();
        this.timeSeriesQueryExecutor = new TimeSeriesQueryExecutor();
    }

    @PostConstruct
    private void init() {
        if (workingDirectory == null || workingDirectory.isBlank()) {
            throw new IllegalStateException("Working directory is not configured properly.");
        }
        LOG.info("Working directory initialized: {}", workingDirectory);
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public TimeSeriesResponse fetchTimeSeriesData(TimeSeriesQuery timeSeriesQuery) {
        TimeSeriesResponse timeSeriesResponse = new TimeSeriesResponse();
        switch(timeSeriesQuery.getDataReduction().getType()){
            case "raw":
                timeSeriesResponse = timeSeriesRawQuery(timeSeriesQuery);
                break;
            case "aggregation":
                break;
            case "visualization-aware":
                timeSeriesResponse = timeSeriesVisualQuery(timeSeriesQuery);
                break;
        }
        return timeSeriesResponse;
        
    }
   
   
    @Override
    public TabularResults fetchTabularData(TabularQuery tabularQuery) {
        TabularResults tabularResults = new TabularResults();
        Path path = Paths.get(source);
        if (Files.isDirectory(path)) {
            List<Table> tables = getTablesFromPath(path);
            List<String> jsonDataList = new ArrayList<>();
            List<TabularColumn> columns = new ArrayList<>();
            for (Table table : tables) {
                QueryResult queryResult = tabularQueryExecutor.queryTabularData(table, tabularQuery);
                Table resultsTable = queryResult.getResultTable();
                jsonDataList.add(getJsonDataFromTableSawTable(resultsTable));
                if (columns.isEmpty()) {
                    columns.addAll(
                        resultsTable.columns().stream().map(this::getTabularColumnFromTableSawColumn).toList());
                }
            }
            LOG.debug("{}", tables.stream().map(table -> table.name()).toList());
            tabularResults.setFileNames(tables.stream().map(table -> table.name()).toList());
            tabularResults.setData("[" + String.join(",", jsonDataList) + "]");
            tabularResults.setColumns(columns);
        } else if (Files.isRegularFile(path)) {
            if (tabularQuery.getDatasetId().endsWith(".json")) {
                String json = readJsonFromFile(path);
                tabularResults.setData(json);
            } else {
                    Table table = readCsvFromFile(path);
                    QueryResult queryResult = tabularQueryExecutor.queryTabularData(table, tabularQuery);
                    Table resultsTable = queryResult.getResultTable();
                    Map<String, List<Object>>uniqueColumnValues = getUniqueValuesForColumns(table, table.columns().stream().map(this::getTabularColumnFromTableSawColumn).toList());

                    tabularResults.setFileNames(Arrays.asList(new String[]{table.name()}));
                    tabularResults.setData(getJsonDataFromTableSawTable(resultsTable));
                    tabularResults.setColumns(resultsTable.columns().stream().map(this::getTabularColumnFromTableSawColumn).toList());
                    tabularResults.setOriginalColumns(table.columns().stream().map(this::getTabularColumnFromTableSawColumn).toList());
                    tabularResults.setTotalItems(table.rowCount()); // Add this line to return total items
                    tabularResults.setQuerySize(queryResult.getRowCount()); // Set the filtered row count here
                    tabularResults.setUniqueColumnValues(uniqueColumnValues);
                }
            }
        return tabularResults;
    }
    
    public TabularColumn getTimestampColumn() {
        Path path = Paths.get(source);
        Table table = readCsvFromFile(path);
        boolean hasHeader = table.columnNames().size() > 0;
        if (!hasHeader)
            return null;
        for (int i = 0; i < table.columnCount(); i++) {
            ColumnType columnType = table.column(i).type();
            if (columnType == ColumnType.LOCAL_DATE_TIME || columnType == ColumnType.INSTANT) {
                return getTabularColumnFromTableSawColumn(table.column(i));
            }
        }
        return null;
    }

    @Override
    public List<TabularColumn> getColumns() {
        Table table = getTablesFromPath(Paths.get(getSource())).get(0); // Assuming the first table for
                                                                                    // simplicity
        return table.columns().stream().map(this::getTabularColumnFromTableSawColumn).toList();
    }

    private TimeSeriesResponse timeSeriesRawQuery(TimeSeriesQuery timeSeriesQuery){
        TimeSeriesResponse timeSeriesResponse = new TimeSeriesResponse();
        Path path = Paths.get(source); 
        // Directory logic
        if (Files.isDirectory(path)) {
            // TODO: Implement directory logic
        } else if (Files.isRegularFile(path)) {
            Table table = readCsvFromFile(path);
            Table resultsTable = timeSeriesQueryExecutor.queryTabularData(table, timeSeriesQuery);
            // timeSeriesResponse.setFileNames(Arrays.asList(new String[]{table.name()}));
            timeSeriesResponse.setData(getJsonDataFromTableSawTable(resultsTable));
            timeSeriesResponse.setColumns(
                table.columns().stream().map(this::getTabularColumnFromTableSawColumn).toList()
            );
        } else {
            throw new IllegalArgumentException("Invalid path: " + source);
        }
        return timeSeriesResponse;
    }

    private TimeSeriesResponse timeSeriesVisualQuery(TimeSeriesQuery timeSeriesQuery) {
        TimeSeriesResponse timeSeriesResponse = new TimeSeriesResponse();
        
        // Extract details from the query
        DataReduction dataReduction = timeSeriesQuery.getDataReduction();
        double errorBound = dataReduction.getErrorBound();
        int width = dataReduction.getViewport().getWidth();
        int height = dataReduction.getViewport().getHeight();
        String datasetId = timeSeriesQuery.getDatasetId();
        long from = DateTimeUtil.parseDateTimeString(timeSeriesQuery.getFrom());
        long to = DateTimeUtil.parseDateTimeString(timeSeriesQuery.getTo());
        
        // Metadata for CsvDataset
        String timeFormat = "yyyy-MM-dd'T'HH:mm:ss"; // Example: Adjust as per your dataset
        String delimiter = ","; // Example: Adjust as per your dataset
        boolean hasHeader = true; // Assume true; adjust as needed
        if(timeSeriesQuery.getTimestampColumn() == null){
            TabularColumn timestampColumn = getTimestampColumn();
            if(timestampColumn != null){
                timeSeriesQuery.setTimestampColumn(timestampColumn.getName());
            }
            else{
                LOG.error("No timestamp column found for datasetId: {}", datasetId);
                throw new IllegalArgumentException("No timestamp column found for datasetId: " + datasetId);
            }
        }
        String timestampColumnName = timeSeriesQuery.getTimestampColumn();

        // Extract measures as indices
        List<String> measureNames = timeSeriesQuery.getColumns(); // Get measure names from the query
        List<Integer> measureIndices = new ArrayList<>();
        Path path = Paths.get(source);
    
        // Read the CSV file
        Table table = readCsvFromFile(path);
    
        // Map measure names to indices
        for (String measureName : measureNames) {
            int index = table.columnIndex(measureName);
            if (index == -1) {
                throw new IllegalArgumentException("Measure name not found: " + measureName);
            }
            measureIndices.add(index);
        }
    
        // Construct the CsvDataset
        CsvDataset cacheDataset;
        try {
            cacheDataset = new CsvDataset(
                source, source, "csv", source, timeFormat, timestampColumnName, delimiter, hasHeader
            );
        } catch (IOException e) {
            throw new RuntimeException("Error creating CsvDataset: " + e.getMessage(), e);
        }
    
        // Construct the Query
        Query cacheQuery = new Query(
            from, to, measureIndices, (float) (1 - errorBound), width, height, null
        );
    
        // Fetch or create the MinMaxCache
        MinMaxCache minMaxCache = cacheMap.computeIfAbsent(datasetId, key -> {
            try {
                CsvQueryExecutor minMaxCacheQueryExecutor =
                    new CsvQueryExecutor(cacheDataset);
                return new MinMaxCache(minMaxCacheQueryExecutor, cacheDataset, 0.5, 4, 6);
            } catch (IOException e) {
                LOG.error("Failed to initialize MinMaxCache for dataset: {}", datasetId, e);
                throw new RuntimeException("Error initializing MinMaxCache: " + e.getMessage(), e);
            }
        });
    
        // Execute the query
        if (minMaxCache == null) {
            throw new IllegalStateException("MinMaxCache is not initialized for dataset: " + datasetId);
        }
    
        minMaxCache.executeQuery(cacheQuery);
    
        // Prepare the response
        // timeSeriesResponse.setDatasetId(datasetId);
        // timeSeriesResponse.setCache(minMaxCache);
    
        LOG.info("Time series visual query executed successfully for dataset: {}", datasetId);
    
        return timeSeriesResponse;
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
        // Check if the table is already cached
        if (tableCache.containsKey(filePath)) {
            return tableCache.get(filePath);
        }
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            CsvReadOptions csvReadOptions = createCsvReadOptions(inputStream);
            Table table = Table.read().usingOptions(csvReadOptions).setName(filePath.getFileName().toString());
            tableCache.put(filePath, table);
            return table;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read CSV from file", e);
        }
    }
    
    public void setSource(String source){
        this.source = normalizeSource(source);  
    }

    private String normalizeSource(String source){
        return Path.of(workingDirectory, source.replace("file://", "")).toString();
    }

    private CsvReadOptions createCsvReadOptions(InputStream inputStream) {
        return CsvReadOptions.builder(inputStream).build();
    }

    private TabularColumn getTabularColumnFromTableSawColumn(Column<?> tableSawColumn) {
        String name = tableSawColumn.name();
        ColumnType type = tableSawColumn.type();
        return new TabularColumn(name, type.name());
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
    
    private Map<String, List<Object>> getUniqueValuesForColumns(Table table, List<TabularColumn> tabularColumns) {
        Map<String, List<Object>> uniqueValues = new HashMap<>();

        // Iterate through all the visual columns passed
        for (TabularColumn tabularColumn : tabularColumns) {
            String columnName = tabularColumn.getName();
            Column<?> column = table.column(columnName);

            // Fetch the unique values for each column and store them in the map
            List<Object> uniqueColumnValues = (List<Object>) column.unique().asList();
                uniqueValues.put(columnName, uniqueColumnValues);
        }
        
        return uniqueValues;
    }

}



