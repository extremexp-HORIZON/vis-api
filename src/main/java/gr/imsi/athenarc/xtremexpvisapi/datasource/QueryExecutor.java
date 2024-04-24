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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gr.imsi.athenarc.xtremexpvisapi.domain.VisualColumn;
import gr.imsi.athenarc.xtremexpvisapi.domain.VisualizationResults;
import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.AbstractFilter;
import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.RangeFilter;
import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.RangeFilter.DateTimeRangeFilter;
import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.RangeFilter.NumberRangeFilter;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.VisualQuery;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;
import tech.tablesaw.io.csv.CsvReadOptions;
import tech.tablesaw.selection.Selection;

public class QueryExecutor {
 
    private static final Logger LOG = LoggerFactory.getLogger(QueryExecutor.class);

    private String tableName;
    private String csvPath;

    public QueryExecutor(String tableName, String csvPath) {
        this.tableName = tableName;
        this.csvPath = csvPath;
    }


    public String getColumn(String datasetId, String columnName){
        try (InputStream inputStream = new FileInputStream(csvPath)) {
            // Get column types
        
            CsvReadOptions csvReadOptions = createCsvReadOptions(inputStream);
            Table table = Table.read().usingOptions(csvReadOptions);
            // Create initial empty selection
            return getJsonDataFromTableSawTable(table.selectColumns(new String[]{columnName}));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<VisualColumn> getColumns(String datasetId){
        try (InputStream inputStream = new FileInputStream(csvPath)) {
            // Get column types
            // Initialize the table
            CsvReadOptions csvReadOptions = createCsvReadOptions(inputStream);
            Table table = Table.read().usingOptions(csvReadOptions);
            // Create initial empty selection
            return table.columns().stream().map(col -> getVisualColumnFromTableSawColumn(col)).toList();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public VisualizationResults executeQuery(VisualQuery query){
        VisualizationResults visualizationResults = new VisualizationResults();
        try (InputStream inputStream = new FileInputStream(csvPath)) {
           
            // Initialize the table
            CsvReadOptions csvReadOptions = createCsvReadOptions(inputStream);
            Table table = Table.read().usingOptions(csvReadOptions);
            
            // Create initial empty selection
            Selection selection = null;

            // Apply filter filters
            for (AbstractFilter filter : query.getFilters()) {
                Selection filterSelection = null;
                if (filter instanceof RangeFilter) {
                    RangeFilter<?> rangeFilter = (RangeFilter<?>) filter;
                    if (rangeFilter instanceof NumberRangeFilter) {
                        LOG.debug("Number range filtering {}, with min {} and max {}", rangeFilter.getColumn(), rangeFilter.getMinValue(), rangeFilter.getMaxValue());
                        NumberRangeFilter numberRangeFilter = (NumberRangeFilter) rangeFilter;
                        // Assuming column is a Number column
                        filterSelection = table.numberColumn(rangeFilter.getColumn())
                                .isGreaterThanOrEqualTo(numberRangeFilter.getMinValue())
                                .and(table.numberColumn(rangeFilter.getColumn())
                                .isLessThanOrEqualTo(numberRangeFilter.getMaxValue()));
                    } else if (rangeFilter instanceof DateTimeRangeFilter) {
                        LOG.debug("Date range filtering {}, with min {} and max {}", rangeFilter.getColumn(), rangeFilter.getMinValue(), rangeFilter.getMaxValue());
                        DateTimeRangeFilter dateTimeRangeFilter = (DateTimeRangeFilter) rangeFilter;
                        // Assuming column is a Date-Time column
                        filterSelection = table.dateTimeColumn(rangeFilter.getColumn())
                                .isBetweenIncluding(dateTimeRangeFilter.getMinValue(), dateTimeRangeFilter.getMaxValue());
                    }
                    // Add other types of RangeFilters here if needed
                }
                // Add other types of filters here 
                selection = selection == null ? filterSelection : selection.and(filterSelection);
            }
            LOG.debug("Selection is: {}", selection);
            // Get the resulting table
            Table resultTable = table;

            // Selection
            if(selection != null)
                resultTable = table.where(selection);

            // Limit
            Integer limit = query.getLimit();
            if(limit != null) resultTable = resultTable.first(limit); 

            // Projection 
            String[] columnNames = query.getColumns().toArray(String[]::new);
            if(columnNames.length != 0)
                resultTable = resultTable.selectColumns(columnNames);

            visualizationResults.setData(getJsonDataFromTableSawTable(resultTable));
            visualizationResults.setMessage("200");
        } catch (IOException e) {
            visualizationResults.setMessage("400");
            e.printStackTrace();
        }
        // LOG.debug("Data:{}", visualizationResults.getData());
        return visualizationResults;
    }

    private CsvReadOptions createCsvReadOptions(InputStream inputStream) {
        return CsvReadOptions
                    .builder(inputStream)
                    .tableName(tableName)
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
