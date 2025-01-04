package gr.imsi.athenarc.xtremexpvisapi.domain.Query;

import java.util.List;

import gr.imsi.athenarc.xtremexpvisapi.domain.DataReduction;
import gr.imsi.athenarc.xtremexpvisapi.domain.SourceType;
import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.AbstractFilter;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TimeSeriesRequest {
    String datasetId;
    List<String> columns;
    String from;
    String to;
    Integer limit;
    Integer offset;
    DataReduction dataReduction;
    SourceType type;
    List<AbstractFilter> filters; // Added to hold the instantiated filters
}

