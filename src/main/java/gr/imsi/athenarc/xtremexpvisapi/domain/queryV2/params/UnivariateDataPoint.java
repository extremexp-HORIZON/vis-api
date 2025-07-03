package gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UnivariateDataPoint {
    private long timestamp;
    private double value;
}
