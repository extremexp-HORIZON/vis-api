package gr.imsi.athenarc.xtremexpvisapi.domain.queryV2;

import java.util.List;
import java.util.Map;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.GroupedStats;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.RectStats;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MapDataResponse extends DataResponse {
    List<Object[]> points;
    Map<String, List<String>> facets;
    List<GroupedStats> series;
    RectStats rectStats;
    int pointCount;
}