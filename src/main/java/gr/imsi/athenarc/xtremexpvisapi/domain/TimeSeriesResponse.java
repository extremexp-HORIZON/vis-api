package gr.imsi.athenarc.xtremexpvisapi.domain;

import java.util.List;

public class TimeSeriesResponse {
    private String data;
    private Integer totalRecords;
    private Integer limit;
    private Integer offset;
    private List<VisualColumn> columns; // List to store column metadata
    private String timestampColumn; 
    public List<VisualColumn> getColumns() {
        return columns;
    }
    public void setColumns(List<VisualColumn> columns) {
        this.columns = columns;
    }
    public String getTimestampColumn() {
        return timestampColumn;
    }
    public void setTimestampColumn(String timestampColumn) {
        this.timestampColumn = timestampColumn;
    }
    public String getData() {
        return data;
    }
    public void setData(String data) {
        this.data = data;
    }
    public Integer getTotalRecords() {
        return totalRecords;
    }
    public void setTotalRecords(Integer totalRecords) {
        this.totalRecords = totalRecords;
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
    
}
