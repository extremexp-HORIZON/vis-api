package gr.imsi.athenarc.xtremexpvisapi.domain;
import java.util.List;
import java.util.Map;

import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.Filter;
import lombok.Data;

@Data
public class TabularRequest {
    private String datasetId; 
    private List<Filter> filters;
    private List<String> columns; 
    private Integer limit; 
    private Integer offset;
    private List<String> groupBy; 
    private Map<String, Object> aggregation;
    private SOURCE_TYPE type;
}
   