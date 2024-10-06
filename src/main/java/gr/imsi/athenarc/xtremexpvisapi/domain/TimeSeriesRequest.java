package gr.imsi.athenarc.xtremexpvisapi.domain;

import java.util.List;

public class TimeSeriesRequest {
    private String datasetId; // required
    private String timestampColumn; // optional
    private List<String> columns; // required
    private String from; // optional (ISO 8601 format)
    private String to; // optional (ISO 8601 format)
    private Integer limit; // optional
    private Integer offset; // optional
    private DataReduction dataReduction; // optional
    public String getDatasetId() {
        return datasetId;
    }
    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }
    public String getTimestampColumn() {
        return timestampColumn;
    }
    public void setTimestampColumn(String timestampColumn) {
        this.timestampColumn = timestampColumn;
    }
    public List<String> getColumns() {
        return columns;
    }
    public void setColumns(List<String> columns) {
        this.columns = columns;
    }
    public String getFrom() {
        return from;
    }
    public void setFrom(String from) {
        this.from = from;
    }
    public String getTo() {
        return to;
    }
    public void setTo(String to) {
        this.to = to;
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
    public DataReduction getDataReduction() {
        return dataReduction;
    }
    public void setDataReduction(DataReduction dataReduction) {
        this.dataReduction = dataReduction;
    }
    @Override
    public String toString() {
        return "TimeSeriesRequest [datasetId=" + datasetId + ", timestampColumn=" + timestampColumn + ", columns=" + columns
                + ", from=" + from + ", to=" + to + ", limit=" + limit + ", offset=" + offset
                + ", dataReduction=" + dataReduction +"]";
    }


   
   
    
    // getters and setters
}