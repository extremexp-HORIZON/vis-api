package gr.imsi.athenarc.xtremexpvisapi.domain.Query;

import java.util.List;
import java.util.Map;

import gr.imsi.athenarc.xtremexpvisapi.domain.SourceType;
import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.AbstractFilter;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TabularRequest {
    String datasetId;
    Integer limit;
    List<String> columns;
    List<AbstractFilter> filters;
    Integer offset;
    List<String> groupBy;
    Map<String, Object> aggregation;
    SourceType type;
}
