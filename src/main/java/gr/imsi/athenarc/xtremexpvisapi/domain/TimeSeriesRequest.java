package gr.imsi.athenarc.xtremexpvisapi.domain;

import java.util.List;

import lombok.Data;

@Data
public class TimeSeriesRequest {
    private SOURCE_TYPE type;
    private String datasetId; 
    private List<String> columns; 
    private String from; 
    private String to; 
    private Integer limit; 
    private Integer offset; 
    private DataReduction dataReduction;
}