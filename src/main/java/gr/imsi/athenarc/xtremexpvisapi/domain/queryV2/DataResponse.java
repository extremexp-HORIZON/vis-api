package gr.imsi.athenarc.xtremexpvisapi.domain.queryV2;

import java.util.List;
import java.util.Map;

import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.Column;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.GroupedStats;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.RectStats;
import lombok.Data;

@Data
public class DataResponse {

    private String data;
    private List<Column> columns; // List to store column metadata
    private int totalItems; // New field for total item count
    private int querySize;

    // Map-specific fields
    private List<Object[]> points;
    private Map<String, List<String>> facets;
    private List<GroupedStats> series;
    private RectStats rectStats;
    private int pointCount;
    // private int fullyContainedTileCount;
    // private int tileCount;
    // private int ioCount;
    // private int totalTileCount;
    // private int totalPointCount;
}
