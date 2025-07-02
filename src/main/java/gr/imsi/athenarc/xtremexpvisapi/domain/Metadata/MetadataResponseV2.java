package gr.imsi.athenarc.xtremexpvisapi.domain.Metadata;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.Column;
import lombok.Data;

@Data
public class MetadataResponseV2 {
    private DatasetType datasetType;
    private List<String> fileNames;
    private List<Column> originalColumns;
    private int totalItems;
    private Map<String, List<?>> uniqueColumnValues;
    private boolean hasLatLonColumns;
    private List<String> timeColumn;
    // RawVis specific metadata (for now)
    private double xMin;
    private double xMax;
    private double yMin;
    private double yMax;
    private double queryXMin;
    private double queryXMax;
    private double queryYMin;
    private double queryYMax;
    private long timeMin;
    private long timeMax;
    private String measure0;
    private String measure1;
    private LinkedHashSet<String> dimensions = new LinkedHashSet<>();
    private LinkedHashSet<String> measures = new LinkedHashSet<>();
    private Map<String, List<String>> facets;
}
