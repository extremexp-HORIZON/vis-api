package gr.imsi.athenarc.xtremexpvisapi.domain;
import java.util.List;
import java.util.Map;

import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.VisualFilter;

public class TabularRequest {
    private String datasetId; // Required
    private List<VisualFilter> filters;
    private List<String> columns; // Optional
    private Integer limit; // Optional
    private Integer offset; // Optional
    private List<String> groupBy; // Optional
    private Map<String, Object> aggregation; // Optional
    public String getDatasetId() {
        return datasetId;
    }
    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }
    public List<VisualFilter> getFilters() {
        return filters;
    }
    public void setFilters(List<VisualFilter> filters) {
        this.filters = filters;
    }
    public List<String> getColumns() {
        return columns;
    }
    public void setColumns(List<String> columns) {
        this.columns = columns;
    }
    public Integer getLimit() {
        return limit;
    }
    public void setLimit(Integer limit) {
        this.limit = limit;
    }
    public Integer getOffset() {
        return offset;
    }
    public void setOffset(Integer offset) {
        this.offset = offset;
    }
    public List<String> getGroupBy() {
        return groupBy;
    }
    public void setGroupBy(List<String> groupBy) {
        this.groupBy = groupBy;
    }
    public Map<String, Object> getAggregation() {
        return aggregation;
    }
    public void setAggregation(Map<String, Object> aggregation) {
        this.aggregation = aggregation;
    }

    @Override
    public String toString() {
        return "VisualizationDataRequest [datasetId=" + datasetId 
        + ", filters=" + filters
        + ", limit=" + limit 
        + ", groupBy=" + groupBy
        + ", aggregation=" + aggregation
        + ", columns=" + columns
        + ", offset=" + offset + "]";
    }
    
}
   