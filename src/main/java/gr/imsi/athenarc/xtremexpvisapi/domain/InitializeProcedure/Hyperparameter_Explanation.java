package gr.imsi.athenarc.xtremexpvisapi.domain.InitializeProcedure;

import java.util.List;
import java.util.Map;

import gr.imsi.athenarc.xtremexpvisapi.domain.ExplabilityProcedure.ExplanationsRes;;

public class Hyperparameter_Explanation {

    private List<String> hyperparameterNames;
    private Map<String, ExplanationsRes> plots;
    private Map<String, ExplanationsRes> tables;
    private String pipelineMetrics;

    // Getters    
    public List<String> getHyperparameterNames() {
        return hyperparameterNames;
    }

    public Map<String, ExplanationsRes> getPlots() {
        return plots;
    }

    public Map<String, ExplanationsRes> getTables() {
        return tables;
    }

    // Setters
    public void setHyperparameterNames(List<String> hyperparameterNames) {
        this.hyperparameterNames = hyperparameterNames;
    }

    public void setPlots(Map<String, ExplanationsRes> plots) {
        this.plots = plots;
    }

    public void setTables(Map<String, ExplanationsRes> tables) {
        this.tables = tables;
    }

    @Override
    public String toString() {
        return "Hyperparameter_Explanation{" +
                "hyperparameterNames=" + hyperparameterNames +
                ", plots=" + plots +
                ", tables=" + tables +
                '}';
    }

    public String getPipelineMetrics() {
        return pipelineMetrics;
    }

    public void setPipelineMetrics(String pipelineMetrics) {
        this.pipelineMetrics = pipelineMetrics;
    }
    
}
