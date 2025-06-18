package gr.imsi.athenarc.xtremexpvisapi.domain.Query;

import java.util.Map;

import gr.imsi.athenarc.xtremexpvisapi.domain.QueryParams.Rectangle;
import lombok.Data;

@Data
public class TimeSeriesQuery {

    private Long from;

    private Long to;

    private String measure;

    private Rectangle rectangle;

    private long frequency; // seconds

    private Map<String, String> categoricalFilters;
    
}
