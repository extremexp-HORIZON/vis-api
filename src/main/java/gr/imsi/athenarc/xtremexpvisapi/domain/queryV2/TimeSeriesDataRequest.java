package gr.imsi.athenarc.xtremexpvisapi.domain.queryV2;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Map;

import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.Rectangle;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TimeSeriesDataRequest extends DataRequest {
    long frequency;
    Rectangle rect;
    Map<String, String> categoricalFilters;
    String measureCol;
    Long from;
    Long to;
}