package gr.imsi.athenarc.xtremexpvisapi.domain.DataExplorationAdditionalParams;


import java.util.Map;

import lombok.Data;

@Data
public class Aggregation {
    private Map<String, String> aggregationFunctions; // Key: column name, Value: aggregation function
}
