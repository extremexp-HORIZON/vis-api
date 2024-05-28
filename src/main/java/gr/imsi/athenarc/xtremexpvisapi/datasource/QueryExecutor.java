package gr.imsi.athenarc.xtremexpvisapi.datasource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
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
import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.EqualsFilter;
import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.RangeFilter;
import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.RangeFilter.DateTimeRangeFilter;
import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.RangeFilter.NumberRangeFilter;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.VisualQuery;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.DateTimeColumn;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.NumberColumn;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;
import tech.tablesaw.io.csv.CsvReadOptions;
import tech.tablesaw.selection.Selection;
import tech.tablesaw.aggregate.AggregateFunctions;


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
                }else if (filter instanceof EqualsFilter) {
                    EqualsFilter equalsFilter = (EqualsFilter) filter;
                    Column<?> column = table.column(equalsFilter.getColumn());
                    String columnTypeName = column.type().name();
                    switch (columnTypeName) {
                        case "DOUBLE":
                            double doubleValue = Double.parseDouble(equalsFilter.getValue().toString());
                            filterSelection = table.doubleColumn(equalsFilter.getColumn()).isEqualTo(doubleValue);
                            break;
                        case "INTEGER":
                            int intValue = Integer.parseInt(equalsFilter.getValue().toString());
                            filterSelection = table.intColumn(equalsFilter.getColumn()).isEqualTo(intValue);
                            break;
                        case "STRING":
                            filterSelection = table.stringColumn(equalsFilter.getColumn()).isEqualTo(equalsFilter.getValue().toString());
                            break;
                        default:
                            throw new IllegalArgumentException("Unsupported column type for equals filter: " + columnTypeName);
                    }
                }
                // Add other types of filters here 
                selection = selection == null ? filterSelection : selection.and(filterSelection);
            }
            LOG.debug("Selection is: {}", selection);
            // Get the resulting table
            Table resultTable = table;

            LOG.info("row"+resultTable.rowCount());


            // Selection
            if(selection != null)
                resultTable = table.where(selection);
            
            LOG.info("rowse"+resultTable.rowCount());



            // Limit
            Integer limit = query.getLimit();
            if(limit != null)
                resultTable = resultTable.first(limit); 


            LOG.info("rowint"+resultTable.rowCount());

            

            // Projection 
            String[] columnNames = query.getColumns().toArray(String[]::new);
            if(columnNames.length != 0){
                resultTable = resultTable.selectColumns(columnNames);
            }
            
            //Normalization
            List<String> columnsToNormalize = Arrays.asList(columnNames);
            List<String> filteredColumns = columnsToNormalize.stream()
                .filter(columnName -> !columnName.equalsIgnoreCase("timestamp") && !columnName.equalsIgnoreCase("datetime"))
                .collect(Collectors.toList());
            String normalizationType = query.getScaler(); // Assuming this returns a String now
            if ("z".equalsIgnoreCase(normalizationType)) {
                applyZScoreNormalization(resultTable, filteredColumns);
            } else if ("minmax".equalsIgnoreCase(normalizationType)) {
                applyMinMaxNormalization(resultTable, filteredColumns);
            } else if ("log".equalsIgnoreCase(normalizationType)) {
                applyLogTransformation(resultTable, filteredColumns);
            } else {

                LOG.info("No normalization applied");
            }

            //Change Granularity 
            String aggfun = query.getAggFunction(); 
              resultTable=changeGranularity(resultTable,  "timestamp", ChronoUnit.DAYS, aggfun);

            
            visualizationResults.setData(getJsonDataFromTableSawTable(resultTable));
            visualizationResults.setMessage("200");
        } catch (IOException e) {
            visualizationResults.setMessage("400");
            e.printStackTrace();
        }
        // LOG.debug("Data:{}", visualizationResults.getData());
        return visualizationResults;


        
    }

    public Table changeGranularity(Table table, String dateTimeColumnName, ChronoUnit granularity, String aggregationFunction) {
        if (!table.columnNames().contains(dateTimeColumnName)) {
            LOG.error("Datetime column '{}' not found in the table.", dateTimeColumnName);
            return table;
        }
    
        // Get the original datetime column
        DateTimeColumn dateTime = (DateTimeColumn) table.column(dateTimeColumnName);
        // Create a new column with rounded datetime values
        DateTimeColumn modifiedDateTime = DateTimeColumn.create("RoundedDateTime");
        // Manually rounding the datetime to the specified granularity
        for (int i = 0; i < dateTime.size(); i++) {
            switch (granularity) {
                case MINUTES:
                    modifiedDateTime.append(dateTime.get(i).truncatedTo(ChronoUnit.MINUTES));
                    break;
                case HOURS:
                    modifiedDateTime.append(dateTime.get(i).truncatedTo(ChronoUnit.HOURS));
                    break;
                case DAYS:
                    modifiedDateTime.append(dateTime.get(i).truncatedTo(ChronoUnit.DAYS));
                    break;
                default:
                    LOG.error("Unsupported granularity '{}'.", granularity);
                    return table;
            }
        }    
    
        // Add this new column to the table
        
        // Now, group by this new column and aggregate other columns
        Table newtable;
        if (aggregationFunction.equals("sum")) {
            table.addColumns(modifiedDateTime);

            newtable = table.summarize(table.columnNames().stream()
                .filter(col -> !col.equals(dateTimeColumnName) && table.column(col) instanceof NumberColumn)
                .collect(Collectors.toList()), AggregateFunctions.sum).by("RoundedDateTime");
        } else if (aggregationFunction.equals("mean")) {
            table.addColumns(modifiedDateTime);

            newtable = table.summarize(table.columnNames().stream()
                .filter(col -> !col.equals(dateTimeColumnName) && table.column(col) instanceof NumberColumn)
                .collect(Collectors.toList()), AggregateFunctions.mean).by("RoundedDateTime");
        }else if (aggregationFunction.equals("min")) {
            table.addColumns(modifiedDateTime);

            newtable = table.summarize(table.columnNames().stream()
                .filter(col -> !col.equals(dateTimeColumnName) && table.column(col) instanceof NumberColumn)
                .collect(Collectors.toList()), AggregateFunctions.min).by("RoundedDateTime");
        }else if (aggregationFunction.equals("max")) {
            table.addColumns(modifiedDateTime);

            newtable = table.summarize(table.columnNames().stream()
                .filter(col -> !col.equals(dateTimeColumnName) && table.column(col) instanceof NumberColumn)
                .collect(Collectors.toList()), AggregateFunctions.max).by("RoundedDateTime");
        }
         else {
            LOG.error("Unsupported aggregation function '{}'.", aggregationFunction);
            return table;
        }
        
        LOG.info("Data granularity changed and aggregated by '{}'.", newtable.print());
        return newtable;
    }


    public void applyMinMaxNormalization(Table table, List<String> columnNames) {
        for (String columnName : columnNames) {
            if (table.columnNames().contains(columnName)) {
            // Cast the column to DoubleColumn to work with numerical data
                DoubleColumn numericColumn = table.doubleColumn(columnName);
    
                Double min = table.doubleColumn(columnName).min();
                Double max = table.doubleColumn(columnName).max();
    
                if (min != max) {
                    DoubleColumn normalized = numericColumn.map(value -> (value - min) / (max - min));
                // Replace the original column with the normalized one
                    table.replaceColumn(columnName, normalized.setName(columnName + "_normalized"));
                    LOG.info("Normalization applied to column '{}'.", columnName);
                } else {
                    LOG.warn("Cannot normalize column '{}' because all values are the same.", columnName);
                }
            } else {
                LOG.warn("Column '{}' not found in the table.", columnName);
            }
        }
    }


    public void applyZScoreNormalization(Table table, List<String> columnNames) {
        for (String columnName : columnNames) {
            if (table.columnNames().contains(columnName)) {
                DoubleColumn numericColumn = table.doubleColumn(columnName);
    
                double mean = numericColumn.mean();
                double standardDeviation = numericColumn.standardDeviation();
    
                if (standardDeviation > 0) {
                    DoubleColumn standardized = numericColumn.map(value -> (value - mean) / standardDeviation);
                    table.replaceColumn(columnName, standardized.setName(columnName + "_standardized"));
                    LOG.info("Standardization applied to column '{}'.", columnName);
                } else {
                    LOG.warn("Cannot standardize column '{}' because standard deviation is zero.", columnName);
                }
            } else {
                LOG.warn("Column '{}' not found in the table.", columnName);
            }
        }
    }

    public void applyLogTransformation(Table table, List<String> columnNames) {
        for (String columnName : columnNames) {
            if (table.columnNames().contains(columnName)) {
                DoubleColumn numericColumn = table.doubleColumn(columnName);
    
                // Apply log transformation, adding 1 to avoid log(0)
                DoubleColumn logTransformed = numericColumn.map(value -> Math.log(value + 1));
                table.replaceColumn(columnName, logTransformed.setName(columnName + "_log_transformed"));
                LOG.info("Log transformation applied to column '{}'.", columnName);
            } else {
                LOG.warn("Column '{}' not found in the table.", columnName);
            }
        }
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
