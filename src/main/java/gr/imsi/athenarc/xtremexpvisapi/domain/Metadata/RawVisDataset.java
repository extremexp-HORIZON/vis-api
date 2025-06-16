package gr.imsi.athenarc.xtremexpvisapi.domain.Metadata;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class RawVisDataset {

    private String id;

    private String name;

    private DatasetType type;

    private Boolean hasHeader;

    private String[] headers;

    private Integer objectCount;
    
    @JsonProperty("xMin")
    private Float xMin;

    @JsonProperty("xMax")
    private Float xMax;

    @JsonProperty("yMin")
    private Float yMin;

    @JsonProperty("yMax")
    private Float yMax;

    private Float queryXMin;

    private Float queryXMax;

    private Float queryYMin;

    private Float queryYMax;

    private String measure0;

    private String measure1;

    private String lat;

    private String lon;

    private long timeMin;

    private long timeMax;

    private LinkedHashSet<String> dimensions = new LinkedHashSet<>();

    private Map<String, List<String>> facets;
}
