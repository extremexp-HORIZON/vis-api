package gr.imsi.athenarc.xtremexpvisapi.domain.queryv1;

import java.util.List;

import org.springframework.lang.NonNull;

import gr.imsi.athenarc.xtremexpvisapi.domain.queryv1.params.DataReduction;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryv1.params.SourceType;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryv1.params.filter.AbstractFilter;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TimeSeriesRequest {
    @NonNull
    String datasetId;
    SourceType type;
    List<String> columns;
    Integer limit;
    Integer offset;
    List<AbstractFilter> filters; // Added to hold the instantiated filters
    String from;
    String to;
    DataReduction dataReduction;
}

