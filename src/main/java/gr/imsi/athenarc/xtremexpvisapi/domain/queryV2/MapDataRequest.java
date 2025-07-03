package gr.imsi.athenarc.xtremexpvisapi.domain.queryV2;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.Map;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.aggregation.AggregationFunction;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.Rectangle;
import lombok.EqualsAndHashCode;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MapDataRequest extends DataRequest {
    AggregationFunction aggType;
    Rectangle rect;
    Map<String, String> categoricalFilters;
    List<String> groupByCols;
    String measureCol;
    Long from;
    Long to;
}