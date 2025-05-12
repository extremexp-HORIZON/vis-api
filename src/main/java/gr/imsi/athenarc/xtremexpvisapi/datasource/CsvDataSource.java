package gr.imsi.athenarc.xtremexpvisapi.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


import gr.imsi.athenarc.xtremexpvisapi.domain.Metadata.DatasetType;
import gr.imsi.athenarc.xtremexpvisapi.domain.Metadata.MetadataRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.Metadata.MetadataResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.QueryResult;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TabularRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TabularResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TimeSeriesRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TimeSeriesResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.QueryParams.TabularColumn;
import jakarta.annotation.PostConstruct;
import tech.tablesaw.api.*;
import tech.tablesaw.columns.*;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.util.concurrent.ConcurrentHashMap;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.*;

@Component
public class CsvDataSource implements DataSource {
    private static final Logger LOG = LoggerFactory.getLogger(CsvDataSource.class);

    private final TabularQueryExecutor tabularQueryExecutor;
    private final TimeSeriesQueryExecutor timeSeriesQueryExecutor;
    private final ConcurrentHashMap<String, Table> tableCache = new ConcurrentHashMap<>();
    private String source;

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

    public ConcurrentHashMap<String, Table> getTableCache() {
        return tableCache;
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public TimeSeriesResponse fetchTimeSeriesData(TimeSeriesRequest timeSeriesRequest) {
        TimeSeriesResponse timeSeriesResponse = new TimeSeriesResponse();
        switch (timeSeriesRequest.getDataReduction().getType()) {
            case "raw":
                timeSeriesResponse = timeSeriesRawQuery(timeSeriesRequest);
                break;
            case "aggregation":
                break;
        }
        return timeSeriesResponse;

    }

    public boolean hasLatLonColumns(Table table) {
        Set<String> lowerColNames = table.columnNames()
                .stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    
        boolean hasLat = lowerColNames.contains("lat") || lowerColNames.contains("latitude");
        boolean hasLon = lowerColNames.contains("lon") || lowerColNames.contains("long") || lowerColNames.contains("longitude");
    
        return hasLat && hasLon;
    }
    
    @Override
    public TabularResponse fetchTabularData(TabularRequest tabularRequest) {
        TabularResponse tabularResults = new TabularResponse();
        Path path = Paths.get(workingDirectory, source);
        LOG.info("Path: {}", path);
        if (Files.isDirectory(path)) {
            List<Table> tables = getTablesFromPath(path);
            List<String> jsonDataList = new ArrayList<>();
            List<TabularColumn> columns = new ArrayList<>();
            for (Table table : tables) {
                QueryResult queryResult = tabularQueryExecutor.queryTabularData(table, tabularRequest);
                Table resultsTable = queryResult.getResultTable();
                jsonDataList.add(getJsonDataFromTableSawTable(resultsTable));
                if (columns.isEmpty()) {
                    columns.addAll(
                            resultsTable.columns().stream().map(this::getTabularColumnFromTableSawColumn).toList());
                }
            }
            LOG.debug("{}", tables.stream().map(table -> table.name()).toList());
            // tabularResults.setFileNames(tables.stream().map(table -> table.name()).toList());
            tabularResults.setData("[" + String.join(",", jsonDataList) + "]");
            // tabularResults.setColumns(columns);
        } else {
            if (tabularRequest.getDatasetId().endsWith(".json")) {
                String json = readJsonFromFile(path);
                tabularResults.setData(json);
            } else {
                Table table = readCsvFromFile(path);
                QueryResult queryResult = tabularQueryExecutor.queryTabularData(table, tabularRequest);
                Table resultsTable = queryResult.getResultTable();
                tabularResults.setData(getJsonDataFromTableSawTable(resultsTable));
                tabularResults.setTotalItems(table.rowCount()); // Add this line to return total items
                tabularResults.setQuerySize(queryResult.getRowCount()); // Set the filtered row count here
                // Map<String, List<?>> uniqueColumnValues = getUniqueValuesForColumns(table,
                //         table.columns().stream().map(this::getTabularColumnFromTableSawColumn).toList());
                // tabularResults.setFileNames(Arrays.asList(new String[] { table.name() }));
                tabularResults.setColumns(
                        resultsTable.columns().stream().map(this::getTabularColumnFromTableSawColumn).toList());
                // tabularResults.setOriginalColumns(
                //         table.columns().stream().map(this::getTabularColumnFromTableSawColumn).toList());
               
                // tabularResults.setUniqueColumnValues(uniqueColumnValues);
            }
        }
        return tabularResults;
    }

    public TabularColumn getTimestampColumn() {
        Path path = Paths.get(workingDirectory, source);
        if (Files.isDirectory(path)) {
            List<Table> tables = getTablesFromPath(path);
            Table table = tables.get(0);
            boolean hasHeader = table.columnNames().size() > 0;
            if (!hasHeader)
                return null;
            for (int i = 0; i < table.columnCount(); i++) {
                ColumnType columnType = table.column(i).type();
                if (columnType == ColumnType.LOCAL_DATE_TIME ||
                        columnType == ColumnType.LOCAL_DATE ||
                        columnType == ColumnType.INSTANT) {
                    return getTabularColumnFromTableSawColumn(table.column(i));
                }
            }
        } else {
            Table table = readCsvFromFile(path);
            boolean hasHeader = table.columnNames().size() > 0;
            if (!hasHeader)
                return null;
            for (int i = 0; i < table.columnCount(); i++) {
                ColumnType columnType = table.column(i).type();
                if (columnType == ColumnType.LOCAL_DATE_TIME ||
                        columnType == ColumnType.LOCAL_DATE ||
                        columnType == ColumnType.INSTANT) {
                    return getTabularColumnFromTableSawColumn(table.column(i));
                }
            }
        }
        return null;
    }

    public MetadataResponse getFileMetadata(MetadataRequest metadataRequest) {
        Path path = Paths.get(workingDirectory, source);
        Table table = readCsvFromFile(path);
        
        
        // Map<String, List<?>> uniqueColumnValues = getUniqueValuesForColumns(table,
        //         table.columns().stream().map(this::getTabularColumnFromTableSawColumn).toList());

        MetadataResponse metadataResponse = new MetadataResponse();
        metadataResponse.setFileNames(Arrays.asList(new String[] { table.name() }));
        metadataResponse
                .setOriginalColumns(table.columns().stream().map(this::getTabularColumnFromTableSawColumn).toList());
        metadataResponse.setTotalItems(table.rowCount());
        metadataResponse.setUniqueColumnValues(null);
        metadataResponse.setDatasetType(datasetTypeDetection(table));
        metadataResponse.setHasLatLonColumns(hasLatLonColumns(table));
        List<String> timeColumns = new ArrayList<>();
    for (Column<?> column : table.columns()) {
        ColumnType type = column.type();
        if (type.name().equals("LOCAL_DATE_TIME") || 
            type.name().equals("LOCAL_DATE") || 
            type.name().equals("INSTANT")) {
            timeColumns.add(column.name());
        }
    }

    // Set the timeColumn field based on results
    if (!timeColumns.isEmpty()) {
        metadataResponse.setTimeColumn(timeColumns.size() == 1 ? Collections.singletonList(timeColumns.get(0)) : timeColumns);
    }

        return metadataResponse;
    }

    private DatasetType datasetTypeDetection(Table table) {
        boolean hasDateTimeColumn = false;
        for (int i = 0; i < table.columnCount(); i++) {
            ColumnType columnType = table.column(i).type();
            Column<?> dateColumn = table.column(i);
            int rowCount = Math.min(dateColumn.size(), 10);
            switch (columnType.toString()) {
                case "LOCAL_DATE_TIME":
                    for (int j = 1; j < rowCount; j++) {
                        LocalDateTime previousValue = (LocalDateTime) dateColumn.get(j - 1);
                        LocalDateTime currentValue = (LocalDateTime) dateColumn.get(j);
                        if (previousValue.compareTo(currentValue) > 0) {
                            return DatasetType.tabular;
                        }
                    }
                    hasDateTimeColumn = true;
                    break;
                case "LOCAL_DATE":
                    for (int j = 1; j < rowCount; j++) {
                        LocalDate previousValue = (LocalDate) dateColumn.get(j - 1);
                        LocalDate currentValue = (LocalDate) dateColumn.get(j);
                        if (previousValue.compareTo(currentValue) > 0) {
                            return DatasetType.tabular;
                        }
                    }
                    hasDateTimeColumn = true;
                    break;
                case "INSTANT":
                    for (int j = 1; j < rowCount; j++) {
                        Instant previousValue = (Instant) dateColumn.get(j - 1);
                        Instant currentValue = (Instant) dateColumn.get(j);
                        if (previousValue.compareTo(currentValue) > 0) {
                            return DatasetType.tabular;
                        }
                    }
                    hasDateTimeColumn = true;
                    break;
                default:
                    break;
            }
        }
        return hasDateTimeColumn ? DatasetType.timeseries : DatasetType.tabular;
    }

    private TimeSeriesResponse timeSeriesRawQuery(TimeSeriesRequest timeSeriesRequest) {
        TimeSeriesResponse timeSeriesResponse = new TimeSeriesResponse();
        Path path = Paths.get(workingDirectory, source);
        // Directory logic
        if (Files.isDirectory(path)) {
            // TODO: Implement directory logic
        } else {
            Table table = readCsvFromFile(path);
            Table resultsTable = timeSeriesQueryExecutor.queryTabularData(table, timeSeriesRequest);
            // timeSeriesResponse.setFileNames(Arrays.asList(new String[]{table.name()}));
            timeSeriesResponse.setData(getJsonDataFromTableSawTable(resultsTable));
        }
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
            throw new RuntimeException("Failed to read JSON file", e);
        }
        return new String(jsonData);
    }

    public Table readCsvFromFile(Path filePath) {
        String fileName = filePath.getFileName().toString();
        LOG.info("Reading CSV file: {}", fileName);
    
        // Attempt 1: with sample-based type inference
        try (InputStream stream = Files.newInputStream(filePath)) {
            CsvReadOptions options = CsvReadOptions.builder(stream)
                .header(true)
                .sample(true)
                .build();
            return Table.read().usingOptions(options).setName(fileName);
    
        } catch (Exception firstException) {
            LOG.warn("Initial type inference failed for '{}'. Retrying with full parsing (sample=false)", fileName);
    
            // Attempt 2: fallback to more conservative parsing
            try (InputStream retryStream = Files.newInputStream(filePath)) {
                CsvReadOptions fallbackOptions = CsvReadOptions.builder(retryStream)
                    .header(true)
                    .sample(false)
                    .build();
                return Table.read().usingOptions(fallbackOptions).setName(fileName);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read CSV file (even with fallback): " + fileName, e);
            }
        }
    }
    

    public void setSource(String source) {
        this.source = source;
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

    private Map<String, List<?>> getUniqueValuesForColumns(Table table, List<TabularColumn> tabularColumns) {
        Map<String, List<?>> uniqueValues = new HashMap<>();

        // Iterate through all the visual columns passed
        for (TabularColumn tabularColumn : tabularColumns) {
            String columnName = tabularColumn.getName();
            Column<?> column = table.column(columnName);

            // Fetch the unique values for each column and store them in the map
            List<?> uniqueColumnValues = column.unique().asList();
            uniqueValues.put(columnName, uniqueColumnValues);
        }

        return uniqueValues;
    }

}
