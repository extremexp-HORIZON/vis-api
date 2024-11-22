package gr.imsi.athenarc.xtremexpvisapi.domain;

import java.util.Map;



public class DataReduction {
    private String type; // raw, aggregation, visualization-aware
    private String aggInterval; // required for aggregation
    private Map<String, String> aggFunctions; // required for aggregation
    private ViewPort viewport; // required for visualization-aware
    private double errorBound; // optional

    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public String getAggInterval() {
        return aggInterval;
    }
    public void setAggInterval(String aggInterval) {
        this.aggInterval = aggInterval;
    }
    public Map<String, String> getAggFunctions() {
        return aggFunctions;
    }
    public void setAggFunctions(Map<String, String> aggFunctions) {
        this.aggFunctions = aggFunctions;
    }
    public ViewPort getViewport() {
        return viewport;
    }
    public void setViewport(ViewPort viewport) {
        this.viewport = viewport;
    }
    public double getErrorBound() {
        return errorBound;
    }
    public void setErrorBound(double errorBound) {
        this.errorBound = errorBound;
    }
    
}
