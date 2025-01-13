package gr.imsi.athenarc.xtremexpvisapi.domain.Explainability;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class ExplanationsRes {
    private String explainabilityType;
    private String explanationMethod;
    private String explainabilityModel;
    private String plotName;
    private String plotDescr;
    private String plotType;
    private Features features;
    private String[] hyperparameterList;
    private String[] featureList;
    private Map<String, TableContents> tableContents;
    private Map<String, TableContents> affectedClusters;
    private Float TotalEffectiveness;
    private Float TotalCost;
    private Map<String, TableContents> actions;
    @JsonProperty("xaxis")
    private Axis xAxis;
    @JsonProperty("yaxis")
    private Axis yAxis;
    @JsonProperty("zaxis")
    private Axis zAxis;

    
}
