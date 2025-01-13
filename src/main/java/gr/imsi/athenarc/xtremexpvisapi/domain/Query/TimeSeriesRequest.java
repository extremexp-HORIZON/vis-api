package gr.imsi.athenarc.xtremexpvisapi.domain.Query;

import java.util.List;

import gr.imsi.athenarc.xtremexpvisapi.domain.QueryParams.DataReduction;
import gr.imsi.athenarc.xtremexpvisapi.domain.QueryParams.SourceType;
import gr.imsi.athenarc.xtremexpvisapi.domain.QueryParams.Filter.Filter;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TimeSeriesRequest {
    String datasetId;
    SourceType type;
    List<String> columns;
    Integer limit;
    Integer offset;
    List<Filter> filters; // Added to hold the instantiated filters
    String from;
    String to;
    DataReduction dataReduction;
}

