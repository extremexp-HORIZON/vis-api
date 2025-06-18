package gr.imsi.athenarc.xtremexpvisapi.domain.queryV2;
import java.util.List;

import org.springframework.lang.NonNull;

import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.DatasetMeta;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.aggregation.Aggregation;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.filter.AbstractFilter;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DataRequest {
    @NonNull
    DatasetMeta datasetMeta;
    List<String> columns;
    Integer limit;
    Integer offset;
    List<String> groupBy;
    List<AbstractFilter> filters;
    List<Aggregation> aggregations;
}

