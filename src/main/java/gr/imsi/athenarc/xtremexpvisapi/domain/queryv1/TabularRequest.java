package gr.imsi.athenarc.xtremexpvisapi.domain.queryv1;

import gr.imsi.athenarc.xtremexpvisapi.domain.queryv1.params.SourceType;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryv1.params.filter.AbstractFilter;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.lang.NonNull;

@Data
@AllArgsConstructor
public class TabularRequest {
  @NonNull String datasetId;
  SourceType type;
  List<String> columns;
  Integer limit;
  Integer offset;
  List<AbstractFilter> filters;
  List<String> groupBy;
  Map<String, Object> aggregation;
}
