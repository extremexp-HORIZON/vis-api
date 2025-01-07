
package gr.imsi.athenarc.xtremexpvisapi.datasource;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gr.imsi.athenarc.xtremexpvisapi.domain.Query.QueryResult;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TabularRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.QueryParams.Filter.AbstractFilter;
import gr.imsi.athenarc.xtremexpvisapi.domain.QueryParams.Filter.EqualsFilter;
import gr.imsi.athenarc.xtremexpvisapi.domain.QueryParams.Filter.RangeFilter;
import gr.imsi.athenarc.xtremexpvisapi.domain.QueryParams.Filter.RangeFilter.DateTimeRangeFilter;
import gr.imsi.athenarc.xtremexpvisapi.domain.QueryParams.Filter.RangeFilter.DoubleRangeFilter;
import gr.imsi.athenarc.xtremexpvisapi.domain.QueryParams.Filter.RangeFilter.IntegerRangeFilter;
import tech.tablesaw.aggregate.AggregateFunctions;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;
import tech.tablesaw.selection.Selection;

public class TabularQueryExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(TabularQueryExecutor.class);

    public QueryResult queryTabularData(Table table, TabularRequest tabularRequest) {
        // Table resultTable = null;

        Selection selection = null;
        if (tabularRequest.getFilters() != null) {
            for (AbstractFilter filter : tabularRequest.getFilters()) {
                Selection filterSelection = null;
                if (filter instanceof RangeFilter) {
                    RangeFilter<?> rangeFilter = (RangeFilter<?>) filter;
                    // Handle Double RangeFilter
                    if (rangeFilter instanceof DoubleRangeFilter) {
                        LOG.debug("Number range filtering {}, with min {} and max {}", rangeFilter.getColumn(),
                                rangeFilter.getMinValue(), rangeFilter.getMaxValue());
                        // Assuming column is a Number column
                        filterSelection = table.numberColumn(rangeFilter.getColumn())
                                .isGreaterThanOrEqualTo((Double) rangeFilter.getMinValue())
                                .and(table.numberColumn(rangeFilter.getColumn())
                                        .isLessThanOrEqualTo((Double) rangeFilter.getMaxValue()));
                        // Handle Datetime RangeFilter
                    } else if (rangeFilter instanceof DateTimeRangeFilter) {
                        LOG.debug("Date range filtering {}, with min {} and max {}", rangeFilter.getColumn(),
                                rangeFilter.getMinValue(), rangeFilter.getMaxValue());
                        // Assuming column is a Date-Time column
                        filterSelection = table.dateTimeColumn(rangeFilter.getColumn())
                                .isBetweenIncluding((LocalDateTime) rangeFilter.getMinValue(),
                                        (LocalDateTime) rangeFilter.getMaxValue());
                    } else if (rangeFilter instanceof IntegerRangeFilter) {
                        LOG.debug("Integer range filtering {}, with min {} and max {}", rangeFilter.getColumn(),
                                rangeFilter.getMinValue(), rangeFilter.getMaxValue());
                        // Assuming column is an Integer column
                        filterSelection = table.intColumn(rangeFilter.getColumn())
                                .isGreaterThanOrEqualTo((Integer) rangeFilter.getMinValue())
                                .and(table.intColumn(rangeFilter.getColumn())
                                        .isLessThanOrEqualTo((Integer) rangeFilter.getMaxValue()));
                    }
                    // Add other types of RangeFilters here if needed
                } else if (filter instanceof EqualsFilter) {
                    EqualsFilter<?> equalsFilter = (EqualsFilter<?>) filter;
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
                            LOG.debug("String equals filtering {}, with value {}", equalsFilter.getColumn(),
                                    equalsFilter.getValue());

                            filterSelection = table.stringColumn(equalsFilter.getColumn())
                                    .isEqualTo(equalsFilter.getEqualValue().toString());
                            break;
                        case "LOCAL_DATE_TIME":
                            LocalDateTime localDateTimeValue = LocalDateTime.parse(equalsFilter.getValue().toString());
                            filterSelection = table.dateTimeColumn(equalsFilter.getColumn())
                                    .isEqualTo(localDateTimeValue);
                            break;
                        default:
                            throw new IllegalArgumentException(
                                    "Unsupported column type for equals filter: " + columnTypeName);
                    }
                }

                // Add other types of filters here
                selection = selection == null ? filterSelection : selection.and(filterSelection);
            }
        }
        LOG.debug("Selection is: {}", selection);

        Table resultTable = (selection != null) ? table.where(selection) : table;
        int rowCount = resultTable.rowCount();
        LOG.info("Row count after filtering: {}", rowCount);

        resultTable = applyColumnSelection(resultTable, tabularRequest.getColumns());
        if (tabularRequest.getAggregation() != null && !tabularRequest.getAggregation().isEmpty()) {
            resultTable = applyAggregation(resultTable, tabularRequest.getGroupBy(), tabularRequest.getAggregation());
        }

        resultTable = applyPagination(resultTable, tabularRequest.getLimit(), tabularRequest.getOffset());

        LOG.info("Final table after query has {} rows.", resultTable.rowCount());

        return new QueryResult(resultTable, rowCount);

    }

    // Apply pagination
    private Table applyPagination(Table table, Integer limit, Integer offset) {
        // Set default values for limit and offset

        if (offset == null || offset < 0) {
            offset = 0; // Reset negative offsets to 0
        }

        // Log pagination values for debugging
        LOG.debug("Applying pagination with offset: {} and limit: {}", offset, limit);

        // If offset is greater than 0, drop rows to get to the desired starting point
        if (offset > 0) {
            table = table.dropRange(0, offset);
        }

        // Get the first 'limit' rows after applying the offset
        if (limit > 0) {
            table = table.first(limit);
        }

        LOG.debug("Table after pagination has {} rows.", table.rowCount());
        return table;
    }

    // Apply column selection
    private Table applyColumnSelection(Table table, List<String> columns) {
        if (columns != null && !columns.isEmpty()) {
            table = table.selectColumns(columns.toArray(new String[0]));
        }
        return table;
    }

    private Table applyAggregation(Table table, List<String> groupByColumns, Map<String, Object> aggregation) {
        Table resultTable = null;

        // Iterate over the aggregation map
        for (Map.Entry<String, Object> agg : aggregation.entrySet()) {
            String column = agg.getKey();
            Object aggFunctions = agg.getValue();

            // Check if the aggregation functions are in an array (multiple functions for
            // the same column)
            if (aggFunctions instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> aggList = (List<String>) aggFunctions;
                for (String aggFunction : aggList) {
                    resultTable = applySingleAggregation(table, groupByColumns, resultTable, column, aggFunction);
                }
            } else if (aggFunctions instanceof String) {
                // Single aggregation function for the column
                String aggFunction = (String) aggFunctions;
                resultTable = applySingleAggregation(table, groupByColumns, resultTable, column, aggFunction);
            } else {
                LOG.error("Unsupported aggregation value type for column '{}'", column);
            }
        }
        resultTable = renameAggregatedColumns(resultTable);
        return resultTable;
    }

    private Table renameAggregatedColumns(Table resultTable) {
        // Create a new table to hold the renamed columns
        Table renamedTable = Table.create(resultTable.name());

        for (Column<?> col : resultTable.columns()) {
            String originalName = col.name();
            // Replace spaces and brackets with underscores (e.g., "Mean [state]" ->
            // "Mean_state")
            String newName = originalName.replace(" [", "_").replace("]", "");

            // Add renamed column to the new table
            renamedTable.addColumns(col.copy().setName(newName));
        }

        return renamedTable;
    }

    private Table applySingleAggregation(Table table, List<String> groupByColumns, Table resultTable, String column,
            String aggFunction) {
        Table aggregatedTable;

        // Check if groupByColumns is null or empty, indicating global aggregation
        boolean globalAggregation = (groupByColumns == null || groupByColumns.isEmpty());

        switch (aggFunction.toLowerCase()) {
            case "sum":
                if (globalAggregation) {
                    aggregatedTable = table.summarize(column, AggregateFunctions.sum).apply(); // Global aggregation
                } else {
                    aggregatedTable = table.summarize(column, AggregateFunctions.sum)
                            .by(groupByColumns.toArray(new String[0]));
                }
                break;
            case "avg":
                if (globalAggregation) {
                    aggregatedTable = table.summarize(column, AggregateFunctions.mean).apply(); // Global aggregation
                } else {
                    aggregatedTable = table.summarize(column, AggregateFunctions.mean)
                            .by(groupByColumns.toArray(new String[0]));
                }
                break;
            case "count":
                if (globalAggregation) {
                    aggregatedTable = table.summarize(column, AggregateFunctions.count).apply(); // Global aggregation
                } else {
                    aggregatedTable = table.summarize(column, AggregateFunctions.count)
                            .by(groupByColumns.toArray(new String[0]));
                }
                break;
            case "max":
                if (globalAggregation) {
                    aggregatedTable = table.summarize(column, AggregateFunctions.max).apply(); // Global aggregation
                } else {
                    aggregatedTable = table.summarize(column, AggregateFunctions.max)
                            .by(groupByColumns.toArray(new String[0]));
                }
                break;
            case "min":
                if (globalAggregation) {
                    aggregatedTable = table.summarize(column, AggregateFunctions.min).apply(); // Global aggregation
                } else {
                    aggregatedTable = table.summarize(column, AggregateFunctions.min)
                            .by(groupByColumns.toArray(new String[0]));
                }
                break;
            default:
                LOG.error("Unsupported aggregation function '{}'", aggFunction);
                return resultTable; // Return the resultTable unchanged if the function is unsupported
        }

        // If it's the first aggregation, set it as the resultTable, else join the
        // tables
        if (resultTable == null) {
            return aggregatedTable;
        } else {
            return resultTable.joinOn(groupByColumns.toArray(new String[0])).inner(aggregatedTable);
        }
    }

}
