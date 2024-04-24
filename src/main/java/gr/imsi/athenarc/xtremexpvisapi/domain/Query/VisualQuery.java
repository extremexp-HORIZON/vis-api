package gr.imsi.athenarc.xtremexpvisapi.domain.Query;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gr.imsi.athenarc.xtremexpvisapi.domain.VisualColumn;
import gr.imsi.athenarc.xtremexpvisapi.domain.ViewPort;
import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.AbstractFilter;
import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.EqualsFilter;
import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.RangeFilter;
import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.VisualFilter;

public class VisualQuery{

    private static final Logger LOG = LoggerFactory.getLogger(VisualQuery.class);

    private static DateTimeFormatter dateTimeFormatter =  
        new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd[ [HH][:mm][:ss][.SSS]]")
        .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
        .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
        .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
        .toFormatter(); 
        
    String datasetId;
    ViewPort viewPort;
    Integer limit;
    List<String> columns;
    List<AbstractFilter> filters;

    public VisualQuery(String datasetId, ViewPort viewPort, List<String> columns, Integer limit) {
        this.datasetId = datasetId;
        this.viewPort = viewPort;
        this.columns = columns;
        this.limit = limit;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public List<String> getColumns() {
        return columns;
    }

    public ViewPort getViewPort() {
        return viewPort;
    }
    public List<AbstractFilter> getFilters() {
        return filters;
    }
    public Integer getLimit() {
        return limit;
    }
    
    @Override
    public String toString() {
        return "VisualQuery [datasetId=" + datasetId + ", filters=" + filters + ", columns=" + columns 
                + ", viewPort=" + viewPort + "]";
    }

    public void instantiateFilters(List<VisualFilter> visualFilters, List<VisualColumn> tableColumns){
        this.filters = visualFilters.stream().map(filter -> mapFilter(filter, tableColumns)).toList();
    }

    private AbstractFilter mapFilter(AbstractFilter filter, List<VisualColumn> columns){
        VisualColumn column =  columns.stream()
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
                        Double minIntNumber = value.get("min").asDouble();
                        Double maxIntNumber = value.get("max").asDouble();
                        return rangeFilter.new NumberRangeFilter(column.getName(), minIntNumber, maxIntNumber);
                    case "FLOAT" :
                        Double minFloatNumber = value.get("min").asDouble();
                        Double maxFloatNumber = value.get("max").asDouble();
                        return rangeFilter.new NumberRangeFilter(column.getName(), minFloatNumber, maxFloatNumber);
                    case "DOUBLE" :
                        Double minDoubleNumber = value.get("min").asDouble();
                        Double maxDoubleNumber = value.get("max").asDouble();
                        return rangeFilter.new NumberRangeFilter(column.getName(), minDoubleNumber, maxDoubleNumber);
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
                    case "INTEGER" :
                        Double number = value.get("value").asDouble();
                        return equalsFilter.new NumberEqualsFilter(column.getName(), number);
                    case "FLOAT" :
                        Double floatNumber = value.get("value").asDouble();
                        return equalsFilter.new NumberEqualsFilter(column.getName(), floatNumber);
                    case "DOUBLE" :
                        Double doubleNumber = value.get("value").asDouble();
                        return equalsFilter.new NumberEqualsFilter(column.getName(), doubleNumber);
                    case "LOCAL_DATE_TIME":
                        LocalDateTime dateTime = LocalDateTime.parse(value.get("value").asText(), dateTimeFormatter);
                        return equalsFilter.new DateTimeEqualsFilter(column.getName(), dateTime);
                    default:
                        //TODO: add sth here
                        return null;
                }
            default:
                //TODO: add sth here
                return null;
        }
    }
}
