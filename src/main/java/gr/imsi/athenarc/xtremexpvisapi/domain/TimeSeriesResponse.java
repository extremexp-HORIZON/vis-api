package gr.imsi.athenarc.xtremexpvisapi.domain;
import java.util.Map;

import gr.imsi.athenarc.visual.middleware.domain.TimeInterval;
import lombok.Data;

@Data
public class TimeSeriesResponse {

    private String data;
    private Integer totalRecords;
    private Integer limit;
    private Integer offset;
    private Map<String, TimeInterval> fileTimeRange;
}
