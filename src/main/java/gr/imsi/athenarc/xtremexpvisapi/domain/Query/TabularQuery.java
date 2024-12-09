package gr.imsi.athenarc.xtremexpvisapi.domain.Query;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gr.imsi.athenarc.xtremexpvisapi.domain.SOURCE_TYPE;
import gr.imsi.athenarc.xtremexpvisapi.domain.TabularColumn;
import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.AbstractFilter;
import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.EqualsFilter;
import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.RangeFilter;
import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.Filter;

public class TabularQuery {

    private static final Logger LOG = LoggerFactory.getLogger(TabularQuery.class);

    private static DateTimeFormatter dateTimeFormatter =  
        new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd[ [HH][:mm][:ss][.SSS]]")
        .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
        .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
        .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
        .parseDefaulting(ChronoField.MILLI_OF_SECOND, 0)
        .toFormatter(); 
        
    String datasetId;
    Integer limit;
    List<String> columns;
    List<AbstractFilter> filters;
    Integer offset;
    List<String> groupBy;
    Map<String, Object> aggregation;
    SOURCE_TYPE type;
    
    public TabularQuery(String datasetId, Integer limit, List<String> columns, Integer offset, List<String> groupBy, Map<String, Object> aggregation, SOURCE_TYPE type) {

        this.datasetId = datasetId;
        this.limit = limit;
        this.columns = columns;
        this.offset = offset;
        this.groupBy = groupBy;
        this.aggregation = aggregation;
        this.type = type;
    }


    
   public void populateAllColumnsIfEmpty(Map<String, List<Object>> jsonData) {
        if (this.columns == null || this.columns.isEmpty()) {
            // Set columns to all available keys in the JSON data
            this.columns = new ArrayList<>(jsonData.keySet());
        }
    }

    public void instantiateFilters(List<Filter> filters, List<TabularColumn> tableColumns){
        this.filters = filters != null ?  filters.stream().map(filter -> mapFilter(filter, tableColumns)).toList() : null;
    }

    private AbstractFilter mapFilter(AbstractFilter filter, List<TabularColumn> columns){
        TabularColumn column =  columns.stream()
                           .filter(obj -> obj.getName().equals(filter.getColumn()))
                           .findFirst()
                           .orElse(null);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode value = mapper.valueToTree(filter.getValue());
        switch(filter.getType()){
            case "range":
                // Handle range filter
                RangeFilter rangeFilter = new RangeFilter();
                LOG.info("type: {}", column.getType());
                switch(column.getType()){
                    case "INTEGER" :
                        Integer minIntNumber = value.get("min").asInt();
                        Integer maxIntNumber = value.get("max").asInt();
                        return rangeFilter.new IntegerRangeFilter(column.getName(), minIntNumber, maxIntNumber);
                    case "FLOAT" :
                        Double minFloatNumber = value.get("min").asDouble();
                        Double maxFloatNumber = value.get("max").asDouble();
                        return rangeFilter.new DoubleRangeFilter(column.getName(), minFloatNumber, maxFloatNumber);
                    case "DOUBLE" :
                        Double minDoubleNumber = value.get("min").asDouble();
                        Double maxDoubleNumber = value.get("max").asDouble();
                        return rangeFilter.new DoubleRangeFilter(column.getName(), minDoubleNumber, maxDoubleNumber);
                    case "LOCAL_DATE_TIME":
                        LocalDateTime minDateTime = LocalDateTime.parse(value.get("min").asText(), dateTimeFormatter);
                        LocalDateTime maxDateTime =  LocalDateTime.parse(value.get("max").asText(), dateTimeFormatter);
                        return rangeFilter.new DateTimeRangeFilter(column.getName(), minDateTime, maxDateTime);
                    default:
                        //TODO: add sth here
                        return null;
                }
            case "equals":
                EqualsFilter equalsFilter = new EqualsFilter();
                LOG.info("type: {}", column.getType());
                switch(column.getType()){
                    case "INTEGER":
                        Integer number = value.asInt();
                        LOG.debug("Creating equals filter for numeric type: {}", number);
                        return equalsFilter.new IntegerEqualsFilter(column.getName(), number);
                    case "FLOAT":
                    case "DOUBLE":
                        Double doubleNumber = value.asDouble();
                        LOG.debug("Creating equals filter for numeric type: {}", doubleNumber);
                        return equalsFilter.new DoubleEqualsFilter(column.getName(), doubleNumber);
                    case "LOCAL_DATE_TIME":
                        LocalDateTime dateTime = LocalDateTime.parse(value.asText(), dateTimeFormatter);
                        LOG.debug("Creating equals filter for date/time: {}", dateTime);
                        return equalsFilter.new DateTimeEqualsFilter(column.getName(), dateTime);
                    case "STRING":  // Add this case
                        String stringValue = value.asText();  // Extract the string value
                        LOG.debug("Creating equals filter for string type: {}", stringValue);
                        return equalsFilter.new StringEqualsFilter(column.getName(), stringValue);  // Return a StringEqualsFilter
                    default:
                        LOG.error("Unsupported column type for equals filter: {}", column.getType());
                        return null;
                }
            default:
                //TODO: add sth here
                return null;
        }
    }

    public String getDatasetId() {
        return datasetId;
    }

    public List<String> getColumns() {
        return columns;
    }

    public List<AbstractFilter> getFilters() {
        return filters;
    }
    public Integer getLimit() {
        return limit;
    }

    public Integer getOffset() {
        return offset;
    }
    public List<String> getGroupBy() {
        return groupBy;
    }

    public Map<String, Object> getAggregation() {
        return aggregation;
    }

    public SOURCE_TYPE getType() {
        return type;
    }
    
    @Override
    public String toString() {
        return "TabularQuery [datasetId=" + datasetId 
                + ", filters=" + filters 
                + ", columns=" + columns 
                + ", limit=" + limit 
                + ", offset=" + offset 
                + ", groupBy=" + groupBy
                + ", aggregation=" + aggregation + "]";
    }
}
