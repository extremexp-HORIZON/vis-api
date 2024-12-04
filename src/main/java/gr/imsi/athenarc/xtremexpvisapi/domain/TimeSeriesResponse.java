package gr.imsi.athenarc.xtremexpvisapi.domain;

import java.util.List;

import lombok.Data;

@Data
public class TimeSeriesResponse {
    private String data;
    private Integer totalRecords;
    private Integer limit;
    private Integer offset;
    private List<TabularColumn> columns; // List to store column metadata
    private String timestampColumn; 
}
