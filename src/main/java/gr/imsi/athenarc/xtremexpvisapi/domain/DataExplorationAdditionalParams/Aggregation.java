package gr.imsi.athenarc.xtremexpvisapi.domain.DataExplorationAdditionalParams;


import java.util.Map;

public class Aggregation {
    private Map<String, String> aggregationFunctions; // Key: column name, Value: aggregation function

    // Getters and Setters
    public Map<String, String> getAggregationFunctions() {
        return aggregationFunctions;
    }

    public void setAggregationFunctions(Map<String, String> aggregationFunctions) {
        this.aggregationFunctions = aggregationFunctions;
    }

    @Override
    public String toString() {
        return "Aggregation [aggregationFunctions=" + aggregationFunctions + "]";
    }
}
