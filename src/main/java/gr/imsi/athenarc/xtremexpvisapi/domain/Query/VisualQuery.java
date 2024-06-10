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

public class VisualQuery {

    private static final Logger LOG = LoggerFactory.getLogger(VisualQuery.class);

    private static DateTimeFormatter dateTimeFormatter =  
        new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd[ [HH][:mm][:ss][.SSS]]")
        .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
        .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
        .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
        .parseDefaulting(ChronoField.MILLI_OF_SECOND, 0)
        .toFormatter(); 
        
    String datasetId;
    ViewPort viewPort;
    Integer limit;
    List<String> columns;
    List<AbstractFilter> filters;
    String scaler;
    String aggFunction;
    

    public VisualQuery(String datasetId, ViewPort viewPort, List<String> columns, Integer limit, String scaler, String aggFunction){ 
        this.datasetId = datasetId;
        this.viewPort = viewPort;
        this.columns = columns;
        this.limit = limit;
        this.scaler=scaler;
        this.aggFunction=aggFunction;
    }

    public String getAggFunction() {
        return aggFunction;
    }

    public String getScaler() {
        return scaler;
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
                + ", viewPort=" + viewPort + ", limit=" + limit + ", isMinMaxScale=" + scaler + ", aggFunction=" + aggFunction + "]";
    }

    public void instantiateFilters(List<VisualFilter> visualFilters, List<VisualColumn> tableColumns){
        this.filters = visualFilters != null ?  visualFilters.stream().map(filter -> mapFilter(filter, tableColumns)).toList() : null;
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
                    default:
                        LOG.error("Unsupported column type for equals filter: {}", column.getType());
                        return null;
                }
            default:
                //TODO: add sth here
                return null;
        }
    }
}
