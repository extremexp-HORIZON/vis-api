package gr.imsi.athenarc.xtremexpvisapi.domain.queryv2;

import gr.imsi.athenarc.xtremexpvisapi.domain.queryv2.params.DataSource;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryv2.params.aggregation.Aggregation;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryv2.params.filter.AbstractFilter;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.lang.NonNull;

@Data
@AllArgsConstructor
public class DataRequest {
  @NonNull DataSource dataSource;
  List<String> columns;
  Integer limit;
  Integer offset;
  List<String> groupBy;
  List<AbstractFilter> filters;
  List<Aggregation> aggregations;
  // Optional flag to control whether executeDataRequest should compute
  // the total number of matching rows (ignoring LIMIT/OFFSET).
  // Null means "use default behavior" (count enabled).
  Boolean includeTotalItems;
}
