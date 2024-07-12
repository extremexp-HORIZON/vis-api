
package gr.imsi.athenarc.xtremexpvisapi.datasource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import gr.imsi.athenarc.xtremexpvisapi.service.DataService;

import gr.imsi.athenarc.xtremexpvisapi.domain.*;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.VisualQuery;
import tech.tablesaw.api.*;
import tech.tablesaw.columns.*;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;



@Component
public class CsvDataSource implements DataSource {

    private final CsvQueryExecutor csvQueryExecutor;
    private final DataService dataService;

    @Autowired
    public CsvDataSource(DataService dataService) {
        this.csvQueryExecutor = new CsvQueryExecutor();
        this.dataService = dataService;
    }

    @Override
    public String getSource() {
        // Not needed anymore, so return null or throw UnsupportedOperationException
        throw new UnsupportedOperationException("getSource() is not used.");
    }

    @Override
    public VisualizationResults fetchData(VisualQuery visualQuery) {
        Table table = getTable(visualQuery.getDatasetId());
        Table resultsTable = csvQueryExecutor.queryTable(table, visualQuery);
        VisualizationResults visualizationResults = new VisualizationResults();
        visualizationResults.setData(getJsonDataFromTableSawTable(resultsTable));
        visualizationResults.setColumns(resultsTable.columns().stream().map(this::getVisualColumnFromTableSawColumn).toList());
        visualizationResults.setTimestampColumn(getTimestampColumn(table));
        return visualizationResults;
    }

    @Override
    public String getTimestampColumn(Table table) {
        boolean hasHeader = table.columnNames().size() > 0;
        if(!hasHeader) return "";
            String timestampCol = null;
            for (int i = 0; i < table.columnCount(); i++) {
                ColumnType columnType = table.column(i).type();
                if (columnType == ColumnType.LOCAL_DATE_TIME || columnType == ColumnType.INSTANT) {
                    timestampCol = table.column(i).name();
                    break;
                }
            }
            return timestampCol;
    }

    @Override
    public String getColumn(String source, String columnName) {
        Table table = getTable(source);
        return getJsonDataFromTableSawTable(table.selectColumns(new String[]{columnName}));
    }

    @Override
    public List<VisualColumn> getColumns(String source) {
        Table table = getTable(source);
        return table.columns().stream().map(this::getVisualColumnFromTableSawColumn).toList();
    }

    private Table getTable(String source){
        Table table;
        if (isLocalFile(source)) {
            table = readCsvFromFile("/data/xtreme/experiments/" + source.replace("file://", ""));
        } else { // zenoh
            table = readCsvFromZenoh(source.replace("zenoh://", ""));
        }
        return table;
    }

    private boolean isLocalFile(String source) {
        return source.startsWith("file://");
    }

    private Table readCsvFromFile(String filePath) {
        try (InputStream inputStream = new FileInputStream(filePath)) {
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