package gr.imsi.athenarc.xtremexpvisapi.datasource;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TimeSeriesRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.QueryParams.Filter.AbstractFilter;
import gr.imsi.athenarc.xtremexpvisapi.domain.QueryParams.Filter.EqualsFilter;
import gr.imsi.athenarc.xtremexpvisapi.domain.QueryParams.Filter.RangeFilter;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;
import tech.tablesaw.selection.Selection;

public class TimeSeriesQueryExecutor {
     private static final Logger LOG = LoggerFactory.getLogger(TimeSeriesQueryExecutor.class);

    public Table queryTabularData(Table table, TimeSeriesRequest timeSeriesRequest) {
        Selection selection = null;
        if(timeSeriesRequest.getFrom() != null && timeSeriesRequest.getTo()!=null){
            for (AbstractFilter filter : timeSeriesRequest.getFilters()) {
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
                            if (!(equalsFilter.getValue() instanceof Boolean)) {
                                throw new IllegalArgumentException(
                                        "Value must be of type Boolean for column: " + equalsFilter.getColumn());
                            }
                            LOG.debug("Double equals filtering {}, with value {}", equalsFilter.getColumn(),
                                    equalsFilter.getValue());
                            boolean booleanValue = Boolean.parseBoolean(equalsFilter.getValue().toString());
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
                                        "Min and Max must be of type LocalDate for column: " + equalsFilter.getColumn());
                            }
                            LOG.debug("Date range filtering {}, with value {}", equalsFilter.getColumn(),
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
                }

                // Add other types of filters here
                selection = selection == null ? filterSelection : selection.and(filterSelection);
            }
        }
        LOG.debug("Selection is: {}", selection);
     
        Table resultTable = (selection != null) ? table.where(selection) : table;
        resultTable = applyPagination(resultTable, timeSeriesRequest.getLimit(), timeSeriesRequest.getOffset());
        resultTable = applyColumnSelection(resultTable, timeSeriesRequest.getColumns());
       

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
