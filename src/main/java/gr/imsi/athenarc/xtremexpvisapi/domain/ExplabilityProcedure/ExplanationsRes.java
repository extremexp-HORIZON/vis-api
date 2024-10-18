package gr.imsi.athenarc.xtremexpvisapi.domain.ExplabilityProcedure;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;



@JsonIgnoreProperties(ignoreUnknown = true)


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

   
    public Map<String, TableContents> getTableContents() {
        return tableContents;
    }
    public void setTableContents(Map<String, TableContents> tableContents) {
        this.tableContents = tableContents;
    }
    public Features getFeatures() {
        return features;
    }
    public void setFeatures(Features features) {
        this.features = features;
    }
    public Axis getxAxis() {
        return xAxis;
    }
    public void setxAxis(Axis xAxis) {
        this.xAxis = xAxis;
    }
    public Axis getyAxis() {
        return yAxis;
    }
    public void setyAxis(Axis yAxis) {
        this.yAxis = yAxis;
    }
    public Axis getzAxis() {
        return zAxis;
    }
    public void setzAxis(Axis zAxis) {
        this.zAxis = zAxis;
    }
    public String getExplainabilityType() {
        return explainabilityType;
    }
    public void setExplainabilityType(String explainabilityType) {
        this.explainabilityType = explainabilityType;
    }
    
    public String getExplanationMethod() {
        return explanationMethod;
    }
    public void setExplanationMethod(String explanationMethod) {
        this.explanationMethod = explanationMethod;
    }
    public String getExplainabilityModel() {
        return explainabilityModel;
    }
    public void setExplainabilityModel(String explainabilityModel) {
        this.explainabilityModel = explainabilityModel;
    }
    public String getPlotName() {
        return plotName;
    }
    public void setPlotName(String plotName) {
        this.plotName = plotName;
    }
    public String getPlotDescr() {
        return plotDescr;
    }
    public void setPlotDescr(String plotDescr) {
        this.plotDescr = plotDescr;
    }
    public String getPlotType() {
        return plotType;
    }
    public void setPlotType(String plotType) {
        this.plotType = plotType;
    }
    public String[] getFeatureList() {
        return featureList;
    }
    public void setFeatureList(String[] featureList) {
        this.featureList = featureList;
    }
    public String[] getHyperparameterList() {
        return hyperparameterList;
    }
    public void setHyperparameterList(String[] hyperparameterList) {
        this.hyperparameterList = hyperparameterList;
    }

    
}
