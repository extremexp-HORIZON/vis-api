package gr.imsi.athenarc.xtremexpvisapi.domain;

import java.util.List;
import java.util.Map;

import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.VisualFilter;

public class VisualizationDataRequest {

    private String visualizationType;
    private ViewPort viewPort;
    private List<String> columns;
    private String aggFunction;
    private List<String> groupBy;
    private List<VisualFilter> filters;
    private Map<String, Object> constraints;
    private String taskId;
    private Integer limit;

    public String getVisualizationType() {
        return visualizationType;
    }
    public void setVisualizationType(String visualizationType) {
        this.visualizationType = visualizationType;
    }
    public List<String> getColumns() {
        return columns;
    }
    public void setColumns(List<String> columns) {
        this.columns = columns;
    }
    public String getAggFunction() {
        return aggFunction;
    }
    public void setAggFunction(String aggFunction) {
        this.aggFunction = aggFunction;
    }
    public List<String> getGroupBy() {
        return groupBy;
    }
    public void setGroupBy(List<String> groupBy) {
        this.groupBy = groupBy;
    }
    public List<VisualFilter> getFilters() {
        return filters;
    }
    public void setFilters(List<VisualFilter> filters) {
        this.filters = filters;
    }
    public Map<String, Object> getConstraints() {
        return constraints;
    }
    public void setConstraints(Map<String, Object> constraints) {
        this.constraints = constraints;
    }
    public String getTaskId() {
        return taskId;
    }
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    public ViewPort getViewPort() {
        return viewPort;
    }
    public void setViewPort(ViewPort viewPort) {
        this.viewPort = viewPort;
    }
    public Integer getLimit() {
        return limit;
    }
    public void setLimit(Integer limit) {
        this.limit = limit;
    }
    @Override
    public String toString() {
        return "VisualizationDataRequest [aggFunction=" + aggFunction + ", columns=" + columns + ", constraints="
                + constraints + ", filters=" + filters + ", groupBy=" + groupBy + ", limit=" + limit + ", taskId="
                + taskId + ", viewPort=" + viewPort + ", visualizationType=" + visualizationType + "]";
    }

}
