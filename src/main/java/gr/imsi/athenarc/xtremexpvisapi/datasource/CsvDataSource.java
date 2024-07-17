package gr.imsi.athenarc.xtremexpvisapi.datasource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import gr.imsi.athenarc.xtremexpvisapi.service.DataService;
import gr.imsi.athenarc.xtremexpvisapi.domain.*;
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

    private final CsvQueryExecutor csvQueryExecutor;
    private final DataService dataService;

    @Value("${app.working.directory}")
    private String workingDirectory;

    @Autowired
    public CsvDataSource(DataService dataService) {
        this.csvQueryExecutor = new CsvQueryExecutor();
        this.dataService = dataService;
    }

    @Override
    public String getSource() {
        throw new UnsupportedOperationException("getSource() is not used.");
    }

    @Override
    public VisualizationResults fetchData(VisualQuery visualQuery) {

        VisualizationResults visualizationResults = new VisualizationResults();

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
                if(columns.isEmpty()){
                    columns.addAll(resultsTable.columns().stream().map(this::getVisualColumnFromTableSawColumn).toList());
                }
                if (timestampColumn.isEmpty()) {
                    timestampColumn = getTimestampColumn(resultsTable);
                }
            }
            visualizationResults.setData("[" + String.join(",", jsonDataList) + "]");
            visualizationResults.setColumns(columns);
            visualizationResults.setTimestampColumn(timestampColumn);
        } else if(visualQuery.getDatasetId().startsWith("file://")){
            String source = normalizeSource(visualQuery.getDatasetId());
            Path path = Paths.get(source);
            Table table = readCsvFromFile(path);
            Table resultsTable = csvQueryExecutor.queryTable(table, visualQuery);
            visualizationResults.setData(getJsonDataFromTableSawTable(resultsTable));
            visualizationResults.setColumns(resultsTable.columns().stream().map(this::getVisualColumnFromTableSawColumn).toList());
            visualizationResults.setTimestampColumn(getTimestampColumn(resultsTable));
        }
        return visualizationResults;
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

    @Override
    public String getTimestampColumn(Table table) {
        boolean hasHeader = table.columnNames().size() > 0;
        if (!hasHeader) return "";
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
        Table table = getTablesFromPath(Paths.get(normalizeSource(source))).get(0); // Assuming the first table for simplicity
        return getJsonDataFromTableSawTable(table.selectColumns(new String[]{columnName}));
    }

    @Override
    public List<VisualColumn> getColumns(String source) {
        Table table = getTablesFromPath(Paths.get(normalizeSource(source))).get(0); // Assuming the first table for simplicity
        return table.columns().stream().map(this::getVisualColumnFromTableSawColumn).toList();
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

    private Table readCsvFromFile(Path filePath) {
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            CsvReadOptions csvReadOptions = createCsvReadOptions(inputStream);
            return Table.read().usingOptions(csvReadOptions);
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
}
