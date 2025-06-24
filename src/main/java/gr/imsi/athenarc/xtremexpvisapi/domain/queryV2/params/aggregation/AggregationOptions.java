package gr.imsi.athenarc.xtremexpvisapi.domain.queryv2.params.aggregation;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AggregationOptions {
    private boolean distinct = false;
    private Double percentileValue; // For percentile functions (0.0 to 1.0)
    private String separator; // For STRING_AGG
    private String orderBy; // For ordered aggregations like ARRAY_AGG
    private String orderDirection = "ASC"; // ASC or DESC
}