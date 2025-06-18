package gr.imsi.athenarc.xtremexpvisapi.domain.Query;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UnivariateDataPoint {
    private long timestamp;
    private double value;
}
