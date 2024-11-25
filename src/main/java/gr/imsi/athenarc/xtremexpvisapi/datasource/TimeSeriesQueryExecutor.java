package gr.imsi.athenarc.xtremexpvisapi.datasource;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.AbstractFilter;
import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.EqualsFilter;
import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.RangeFilter;
import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.RangeFilter.DateTimeRangeFilter;
import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.RangeFilter.DoubleRangeFilter;
import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.RangeFilter.IntegerRangeFilter;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TimeSeriesQuery;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;
import tech.tablesaw.selection.Selection;

public class TimeSeriesQueryExecutor {
     private static final Logger LOG = LoggerFactory.getLogger(TimeSeriesQueryExecutor.class);

    public Table queryTabularData(Table table, TimeSeriesQuery query) {
        // Table resultTable = null;
 
        Selection selection = null;
        if(query.getFrom() != null && query.getTo()!=null){
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
     
        Table resultTable = (selection != null) ? table.where(selection) : table;
        resultTable = applyPagination(resultTable, query.getLimit(), query.getOffset());
        resultTable = applyColumnSelection(resultTable, query.getColumns());
       

        LOG.info("Final table after query has {} rows.", resultTable.rowCount());

      
    
        return resultTable;

    }




    // Apply pagination
    private Table applyPagination(Table table, Integer limit, Integer offset) {
        if (offset != null && offset > 0) {
            table = table.dropRange(0, offset);
        }

        if (limit != null && limit > 0) {
            table = table.first(limit);
        }

        return table;
    }

    // Apply column selection
    private Table applyColumnSelection(Table table, List<String> columns) {
        if (columns != null && !columns.isEmpty()) {
            table = table.selectColumns(columns.toArray(new String[0]));
        }
        return table;
    }

  

    
}
