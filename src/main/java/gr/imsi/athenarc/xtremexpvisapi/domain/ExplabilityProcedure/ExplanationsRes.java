package gr.imsi.athenarc.xtremexpvisapi.domain.ExplabilityProcedure;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;



@JsonIgnoreProperties(ignoreUnknown = true)

@Data
public class ExplanationsRes {
    private String explainabilityType ;
    private String explanationMethod ;
    private String explainabilityModel ;
    private String plotName ;
    private String plotDescr ;
    private String plotType ;
    private Features features;
    private String[] featureList;
    private String[] hyperparameterList;
    private Axis  xAxis;
    private Axis  yAxis;
    private Axis  zAxis;
    private Map<String, TableContents> tableContents;
    
}
