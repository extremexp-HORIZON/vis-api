package gr.imsi.athenarc.xtremexpvisapi.domain;
import lombok.Data;

@Data
public class TimeSeriesResponse {

    private String data;
    private Integer totalRecords;
    private Integer limit;
    private Integer offset;
}
