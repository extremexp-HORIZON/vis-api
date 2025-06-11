package gr.imsi.athenarc.xtremexpvisapi.domain.Query;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class MapDataResponse {

    private List<Object[]> points;

    private Map<String, List<String>> facets;

    private List<GroupedStats> series;

    private RectStats rectStats;

    private int fullyContainedTileCount;
    private int tileCount;
    private int pointCount;
    private int ioCount;

    private int totalTileCount;
    private int totalPointCount;
    
}
