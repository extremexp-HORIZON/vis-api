package gr.imsi.athenarc.xtremexpvisapi.domain.queryV2;

import java.util.List;

import org.springframework.lang.NonNull;

import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.DataSource;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.aggregation.Aggregation;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.filter.AbstractFilter;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "dataType", defaultImpl = DataRequest.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = MapDataRequest.class, name = "map"),
        @JsonSubTypes.Type(value = TimeSeriesDataRequest.class, name = "timeseries")
// Add TabularDataRequest if you have one
})
@Data
@NoArgsConstructor
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
    String dataType; // "map", "timeseries"
}
