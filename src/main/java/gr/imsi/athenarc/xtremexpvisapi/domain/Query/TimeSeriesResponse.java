package gr.imsi.athenarc.xtremexpvisapi.domain.Query;
import java.util.Map;

import lombok.Data;

@Data
public class TimeSeriesResponse {

    private String data;
    private Integer totalRecords;
    private Integer limit;
    private Integer offset;
}
