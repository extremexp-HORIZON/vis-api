package gr.imsi.athenarc.xtremexpvisapi.domain.InitializeProcedure;

import java.util.Map;

import gr.imsi.athenarc.xtremexpvisapi.domain.ExplabilityProcedure.ExplanationsRes;

public class Feature_Explanation {

    private String[] featureNames;
    private Map<String, ExplanationsRes> plots;
    private Map<String, ExplanationsRes> tables;
    private String modelMetrics;

    public String[] getFeatureNames() {
        return featureNames;
    }

    public void setFeatureNames(String[] featureNames) {
        this.featureNames = featureNames;
    }

    public Map<String, ExplanationsRes> getPlots() {
        return plots;
    }

    public void setPlots(Map<String, ExplanationsRes> plots) {
        this.plots = plots;
    }

    public Map<String, ExplanationsRes> getTables() {
        return tables;
    }

    public void setTables(Map<String, ExplanationsRes> tables) {
        this.tables = tables;
    }

    public String getModelMetrics() {
        return modelMetrics;
    }

    public void setModelMetrics(String modelMetrics) {
        this.modelMetrics = modelMetrics;
    }
    
}
