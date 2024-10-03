package gr.imsi.athenarc.xtremexpvisapi.domain;

import java.util.List;

import gr.imsi.athenarc.xtremexpvisapi.domain.DataExplorationAdditionalParams.GeographicalParams;
import gr.imsi.athenarc.xtremexpvisapi.domain.DataExplorationAdditionalParams.TabularParams;
import gr.imsi.athenarc.xtremexpvisapi.domain.DataExplorationAdditionalParams.TemporalParams;
import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.VisualFilter;

public class VisualizationDataRequest {

    private String datasetId;
    private ViewPort viewPort;
    private List<String> columns;
    private List<VisualFilter> filters;
    private String aggFunction;
    private Integer limit;
    private String scaler;
    private String visualizationType;
    private String visualizationMethod;
    private TemporalParams temporalParams;
    private GeographicalParams geographicalParams;
    private TabularParams tabularParams;
    private Integer offset; // New field for offset
    
    public Integer getOffset() {
        return offset;
    } 
    public void setOffset(Integer offset) {
        this.offset = offset;
    }


    // Getters and Setters

    public TabularParams getTabularParams() {
        return tabularParams;
    }
    public void setTabularParams(TabularParams tabularParams) {
        this.tabularParams = tabularParams;
    }
    // Getters and Setters for GeographicalParams
    public GeographicalParams getGeographicalParams() {
        return geographicalParams;
    }
    public void setGeographicalParams(GeographicalParams geographicalParams) {
        this.geographicalParams = geographicalParams;
    }


    // Getters and Setters for TemporalParams
    public TemporalParams getTemporalParams() {
        return temporalParams;
    }
    public void setTemporalParams(TemporalParams temporalParams) {
        this.temporalParams = temporalParams;
    }

    // Getters and Setters for new fields
    public String getVisualizationType() {
        return visualizationType;
    }
    public void setVisualizationType(String visualizationType) {
        this.visualizationType = visualizationType;
    }
    public String getVisualizationMethod() {
        return visualizationMethod;
    }
    public void setVisualizationMethod(String visualizationMethod) {
        this.visualizationMethod = visualizationMethod;
    }
    
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
                + ", visualizationType=" + visualizationType + ", visualizationMethod=" + visualizationMethod + ", temporalParams=" + temporalParams + ", geographicalParams=" + geographicalParams + ", tabularParams=" + tabularParams + ", offset=" + offset + "]";
    }
    
}
