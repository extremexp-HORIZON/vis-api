package gr.imsi.athenarc.xtremexpvisapi.domain.DataExplorationAdditionalParams;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class TabularParams {
    private List<String> columns;
    private Map<String, Object> groups;  // Assuming groups is a dynamic object
    private int pages;
}
