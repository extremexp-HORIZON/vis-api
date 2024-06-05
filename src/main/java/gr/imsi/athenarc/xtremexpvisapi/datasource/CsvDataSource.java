package gr.imsi.athenarc.xtremexpvisapi.datasource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import gr.imsi.athenarc.xtremexpvisapi.domain.VisualColumn;
import gr.imsi.athenarc.xtremexpvisapi.domain.VisualizationResults;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.VisualQuery;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;
import tech.tablesaw.io.csv.CsvReadOptions;

@Component
public class CsvDataSource implements DataSource {

    @Value("${app.working.directory}")
    public String workingDirectory;

    public String source;
    public CsvQueryExecutor csvQueryExecutor;

    public CsvDataSource(String source) {
        this.source = source;
        this.csvQueryExecutor = new CsvQueryExecutor();
    }
   
    @Override
    public String getSource() {
        return source;
    }

    @Override
    public VisualizationResults fetchData(VisualQuery visualQuery) {
        Table table = getTable(source);
        Table resultsTable = csvQueryExecutor.queryTable(table, visualQuery);
        VisualizationResults visualizationResults = new VisualizationResults();
        visualizationResults.setData(getJsonDataFromTableSawTable(resultsTable));
        return visualizationResults;
    }

    @Override
    public String getColumn(String source, String columnName) {
        Table table = getTable(source);
        return getJsonDataFromTableSawTable(table.selectColumns(new String[]{columnName}));
    }

    @Override
    public List<VisualColumn> getColumns(String source) {
        Table table = getTable(source);    
        return table.columns().stream().map(col -> getVisualColumnFromTableSawColumn(col)).toList();
    }

    private Table getTable(String source){
        Table table;
        if (isLocalFile(source)) {
            table = readCsvFromFile(workingDirectory + source.replace("file://", ""));
        } else { // zenoh
            table = readCsvFromApi(source.replace("zenoh://", ""));
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

    private Table readCsvFromApi(String apiUrl) {
        // TODO: Implement Zenoh File Fetch
        return null;
    }

    private CsvReadOptions createCsvReadOptions(InputStream inputStream) {
        return CsvReadOptions
                    .builder(inputStream)
                    .build();
    }

    private VisualColumn getVisualColumnFromTableSawColumn(Column tableSawColumn){
        String name = tableSawColumn.name();
        ColumnType type = tableSawColumn.type();
        return new VisualColumn(name, type.name());
    }

    private String getJsonDataFromTableSawTable(Table table) {
        List<String> columnNames = table.columnNames();
            columnNames.stream().map((s) -> {
            return s;
        });

        Spliterator<Row> spliterator = Spliterators.spliteratorUnknownSize(table.iterator(), 0);
        Stream<Row> rowStream = StreamSupport.stream(spliterator, false);

        String rowsJsonArray = rowStream.map((row) -> {
        String jsonKeyValues = columnNames.stream().map((columnName) -> {
            String propertyVal = "";
            Object columnValue = row.getObject(columnName);
            ColumnType colType = table.column(columnName).type();
            if (colType == ColumnType.STRING || colType == ColumnType.LOCAL_DATE_TIME) {
                propertyVal = "\""+ columnName +"\""+":"+ "\""+columnValue+"\"";
            } else {
                propertyVal = "\""+ columnName +"\""+":"+columnValue;
            }
            return propertyVal;
            }).collect(Collectors.joining(","));
                return "{"+ jsonKeyValues + "}";
        }).collect(Collectors.joining(","));

        String finalString = "["+ rowsJsonArray +"]";

        return finalString;
    }
    
}
