// package gr.imsi.athenarc.xtremexpvisapi.datasource;

// import java.time.LocalDateTime;
// import java.time.temporal.ChronoUnit;
// import java.util.Arrays;
// import java.util.List;

// import java.util.stream.Collectors;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.AbstractFilter;
// import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.EqualsFilter;
// import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.RangeFilter;
// import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.RangeFilter.DateTimeRangeFilter;
// import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.RangeFilter.DoubleRangeFilter;
// import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.RangeFilter.IntegerRangeFilter;
// import gr.imsi.athenarc.xtremexpvisapi.domain.Query.VisualQuery;
// import tech.tablesaw.api.DateTimeColumn;
// import tech.tablesaw.api.DoubleColumn;
// import tech.tablesaw.api.NumberColumn;
// import tech.tablesaw.api.Table;
// import tech.tablesaw.columns.Column;
// import tech.tablesaw.selection.Selection;
// import tech.tablesaw.aggregate.AggregateFunctions;


// public class CsvQueryExecutor {

//     private static final Logger LOG = LoggerFactory.getLogger(CsvQueryExecutor.class);

//     public Table queryTable(Table table, VisualQuery query) {
//         // Table resultTable = null;
 
//         Selection selection = null;
//         if(query.getFilters() != null){
//             for (AbstractFilter filter : query.getFilters()) {
//                 Selection filterSelection = null;
//                 if (filter instanceof RangeFilter) {
//                     RangeFilter<?> rangeFilter = (RangeFilter<?>) filter;
//                     //Handle Double RangeFilter
//                     if (rangeFilter instanceof DoubleRangeFilter) {
//                         LOG.debug("Number range filtering {}, with min {} and max {}", rangeFilter.getColumn(), rangeFilter.getMinValue(), rangeFilter.getMaxValue());
//                         DoubleRangeFilter numberRangeFilter = (DoubleRangeFilter) rangeFilter;
//                         // Assuming column is a Number column
//                         filterSelection = table.numberColumn(rangeFilter.getColumn())
//                                 .isGreaterThanOrEqualTo(numberRangeFilter.getMinValue())
//                                 .and(table.numberColumn(rangeFilter.getColumn())
//                                 .isLessThanOrEqualTo(numberRangeFilter.getMaxValue()));

//                     // Handle Datetime RangeFilter           
//                     } else if (rangeFilter instanceof DateTimeRangeFilter) {
//                         LOG.debug("Date range filtering {}, with min {} and max {}", rangeFilter.getColumn(), rangeFilter.getMinValue(), rangeFilter.getMaxValue());
//                         DateTimeRangeFilter dateTimeRangeFilter = (DateTimeRangeFilter) rangeFilter;
//                         // Assuming column is a Date-Time column
//                         filterSelection = table.dateTimeColumn(rangeFilter.getColumn())
//                                 .isBetweenIncluding(dateTimeRangeFilter.getMinValue(), dateTimeRangeFilter.getMaxValue());
//                     } else if (rangeFilter instanceof IntegerRangeFilter) {
//                         LOG.debug("Integer range filtering {}, with min {} and max {}", rangeFilter.getColumn(), rangeFilter.getMinValue(), rangeFilter.getMaxValue());
//                         IntegerRangeFilter integerRangeFilter = (IntegerRangeFilter) rangeFilter;
//                         // Assuming column is an Integer column
//                         filterSelection = table.intColumn(rangeFilter.getColumn())
//                                 .isGreaterThanOrEqualTo(integerRangeFilter.getMinValue())
//                                 .and(table.intColumn(rangeFilter.getColumn())
//                                 .isLessThanOrEqualTo(integerRangeFilter.getMaxValue()));
//                     }
//                     // Add other types of RangeFilters here if needed
//                 }else if (filter instanceof EqualsFilter) {
//                     EqualsFilter equalsFilter = (EqualsFilter) filter;
//                     Column<?> column = table.column(equalsFilter.getColumn());
//                     String columnTypeName = column.type().name();
//                     switch (columnTypeName) {
//                         case "DOUBLE":
//                             double doubleValue = Double.parseDouble(equalsFilter.getValue().toString());
//                             filterSelection = table.doubleColumn(equalsFilter.getColumn()).isEqualTo(doubleValue);
//                             break;
//                         case "INTEGER":
//                             int intValue = Integer.parseInt(equalsFilter.getValue().toString());
//                             filterSelection = table.intColumn(equalsFilter.getColumn()).isEqualTo(intValue);
//                             break;
//                         case "STRING":
//                             LOG.debug("String equals filtering {}, with value {}", equalsFilter.getColumn(), equalsFilter.getValue());

//                             filterSelection = table.stringColumn(equalsFilter.getColumn()).isEqualTo(equalsFilter.getEqualValue().toString());
//                             break;
//                         case "LOCAL_DATE_TIME":
//                             LocalDateTime localDateTimeValue = LocalDateTime.parse(equalsFilter.getValue().toString());
//                             filterSelection = table.dateTimeColumn(equalsFilter.getColumn()).isEqualTo(localDateTimeValue);
//                             break;
//                         default:
//                             throw new IllegalArgumentException("Unsupported column type for equals filter: " + columnTypeName);
//                     }
//                 }
                
//                 // Add other types of filters here 
//                 selection = selection == null ? filterSelection : selection.and(filterSelection);
//             }
//         }
//         LOG.debug("Selection is: {}", selection);
//         // Get the resulting table
//         // resultTable = table;
//         Table resultTable = (selection != null) ? table.where(selection) : table;



//         // Selection
//         // if(selection != null)
//         //     resultTable = table.where(selection);
        
//         LOG.info("Row count after filtering: " + resultTable.rowCount());

//         Integer offset = query.getOffset();
//         if(offset != null && offset > 0) {
//             LOG.info("Applying offset: " + offset);
//             resultTable = resultTable.dropRange(0, offset);
//         }


//         // Limit
//         Integer limit = query.getLimit();
//         if (limit != null && limit > 0) {
//             LOG.info("Applying limit: " + limit);
//             resultTable = resultTable.first(limit); // Get the first 'limit' rows
//         }
//         LOG.info("Row count after applying limit and offset: " + resultTable.rowCount());

//         // Projection 
//         String[] columnNames = query.getColumns().toArray(String[]::new);
//         if(columnNames.length != 0){
//             resultTable = resultTable.selectColumns(columnNames);
//         }
        
//         //Normalization
//         List<String> columnsToNormalize = Arrays.asList(columnNames);
//         List<String> filteredColumns = columnsToNormalize.stream()
//             .filter(columnName -> !columnName.equalsIgnoreCase("timestamp") && !columnName.equalsIgnoreCase("datetime"))
//             .collect(Collectors.toList());
//         String normalizationType = query.getScaler(); // Assuming this returns a String now
//         if ("z".equalsIgnoreCase(normalizationType)) {
//             applyZScoreNormalization(resultTable, filteredColumns);
//         } else if ("minmax".equalsIgnoreCase(normalizationType)) {
//             applyMinMaxNormalization(resultTable, filteredColumns);
//         } else if ("log".equalsIgnoreCase(normalizationType)) {
//             applyLogTransformation(resultTable, filteredColumns);
//         } else {

//             LOG.info("No normalization applied");
//         }

//         // Change Granularity 
//         String aggfun = query.getAggFunction(); 
//         if(aggfun != null) resultTable = changeGranularity(resultTable,  "timestamp", ChronoUnit.DAYS, aggfun);        
//         return resultTable;

//     }

    
//     private Table changeGranularity(Table table, String dateTimeColumnName, ChronoUnit granularity, String aggregationFunction) {
//         if (!table.columnNames().contains(dateTimeColumnName)) {
//             LOG.error("Datetime column '{}' not found in the table.", dateTimeColumnName);
//             return table;
//         }
        
//         // Get the original datetime column
//         DateTimeColumn dateTime = (DateTimeColumn) table.column(dateTimeColumnName);
//         // Create a new column with rounded datetime values
//         DateTimeColumn modifiedDateTime = DateTimeColumn.create("RoundedDateTime");
//         // Manually rounding the datetime to the specified granularity
//         for (int i = 0; i < dateTime.size(); i++) {
//             switch (granularity) {
//                 case MINUTES:
//                     modifiedDateTime.append(dateTime.get(i).truncatedTo(ChronoUnit.MINUTES));
//                     break;
//                 case HOURS:
//                     modifiedDateTime.append(dateTime.get(i).truncatedTo(ChronoUnit.HOURS));
//                     break;
//                 case DAYS:
//                     modifiedDateTime.append(dateTime.get(i).truncatedTo(ChronoUnit.DAYS));
//                     break;
//                 default:
//                     LOG.error("Unsupported granularity '{}'.", granularity);
//                     return table;
//             }
//         }    
    
//         // Add this new column to the table
        
//         // Now, group by this new column and aggregate other columns
//         // Table newtable;
//         table.addColumns(modifiedDateTime);

//         List<String> numericColumns = table.columnNames().stream()
//             .filter(col -> !col.equals(dateTimeColumnName) && table.column(col) instanceof NumberColumn)
//             .collect(Collectors.toList());

//     // Perform the aggregation based on the aggregation function
//     Table aggregatedTable;
//     switch (aggregationFunction) {
//         case "sum":
//             aggregatedTable = table.summarize(numericColumns, AggregateFunctions.sum).by("RoundedDateTime");
//             break;
//         case "mean":
//             aggregatedTable = table.summarize(numericColumns, AggregateFunctions.mean).by("RoundedDateTime");
//             break;
//         case "min":
//             aggregatedTable = table.summarize(numericColumns, AggregateFunctions.min).by("RoundedDateTime");
//             break;
//         case "max":
//             aggregatedTable = table.summarize(numericColumns, AggregateFunctions.max).by("RoundedDateTime");
//             break;
//         default:
//             LOG.error("Unsupported aggregation function '{}'.", aggregationFunction);
//             return table;
//     }

//     // Rename the columns back to their original names (except the datetime column)
//     for (String numericColumn : numericColumns) {
//         String oldColumnName = "Sum [" + numericColumn + "]"; // The summarize function renames columns like this
//         if (aggregationFunction.equals("mean")) {
//             oldColumnName = "Mean [" + numericColumn + "]";
//         } else if (aggregationFunction.equals("min")) {
//             oldColumnName = "Min [" + numericColumn + "]";
//         } else if (aggregationFunction.equals("max")) {
//             oldColumnName = "Max [" + numericColumn + "]";
//         }
//         // Rename to the original column name
//         if (aggregatedTable.columnNames().contains(oldColumnName)) {
//             aggregatedTable.column(oldColumnName).setName(numericColumn);
//         }
//     }

//     // DateTimeColumn originalDateTime = (DateTimeColumn) table.column(dateTimeColumnName);
//     // aggregatedTable.addColumns(originalDateTime);

//     LOG.info("Data granularity changed and aggregated by '{}'.", aggregatedTable.print());
//     return aggregatedTable;
// }


//     private void applyMinMaxNormalization(Table table, List<String> columnNames) {
//         for (String columnName : columnNames) {
//             if (table.columnNames().contains(columnName)) {
//             // Cast the column to DoubleColumn to work with numerical data
//                 DoubleColumn numericColumn = table.doubleColumn(columnName);
    
//                 Double min = table.doubleColumn(columnName).min();
//                 Double max = table.doubleColumn(columnName).max();
    
//                 if (min != max) {
//                     DoubleColumn normalized = numericColumn.map(value -> (value - min) / (max - min));
//                 // Replace the original column with the normalized one
//                     table.replaceColumn(columnName, normalized.setName(columnName + "_normalized"));
//                     LOG.info("Normalization applied to column '{}'.", columnName);
//                 } else {
//                     LOG.warn("Cannot normalize column '{}' because all values are the same.", columnName);
//                 }
//             } else {
//                 LOG.warn("Column '{}' not found in the table.", columnName);
//             }
//         }
//     }


//     private void applyZScoreNormalization(Table table, List<String> columnNames) {
//         for (String columnName : columnNames) {
//             if (table.columnNames().contains(columnName)) {
//                 DoubleColumn numericColumn = table.doubleColumn(columnName);
    
//                 double mean = numericColumn.mean();
//                 double standardDeviation = numericColumn.standardDeviation();
    
//                 if (standardDeviation > 0) {
//                     DoubleColumn standardized = numericColumn.map(value -> (value - mean) / standardDeviation);
//                     table.replaceColumn(columnName, standardized.setName(columnName + "_standardized"));
//                     LOG.info("Standardization applied to column '{}'.", columnName);
//                 } else {
//                     LOG.warn("Cannot standardize column '{}' because standard deviation is zero.", columnName);
//                 }
//             } else {
//                 LOG.warn("Column '{}' not found in the table.", columnName);
//             }
//         }
//     }

//     private void applyLogTransformation(Table table, List<String> columnNames) {
//         for (String columnName : columnNames) {
//             if (table.columnNames().contains(columnName)) {
//                 DoubleColumn numericColumn = table.doubleColumn(columnName);
    
//                 // Apply log transformation, adding 1 to avoid log(0)
//                 DoubleColumn logTransformed = numericColumn.map(value -> Math.log(value + 1));
//                 table.replaceColumn(columnName, logTransformed.setName(columnName + "_log_transformed"));
//                 LOG.info("Log transformation applied to column '{}'.", columnName);
//             } else {
//                 LOG.warn("Column '{}' not found in the table.", columnName);
//             }
//         }
//     }

// }







package gr.imsi.athenarc.xtremexpvisapi.datasource;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.AbstractFilter;
import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.EqualsFilter;
import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.RangeFilter;
import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.RangeFilter.DateTimeRangeFilter;
import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.RangeFilter.DoubleRangeFilter;
import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.RangeFilter.IntegerRangeFilter;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.VisualQuery;
import tech.tablesaw.api.DateTimeColumn;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.NumberColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;
import tech.tablesaw.selection.Selection;
import tech.tablesaw.aggregate.AggregateFunctions;


public class CsvQueryExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(CsvQueryExecutor.class);

    public Table queryTable(Table table, VisualQuery query) {
        // Table resultTable = null;
 
        Selection selection = null;
        if(query.getFilters() != null){
            for (AbstractFilter filter : query.getFilters()) {
                Selection filterSelection = null;
                if (filter instanceof RangeFilter) {
                    RangeFilter<?> rangeFilter = (RangeFilter<?>) filter;
                    //Handle Double RangeFilter
                    if (rangeFilter instanceof DoubleRangeFilter) {
                        LOG.debug("Number range filtering {}, with min {} and max {}", rangeFilter.getColumn(), rangeFilter.getMinValue(), rangeFilter.getMaxValue());
                        DoubleRangeFilter numberRangeFilter = (DoubleRangeFilter) rangeFilter;
                        // Assuming column is a Number column
                        filterSelection = table.numberColumn(rangeFilter.getColumn())
                                .isGreaterThanOrEqualTo(numberRangeFilter.getMinValue())
                                .and(table.numberColumn(rangeFilter.getColumn())
                                .isLessThanOrEqualTo(numberRangeFilter.getMaxValue()));

                    // Handle Datetime RangeFilter           
                    } else if (rangeFilter instanceof DateTimeRangeFilter) {
                        LOG.debug("Date range filtering {}, with min {} and max {}", rangeFilter.getColumn(), rangeFilter.getMinValue(), rangeFilter.getMaxValue());
                        DateTimeRangeFilter dateTimeRangeFilter = (DateTimeRangeFilter) rangeFilter;
                        // Assuming column is a Date-Time column
                        filterSelection = table.dateTimeColumn(rangeFilter.getColumn())
                                .isBetweenIncluding(dateTimeRangeFilter.getMinValue(), dateTimeRangeFilter.getMaxValue());
                    } else if (rangeFilter instanceof IntegerRangeFilter) {
                        LOG.debug("Integer range filtering {}, with min {} and max {}", rangeFilter.getColumn(), rangeFilter.getMinValue(), rangeFilter.getMaxValue());
                        IntegerRangeFilter integerRangeFilter = (IntegerRangeFilter) rangeFilter;
                        // Assuming column is an Integer column
                        filterSelection = table.intColumn(rangeFilter.getColumn())
                                .isGreaterThanOrEqualTo(integerRangeFilter.getMinValue())
                                .and(table.intColumn(rangeFilter.getColumn())
                                .isLessThanOrEqualTo(integerRangeFilter.getMaxValue()));
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
                            LOG.debug("String equals filtering {}, with value {}", equalsFilter.getColumn(), equalsFilter.getValue());

                            filterSelection = table.stringColumn(equalsFilter.getColumn()).isEqualTo(equalsFilter.getEqualValue().toString());
                            break;
                        case "LOCAL_DATE_TIME":
                            LocalDateTime localDateTimeValue = LocalDateTime.parse(equalsFilter.getValue().toString());
                            filterSelection = table.dateTimeColumn(equalsFilter.getColumn()).isEqualTo(localDateTimeValue);
                            break;
                        default:
                            throw new IllegalArgumentException("Unsupported column type for equals filter: " + columnTypeName);
                    }
                }
                
                // Add other types of filters here 
                selection = selection == null ? filterSelection : selection.and(filterSelection);
            }
        }
        LOG.debug("Selection is: {}", selection);
        // Get the resulting table
        // resultTable = table;
        Table resultTable = (selection != null) ? table.where(selection) : table;



        // Selection
        // if(selection != null)
        //     resultTable = table.where(selection);
        

        Integer offset = query.getOffset();
        if(offset != null && offset > 0) {
            resultTable = resultTable.dropRange(0, offset);
        }


        // Limit
        Integer limit = query.getLimit();
        if (limit != null && limit > 0) {
            resultTable = resultTable.first(limit); // Get the first 'limit' rows
        }
        LOG.info("Row count after applying limit and offset: " + resultTable.rowCount());

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

        // Change Granularity 
        String aggfun = query.getAggFunction(); 
        
        if(aggfun != ""){
            LOG.info("mpaine",aggfun);
            resultTable = aggregateData(resultTable,  query.getTemporalGroupColumn(),query.getTemporalGranularity(), aggfun); 

        }
        else{
            LOG.info("No aggregation applied");
        }
        return resultTable;

    }


    private Table aggregateData(Table table, String groupByColumn, ChronoUnit granularity,String aggregationFunction) {
        // Check if the groupByColumn exists in the table
        if (!table.columnNames().contains(groupByColumn)) {
            LOG.error("Group by column '{}' not found in the table.", groupByColumn);
            return table;
        }
    
        // Check if the groupBy column is temporal, numeric, or categorical
        Column<?> groupColumn = table.column(groupByColumn);
    
        // If the column is temporal (date or timestamp), apply date granularity
        if (groupColumn instanceof DateTimeColumn) {
            LOG.info("Applying date granularity to column '{}'", groupByColumn);
            // Handle the temporal case (the existing logic)
            return changeGranularity(table, groupByColumn, granularity, aggregationFunction);  // You can change ChronoUnit based on your need
        }
    
        // Otherwise, it's either numeric or categorical, so we can group by directly
        List<String> numericColumns = table.columnNames().stream()
            .filter(col -> !col.equals(groupByColumn) && table.column(col) instanceof NumberColumn)
            .collect(Collectors.toList());
    
        // Perform the aggregation based on the aggregation function
        Table aggregatedTable;
        switch (aggregationFunction) {
            case "sum":
                aggregatedTable = table.summarize(numericColumns, AggregateFunctions.sum).by(groupByColumn);
                break;
            case "mean":
                aggregatedTable = table.summarize(numericColumns, AggregateFunctions.mean).by(groupByColumn);
                break;
            case "min":
                aggregatedTable = table.summarize(numericColumns, AggregateFunctions.min).by(groupByColumn);
                break;
            case "max":
                aggregatedTable = table.summarize(numericColumns, AggregateFunctions.max).by(groupByColumn);
                break;
            default:
                LOG.error("Unsupported aggregation function '{}'.", aggregationFunction);
                return table;
        }
    
        // Rename the columns back to their original names (same logic as before)
        for (String numericColumn : numericColumns) {
            String oldColumnName = aggregationFunction.substring(0, 1).toUpperCase() + aggregationFunction.substring(1) + " [" + numericColumn + "]";
            if (aggregatedTable.columnNames().contains(oldColumnName)) {
                aggregatedTable.column(oldColumnName).setName(numericColumn);
            }
        }
    
        LOG.info("Data aggregated by '{}'", groupByColumn);
        return aggregatedTable;
    }
    

    
    private Table changeGranularity(Table table, String dateTimeColumnName, ChronoUnit granularity, String aggregationFunction) {
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
        // Table newtable;
        table.addColumns(modifiedDateTime);

        List<String> numericColumns = table.columnNames().stream()
            .filter(col -> !col.equals(dateTimeColumnName) && table.column(col) instanceof NumberColumn)
            .collect(Collectors.toList());

    // Perform the aggregation based on the aggregation function
    Table aggregatedTable;
    switch (aggregationFunction) {
        case "sum":
            aggregatedTable = table.summarize(numericColumns, AggregateFunctions.sum).by("RoundedDateTime");
            break;
        case "mean":
            aggregatedTable = table.summarize(numericColumns, AggregateFunctions.mean).by("RoundedDateTime");
            break;
        case "min":
            aggregatedTable = table.summarize(numericColumns, AggregateFunctions.min).by("RoundedDateTime");
            break;
        case "max":
            aggregatedTable = table.summarize(numericColumns, AggregateFunctions.max).by("RoundedDateTime");
            break;
        default:
            LOG.error("Unsupported aggregation function '{}'.", aggregationFunction);
            return table;
    }

    // Rename the columns back to their original names (except the datetime column)
    for (String numericColumn : numericColumns) {
        String oldColumnName = "Sum [" + numericColumn + "]"; // The summarize function renames columns like this
        if (aggregationFunction.equals("mean")) {
            oldColumnName = "Mean [" + numericColumn + "]";
        } else if (aggregationFunction.equals("min")) {
            oldColumnName = "Min [" + numericColumn + "]";
        } else if (aggregationFunction.equals("max")) {
            oldColumnName = "Max [" + numericColumn + "]";
        }
        // Rename to the original column name
        if (aggregatedTable.columnNames().contains(oldColumnName)) {
            aggregatedTable.column(oldColumnName).setName(numericColumn);
        }
    }

    // DateTimeColumn originalDateTime = (DateTimeColumn) table.column(dateTimeColumnName);
    // aggregatedTable.addColumns(originalDateTime);

    return aggregatedTable;
}


    private void applyMinMaxNormalization(Table table, List<String> columnNames) {
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


    private void applyZScoreNormalization(Table table, List<String> columnNames) {
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

    private void applyLogTransformation(Table table, List<String> columnNames) {
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

}
