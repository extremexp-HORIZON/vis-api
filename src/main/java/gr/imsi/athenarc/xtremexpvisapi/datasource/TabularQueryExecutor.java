package gr.imsi.athenarc.xtremexpvisapi.datasource;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gr.imsi.athenarc.xtremexpvisapi.domain.Query.QueryResult;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TabularRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.QueryParams.Filter.AbstractFilter;
import gr.imsi.athenarc.xtremexpvisapi.domain.QueryParams.Filter.EqualsFilter;
import gr.imsi.athenarc.xtremexpvisapi.domain.QueryParams.Filter.InequalityFilter;
import gr.imsi.athenarc.xtremexpvisapi.domain.QueryParams.Filter.RangeFilter;
import gr.imsi.athenarc.xtremexpvisapi.domain.QueryParams.Filter.StringFilter;
import tech.tablesaw.aggregate.AggregateFunctions;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;
import tech.tablesaw.selection.Selection;

public class TabularQueryExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(TabularQueryExecutor.class);

    public QueryResult queryTabularData(Table table, TabularRequest tabularRequest) {
        Selection selection = null;
        if (tabularRequest.getFilters() != null) {
            for (AbstractFilter filter : tabularRequest.getFilters()) {
                Selection filterSelection = null;
                if (filter instanceof RangeFilter) {
                    LOG.debug("RangeFilter detected: {}", filter);
                    RangeFilter<?> rangeFilter = (RangeFilter<?>) filter;
                    Column<?> column = table.column(rangeFilter.getColumn());
                    String columnTypeName = column.type().name();
                    switch (columnTypeName) {
                        case "DOUBLE":
                            if (!(rangeFilter.getMin() instanceof Double)
                                    || !(rangeFilter.getMax() instanceof Double)) {
                                throw new IllegalArgumentException(
                                        "Min and Max must be of type Double for column: " + rangeFilter.getColumn());
                            }
                            LOG.debug("Number range filtering {}, with min {} and max {}", rangeFilter.getColumn(),
                                    rangeFilter.getMin(), rangeFilter.getMax());
                            filterSelection = table.numberColumn(rangeFilter.getColumn())
                                    .isGreaterThanOrEqualTo((Double) rangeFilter.getMin())
                                    .and(table.numberColumn(rangeFilter.getColumn())
                                            .isLessThanOrEqualTo((Double) rangeFilter.getMax()));
                            break;
                        case "INTEGER":
                            if (!(rangeFilter.getMin() instanceof Integer)
                                    || !(rangeFilter.getMax() instanceof Integer)) {
                                throw new IllegalArgumentException(
                                        "Min and Max must be of type Integer for column: " + rangeFilter.getColumn());
                            }
                            LOG.debug("Integer range filtering {}, with min {} and max {}", rangeFilter.getColumn(),
                                    rangeFilter.getMin(), rangeFilter.getMax());
                            filterSelection = table.intColumn(rangeFilter.getColumn())
                                    .isGreaterThanOrEqualTo((Integer) rangeFilter.getMin())
                                    .and(table.intColumn(rangeFilter.getColumn())
                                            .isLessThanOrEqualTo((Integer) rangeFilter.getMax()));
                            break;
                        case "LOCAL_DATE_TIME":
                            LocalDateTime minDateTime;
                            LocalDateTime maxDateTime;
                            try {
                                minDateTime = LocalDateTime.parse(rangeFilter.getMin().toString());
                                maxDateTime = LocalDateTime.parse(rangeFilter.getMax().toString());
                            } catch (Exception e) {
                                throw new IllegalArgumentException(
                                        "Min and Max must be of type LocalDateTime for column: "
                                                + rangeFilter.getColumn());
                            }
                            LOG.debug("Date range filtering {}, with min {} and max {}", rangeFilter.getColumn(),
                                    rangeFilter.getMin(), rangeFilter.getMax());
                            filterSelection = table.dateTimeColumn(rangeFilter.getColumn())
                                    .isBetweenIncluding(minDateTime, maxDateTime);
                            break;
                        case "LOCAL_DATE":
                            LocalDate minDate;
                            LocalDate maxDate;
                            try {
                                minDate = LocalDate.parse(rangeFilter.getMin().toString());
                                maxDate = LocalDate.parse(rangeFilter.getMax().toString());
                            } catch (Exception e) {
                                throw new IllegalArgumentException(
                                        "Min and Max must be of type LocalDate for column: " + rangeFilter.getColumn());
                            }
                            LOG.debug("Date range filtering {}, with min {} and max {}", rangeFilter.getColumn(),
                                    rangeFilter.getMin(), rangeFilter.getMax());
                            filterSelection = table.dateColumn(rangeFilter.getColumn())
                                    .isBetweenIncluding(minDate, maxDate);
                            break;
                        case "LOCAL_TIME":
                            LocalTime minTime;
                            LocalTime maxTime;
                            try {
                                minTime = LocalTime.parse(rangeFilter.getMin().toString());
                                maxTime = LocalTime.parse(rangeFilter.getMax().toString());
                            } catch (Exception e) {
                                throw new IllegalArgumentException(
                                        "Min and Max must be of type LocalTime for column: " + rangeFilter.getColumn());
                            }
                            LOG.debug("Date range filtering {}, with min {} and max {}", rangeFilter.getColumn(),
                                    rangeFilter.getMin(), rangeFilter.getMax());
                            filterSelection = table.timeColumn(rangeFilter.getColumn())
                                    .isOnOrAfter(minTime)
                                    .and(table.timeColumn(rangeFilter.getColumn())
                                            .isOnOrBefore(maxTime));
                            break;
                        case "INSTANT":
                            Instant minInstant;
                            Instant maxInstant;
                            try {
                                minInstant = Instant.parse(rangeFilter.getMin().toString());
                                maxInstant = Instant.parse(rangeFilter.getMax().toString());
                            } catch (Exception e) {
                                throw new IllegalArgumentException(
                                        "Min and Max must be of type Instant for column: " + rangeFilter.getColumn());
                            }
                            LOG.debug("Date range filtering {}, with min {} and max {}", rangeFilter.getColumn(),
                                    rangeFilter.getMin(), rangeFilter.getMax());
                            filterSelection = table.instantColumn(rangeFilter.getColumn())
                                    .isAfter(minInstant)
                                    .and(table.instantColumn(rangeFilter.getColumn())
                                    .isBefore(maxInstant));
                            break;
                        default:
                            throw new IllegalArgumentException(
                                    "Unsupported column type for range filter: " + columnTypeName);
                    }
                } else if (filter instanceof EqualsFilter) {
                    LOG.debug("EqualsFilter detected: {}", filter);
                    EqualsFilter<?> equalsFilter = (EqualsFilter<?>) filter;
                    Column<?> column = table.column(equalsFilter.getColumn());
                    String columnTypeName = column.type().name();
                    LOG.debug("value type is: {}", equalsFilter.getValue().getClass().getName());
                    switch (columnTypeName) {
                        case "BOOLEAN":
                            LOG.debug("Double equals filtering {}, with value {}", equalsFilter.getColumn(),
                                    equalsFilter.getValue());
                            boolean booleanValue = Boolean.parseBoolean(equalsFilter.getValue().toString().toLowerCase());
                            if (booleanValue) {
                                filterSelection = table.booleanColumn(equalsFilter.getColumn()).isTrue();
                            } else {
                                filterSelection = table.booleanColumn(equalsFilter.getColumn()).isFalse();
                            }
                            break;
                        case "DOUBLE":
                            if (!(equalsFilter.getValue() instanceof Double)) {
                                throw new IllegalArgumentException(
                                        "Value must be of type DOUBLE for column: " + equalsFilter.getColumn());
                            }
                            LOG.debug("Double equals filtering {}, with value {}", equalsFilter.getColumn(),
                                    equalsFilter.getValue());
                            double doubleValue = Double.parseDouble(equalsFilter.getValue().toString());
                            filterSelection = table.doubleColumn(equalsFilter.getColumn()).isEqualTo(doubleValue);
                            break;
                        case "INTEGER":
                            if (!(equalsFilter.getValue() instanceof Integer)) {
                                throw new IllegalArgumentException(
                                        "Value must be of type Integer for column: " + equalsFilter.getColumn());
                            }
                            LOG.debug("Integer equals filtering {}, with value {}", equalsFilter.getColumn(),
                                    equalsFilter.getValue());
                            int intValue = Integer.parseInt(equalsFilter.getValue().toString());
                            filterSelection = table.intColumn(equalsFilter.getColumn()).isEqualTo(intValue);
                            break;
                        case "STRING":
                            if (!(equalsFilter.getValue() instanceof String)) {
                                throw new IllegalArgumentException(
                                        "Value must be of type String for column: " + equalsFilter.getColumn());
                            }
                            LOG.debug("String equals filtering {}, with value {}", equalsFilter.getColumn(),
                                    equalsFilter.getValue());
                            filterSelection = table.stringColumn(equalsFilter.getColumn())
                                    .isEqualTo(equalsFilter.getValue().toString());
                            break;
                        case "LOCAL_DATE_TIME":
                            LocalDateTime localDateTimeValue;
                            try {
                                localDateTimeValue = LocalDateTime.parse(equalsFilter.getValue().toString());
                            } catch (Exception e) {
                                throw new IllegalArgumentException(
                                        "Value must be of type LocalDateTime for column: " + equalsFilter.getColumn());
                            }
                            LOG.debug("DateTime equals filtering {}, with value {}", equalsFilter.getColumn(),
                                    equalsFilter.getValue());
                            filterSelection = table.dateTimeColumn(equalsFilter.getColumn())
                                    .isEqualTo(localDateTimeValue);
                            break;
                        case "LOCAL_DATE":
                            LocalDate date;
                            try {
                                date = LocalDate.parse(equalsFilter.getValue().toString());
                            } catch (Exception e) {
                                throw new IllegalArgumentException(
                                        "Value must be of type LocalDate for column: " + equalsFilter.getColumn());
                            }
                            LOG.debug("Date equals filtering {}, with value {}", equalsFilter.getColumn(),
                            equalsFilter.getValue());
                            filterSelection = table.dateColumn(equalsFilter.getColumn())
                                    .isEqualTo(date);
                            break;
                        case "LOCAL_TIME":
                            LocalTime timeValue;
                            try {
                                timeValue = LocalTime.parse(equalsFilter.getValue().toString());
                            } catch (Exception e) {
                                throw new IllegalArgumentException(
                                        "Value must be of type LocalTime for column: " + equalsFilter.getColumn());
                            }
                            LOG.debug("Time equals filtering {}, with value {}", equalsFilter.getColumn(),
                                    equalsFilter.getValue());
                            filterSelection = table.timeColumn(equalsFilter.getColumn())
                                    .isEqualTo(timeValue);
                            break;
                        case "INSTANT":
                            Instant instantValue;
                            try {
                                instantValue = Instant.parse(equalsFilter.getValue().toString());
                            } catch (Exception e) {
                                throw new IllegalArgumentException(
                                        "Value must be of type Instant for column: " + equalsFilter.getColumn());
                            }
                            LOG.debug("Instant equals filtering {}, with value {}", equalsFilter.getColumn(),
                                    equalsFilter.getValue());
                            filterSelection = table.instantColumn(equalsFilter.getColumn())
                                    .isEqualTo(instantValue);
                            break;
                        default:
                            throw new IllegalArgumentException(
                                    "Unsupported column type for equals filter: " + columnTypeName);
                    }
                } else if (filter instanceof InequalityFilter) {
                    LOG.debug("InequalityFilter detected: {}", filter);
                    InequalityFilter<?> inequalityFilter = (InequalityFilter<?>) filter;
                    Column<?> column = table.column(inequalityFilter.getColumn());
                    String columnTypeName = column.type().name();
                    String operator = inequalityFilter.getOperator();
                    LOG.debug("value type is: {}", inequalityFilter.getValue().getClass().getName());
                    
                    switch (columnTypeName) {
                        case "DOUBLE":
                            if (!(inequalityFilter.getValue() instanceof Double)) {
                                try{
                                    Double.parseDouble(inequalityFilter.getValue().toString());
                                } catch (NumberFormatException e) {
                                    throw new IllegalArgumentException(
                                            "Value must be of type Double for column: " + inequalityFilter.getColumn());
                                }
                            }
                            double doubleValue = Double.parseDouble(inequalityFilter.getValue().toString());
                            LOG.debug("Double inequality filtering {}, with value {} and operator {}", 
                                    inequalityFilter.getColumn(), inequalityFilter.getValue(), operator);
                            
                            switch(operator) {
                                case "gt":
                                    filterSelection = table.doubleColumn(inequalityFilter.getColumn()).isGreaterThan(doubleValue);
                                    break;
                                case "lt":
                                    filterSelection = table.doubleColumn(inequalityFilter.getColumn()).isLessThan(doubleValue);
                                    break;
                                case "gte":
                                    filterSelection = table.doubleColumn(inequalityFilter.getColumn()).isGreaterThanOrEqualTo(doubleValue);
                                    break;
                                case "lte":
                                    filterSelection = table.doubleColumn(inequalityFilter.getColumn()).isLessThanOrEqualTo(doubleValue);
                                    break;
                                default:
                                    throw new IllegalArgumentException("Unsupported operator: " + operator);
                            }
                            break;
                        case "INTEGER":
                            if (!(inequalityFilter.getValue() instanceof Integer)) {
                                try {
                                    Integer.parseInt(inequalityFilter.getValue().toString());
                                } catch (NumberFormatException e) {
                                    throw new IllegalArgumentException(
                                            "Value must be of type Integer for column: " + inequalityFilter.getColumn()); 
                                }
                            }
                            int intValue = Integer.parseInt(inequalityFilter.getValue().toString());
                            LOG.debug("Integer inequality filtering {}, with value {} and operator {}", 
                                    inequalityFilter.getColumn(), inequalityFilter.getValue(), operator);
                            
                            switch(operator) {
                                case "gt":
                                    filterSelection = table.intColumn(inequalityFilter.getColumn()).isGreaterThan(intValue);
                                    break;
                                case "lt":
                                    filterSelection = table.intColumn(inequalityFilter.getColumn()).isLessThan(intValue);
                                    break;
                                case "gte":
                                    filterSelection = table.intColumn(inequalityFilter.getColumn()).isGreaterThanOrEqualTo(intValue);
                                    break;
                                case "lte":
                                    filterSelection = table.intColumn(inequalityFilter.getColumn()).isLessThanOrEqualTo(intValue);
                                    break;
                                default:
                                    throw new IllegalArgumentException("Unsupported operator: " + operator);
                            }
                            break;
                        case "LOCAL_DATE_TIME":
                            LocalDateTime dateTimeValue;
                            try {
                                dateTimeValue = LocalDateTime.parse(inequalityFilter.getValue().toString());
                            } catch (Exception e) {
                                throw new IllegalArgumentException(
                                        "Value must be of type LocalDateTime for column: " + inequalityFilter.getColumn());
                            }
                            
                            switch(operator) {
                                case "gt":
                                    filterSelection = table.dateTimeColumn(inequalityFilter.getColumn()).isAfter(dateTimeValue);
                                    break;
                                case "lt":
                                    filterSelection = table.dateTimeColumn(inequalityFilter.getColumn()).isBefore(dateTimeValue);
                                    break;
                                case "gte":
                                    filterSelection = table.dateTimeColumn(inequalityFilter.getColumn()).isOnOrAfter(dateTimeValue);
                                    break;
                                case "lte":
                                    filterSelection = table.dateTimeColumn(inequalityFilter.getColumn()).isOnOrBefore(dateTimeValue);
                                    break;
                                default:
                                    throw new IllegalArgumentException("Unsupported operator: " + operator);
                            }
                            break;
                        case "LOCAL_DATE":
                            LocalDate dateValue;
                            try {
                                dateValue = LocalDate.parse(inequalityFilter.getValue().toString());
                            } catch (Exception e) {
                                throw new IllegalArgumentException(
                                        "Value must be of type LocalDate for column: " + inequalityFilter.getColumn());
                            }
                            
                            switch(operator) {
                                case "gt":
                                    filterSelection = table.dateColumn(inequalityFilter.getColumn()).isAfter(dateValue);
                                    break;
                                case "lt":
                                    filterSelection = table.dateColumn(inequalityFilter.getColumn()).isBefore(dateValue);
                                    break;
                                case "gte":
                                    filterSelection = table.dateColumn(inequalityFilter.getColumn()).isOnOrAfter(dateValue);
                                    break;
                                case "lte":
                                    filterSelection = table.dateColumn(inequalityFilter.getColumn()).isOnOrBefore(dateValue);
                                    break;
                                default:
                                    throw new IllegalArgumentException("Unsupported operator: " + operator);
                            }
                            break;
                        case "LOCAL_TIME":
                            LocalTime timeValue;
                            try {
                                timeValue = LocalTime.parse(inequalityFilter.getValue().toString());
                            } catch (Exception e) {
                                throw new IllegalArgumentException(
                                        "Value must be of type LocalTime for column: " + inequalityFilter.getColumn());
                            }
                            
                            switch(operator) {
                                case "gt":
                                    filterSelection = table.timeColumn(inequalityFilter.getColumn()).isAfter(timeValue);
                                    break;
                                case "lt":
                                    filterSelection = table.timeColumn(inequalityFilter.getColumn()).isBefore(timeValue);
                                    break;
                                case "gte":
                                    filterSelection = table.timeColumn(inequalityFilter.getColumn()).isOnOrAfter(timeValue);
                                    break;
                                case "lte":
                                    filterSelection = table.timeColumn(inequalityFilter.getColumn()).isOnOrBefore(timeValue);
                                    break;
                                default:
                                    throw new IllegalArgumentException("Unsupported operator: " + operator);
                            }
                            break;
                        case "INSTANT":
                            Instant instantValue;
                            try {
                                instantValue = Instant.parse(inequalityFilter.getValue().toString());
                            } catch (Exception e) {
                                throw new IllegalArgumentException(
                                        "Value must be of type Instant for column: " + inequalityFilter.getColumn());
                            }
                            
                            switch(operator) {
                                case "gt":
                                    filterSelection = table.instantColumn(inequalityFilter.getColumn()).isAfter(instantValue);
                                    break;
                                case "lt":
                                    filterSelection = table.instantColumn(inequalityFilter.getColumn()).isBefore(instantValue);
                                    break;
                                case "gte":
                                    // There's no direct isOnOrAfter for Instant, so we combine isEqual and isAfter
                                    filterSelection = table.instantColumn(inequalityFilter.getColumn()).isEqualTo(instantValue)
                                            .or(table.instantColumn(inequalityFilter.getColumn()).isAfter(instantValue));
                                    break;
                                case "lte":
                                    // There's no direct isOnOrBefore for Instant, so we combine isEqual and isBefore
                                    filterSelection = table.instantColumn(inequalityFilter.getColumn()).isEqualTo(instantValue)
                                            .or(table.instantColumn(inequalityFilter.getColumn()).isBefore(instantValue));
                                    break;
                                default:
                                    throw new IllegalArgumentException("Unsupported operator: " + operator);
                            }
                            break;
                        default:
                            throw new IllegalArgumentException(
                                    "Unsupported column type for inequality filter: " + columnTypeName);
                    }
                } else if (filter instanceof StringFilter) {
                    LOG.debug("StringFilter detected: {}", filter);
                    StringFilter stringFilter = (StringFilter) filter;
                    Column<?> column = table.column(stringFilter.getColumn());
                    String columnTypeName = column.type().name();
                    
                    if (!columnTypeName.equals("STRING")) {
                        throw new IllegalArgumentException(
                                "String filter can only be applied to STRING columns, but column type is: " + columnTypeName);
                    }
                    
                    String value = stringFilter.getValue();
                    String operator = stringFilter.getOperator();
                    LOG.debug("String filtering {}, with value {} and operator {}", 
                            stringFilter.getColumn(), value, operator);
                    
                    switch (operator) {
                        case "contains":
                            filterSelection = table.stringColumn(stringFilter.getColumn()).containsString(value);
                            break;
                        case "startsWith":
                            filterSelection = table.stringColumn(stringFilter.getColumn()).startsWith(value);
                            break;
                        case "endsWith":
                            filterSelection = table.stringColumn(stringFilter.getColumn()).endsWith(value);
                            break;
                        default:
                            throw new IllegalArgumentException("Unsupported string operator: " + operator);
                    }
                }

                // Add other types of filters here
                selection = selection == null ? filterSelection : selection.and(filterSelection);
            }
        }
        LOG.debug("Filter Selection: {}", selection);

        Table resultTable = (selection != null) ? table.where(selection) : table;
        int rowCount = resultTable.rowCount();
        LOG.info("Row count after filtering: {}", rowCount);

        resultTable = applyColumnSelection(resultTable, tabularRequest.getColumns());
        if (tabularRequest.getAggregation() != null && !tabularRequest.getAggregation().isEmpty()) {
            resultTable = applyAggregation(resultTable, tabularRequest.getGroupBy(), tabularRequest.getAggregation());
        }

        resultTable = applyPagination(resultTable, tabularRequest.getLimit(), tabularRequest.getOffset());

        LOG.info("Final result after query has {} rows.", resultTable.rowCount());

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
