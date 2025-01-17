package gr.imsi.athenarc.xtremexpvisapi.domain.Query;

import java.util.List;
import java.util.Map;

import org.springframework.lang.NonNull;

import gr.imsi.athenarc.xtremexpvisapi.domain.QueryParams.SourceType;
import gr.imsi.athenarc.xtremexpvisapi.domain.QueryParams.Filter.AbstractFilter;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TabularRequest {
    @NonNull
    String datasetId;
    SourceType type;
    List<String> columns;
    Integer limit;
    Integer offset;
    List<AbstractFilter> filters;
    List<String> groupBy;
    Map<String, Object> aggregation;
}
