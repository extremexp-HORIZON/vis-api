package gr.imsi.athenarc.xtremexpvisapi.domain.Query;

import java.util.List;
import java.util.Map;

import org.springframework.lang.NonNull;

import gr.imsi.athenarc.xtremexpvisapi.domain.QueryParams.Rectangle;
import gr.imsi.athenarc.xtremexpvisapi.domain.QueryParams.SourceType;
import gr.imsi.athenarc.xtremexpvisapi.domain.QueryParams.Enumeration.AggregateFunctionType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MapDataRequest {
    @NonNull
    private String datasetId;

    private SourceType type;
    
    private AggregateFunctionType aggType;

    private Rectangle rect;

    private Map<String, String> categoricalFilters;

    private List<String> groupByCols;

    private String measureCol;

    private Long from;
    
    private Long to;
}
