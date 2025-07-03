package gr.imsi.athenarc.xtremexpvisapi.domain.queryV2;

import java.util.List;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.UnivariateDataPoint;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TimeSeriesDataResponse extends DataResponse {
    List<UnivariateDataPoint> timeSeriesPoints;
}