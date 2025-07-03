package gr.imsi.athenarc.xtremexpvisapi.domain.queryV2;

import java.util.List;
import java.util.Map;

import org.springframework.lang.NonNull;

import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.DataSource;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.aggregation.Aggregation;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.filter.AbstractFilter;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.aggregation.AggregationFunction;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.SourceType;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.Rectangle;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DataRequest {
    @NonNull
    DataSource dataSource;
    List<String> columns;
    Integer limit;
    Integer offset;
    List<String> groupBy;
    List<AbstractFilter> filters;
    List<Aggregation> aggregations;

    // Map-specific fields
    private String datasetId;
    private SourceType sourceType;
    private AggregationFunction aggType;
    private Rectangle rect;
    private Map<String, String> categoricalFilters;
    private List<String> groupByCols;
    private String measureCol;
    private Long from;
    private Long to;

    // Optional: to distinguish request type
    private String dataType; // "tabular", "map", "timeseries"
}
