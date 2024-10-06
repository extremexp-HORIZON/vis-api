package gr.imsi.athenarc.xtremexpvisapi.domain.Query;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Map;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gr.imsi.athenarc.xtremexpvisapi.domain.VisualColumn;
import gr.imsi.athenarc.xtremexpvisapi.domain.DataReduction;
import gr.imsi.athenarc.xtremexpvisapi.domain.ViewPort;
import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.AbstractFilter;
import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.EqualsFilter;
import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.RangeFilter;
import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.VisualFilter;
import java.time.temporal.ChronoUnit;

public class TimeSeriesQuery {

    private static final Logger LOG = LoggerFactory.getLogger(TimeSeriesQuery.class);

    private static DateTimeFormatter dateTimeFormatter =  
        new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd[ [HH][:mm][:ss][.SSS]]")
        .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
        .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
        .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
        .parseDefaulting(ChronoField.MILLI_OF_SECOND, 0)
        .toFormatter(); 
        
    String datasetId;
    String timestampColumn;
    List<String> columns;
    String from;
    String to;
    Integer limit;
    Integer offset;
    DataReduction dataReduction;

    List<AbstractFilter> filters; // Added to hold the instantiated filters

    
   
    

   

    public TimeSeriesQuery(String datasetId,String timestampColumn, List<String> columns,String from, String to, Integer limit, Integer offset,DataReduction dataReduction) {

        this.datasetId = datasetId;
        this.timestampColumn = timestampColumn;
        this.columns = columns;
        this.from = from;
        this.to = to;
        this.limit = limit;
        this.offset = offset;
        this.dataReduction = dataReduction;
      
    }


   

    public String getDatasetId() {
        return datasetId;
    }




    public String getTimestampColumn() {
        return timestampColumn;
    }




    public List<String> getColumns() {
        return columns;
    }




    public String getFrom() {
        return from;
    }




    public String getTo() {
        return to;
    }




    public Integer getLimit() {
        return limit;
    }




    public Integer getOffset() {
        return offset;
    }




    public DataReduction getDataReduction() {
        return dataReduction;
    }

    public List<AbstractFilter> getFilters() {
        return filters;
    }

    /**
     * Instantiates the filters based on the provided timestampColumn, from, and to values.
     */
    public void instantiateFilters() {
        if (timestampColumn != null && from != null && to != null) {
            this.filters = List.of(mapTimestampFilter(timestampColumn, from, to));
        } else {
            LOG.error("Invalid timestamp or range values.");
        }
    }

    /**
     * Maps the from and to values into a DateTimeRangeFilter for the timestamp column.
     * 
     * @param timestampColumn The column representing the timestamp.
     * @param from The start of the time range.
     * @param to The end of the time range.
     * @return A DateTimeRangeFilter that filters the timestamp column.
     */
    private AbstractFilter mapTimestampFilter(String timestampColumn, String from, String to) {
        try {
            LocalDateTime fromDateTime = LocalDateTime.parse(from, dateTimeFormatter);
            LocalDateTime toDateTime = LocalDateTime.parse(to, dateTimeFormatter);
            
            RangeFilter rangeFilter = new RangeFilter();
            LOG.info("Creating DateTimeRangeFilter for column: {} from: {} to: {}", timestampColumn, fromDateTime, toDateTime);
            
            // Return the DateTimeRangeFilter for the timestamp column
            return rangeFilter.new DateTimeRangeFilter(timestampColumn, fromDateTime, toDateTime);
        } catch (Exception e) {
            LOG.error("Error parsing date values for the timestamp column", e);
            return null;
        }
    }



    // public void instantiateFilters() {
    //     if (timestampColumn != null && from != null && to != null) {
    //         this.filters = List.of(mapTimestampFilter(timestampColumn, from, to));
    //     } else {
    //         LOG.error("Invalid timestamp or range values.");
    //     }
    // }

    // // New method to handle the timestamp filter logic
    // private AbstractFilter mapTimestampFilter(String timestampColumn, String from, String to) {
    //     try {
    //         LocalDateTime fromDateTime = LocalDateTime.parse(from, dateTimeFormatter);
    //         LocalDateTime toDateTime = LocalDateTime.parse(to, dateTimeFormatter);
            
    //         RangeFilter rangeFilter = new RangeFilter();
    //         LOG.info("Creating DateTimeRangeFilter for column: {} from: {} to: {}", timestampColumn, fromDateTime, toDateTime);
            
    //         // Return the DateTimeRangeFilter for the timestamp column
    //         return rangeFilter.new DateTimeRangeFilter(timestampColumn, fromDateTime, toDateTime);
    //     } catch (Exception e) {
    //         LOG.error("Error parsing date values for the timestamp column", e);
    //         return null;
    //     }
    // }
    @Override
    public String toString() {
        return "TabularQuery [datasetId=" + datasetId 
                + ", columns=" + columns 
                + ", from=" + from 
                + ", to=" + to
                + ", limit=" + limit 
                + ", offset=" + offset
                + ", dataReduction=" + dataReduction 
                + ", timestampColumn=" + timestampColumn + "]";
    }
}

