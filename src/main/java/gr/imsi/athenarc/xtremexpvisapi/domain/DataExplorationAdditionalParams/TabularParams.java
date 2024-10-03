package gr.imsi.athenarc.xtremexpvisapi.domain.DataExplorationAdditionalParams;

import java.util.List;
import java.util.Map;

public class TabularParams {
    private List<String> columns;
    private Map<String, Object> groups;  // Assuming groups is a dynamic object
    private int pages;
    public List<String> getColumns() {
        return columns;
    }
    public void setColumns(List<String> columns) {
        this.columns = columns;
    }
    public Map<String, Object> getGroups() {
        return groups;
    }
    public void setGroups(Map<String, Object> groups) {
        this.groups = groups;
    }
    public int getPages() {
        return pages;
    }
    public void setPages(int pages) {
        this.pages = pages;
    }
    
    
    // Getters and Setters
}
