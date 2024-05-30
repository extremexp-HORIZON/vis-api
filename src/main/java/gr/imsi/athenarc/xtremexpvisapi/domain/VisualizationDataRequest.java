package gr.imsi.athenarc.xtremexpvisapi.domain;

import java.util.List;

import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.VisualFilter;

public class VisualizationDataRequest {

    private String datasetId;
    private ViewPort viewPort;
    private List<String> columns;
    private List<VisualFilter> filters;
    
    private String aggFunction;
    private Integer limit;
    private String scaler;
    
    public String getScaler() {
        return scaler;
    }
    public void setScaler(String scaler) {
        this.scaler = scaler;
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
    public List<VisualFilter> getFilters() {
        return filters;
    }
    public void setFilters(List<VisualFilter> filters) {
        this.filters = filters;
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
    public String getDatasetId() {
        return datasetId;
    }
    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }    
    @Override
    public String toString() {
        return "VisualizationDataRequest [datasetId=" + datasetId + ", viewPort=" + viewPort + ", columns=" + columns
                + ", aggFunction=" + aggFunction + ", filters=" + filters + ", limit=" + limit + ", scaler=" + scaler
                + "]";
    }
    
}
