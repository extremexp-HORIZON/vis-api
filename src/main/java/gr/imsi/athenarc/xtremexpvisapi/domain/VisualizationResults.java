package gr.imsi.athenarc.xtremexpvisapi.domain;

import java.util.List;

public class VisualizationResults {

    private String data;
    private List<String> fileNames;
    private List<VisualColumn> columns; // List to store column metadata
    private String timestampColumn; 
    private int totalItems; // New field for total item count


    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }

    public List<VisualColumn> getColumns() {
        return columns;
    }

    public void setColumns(List<VisualColumn> columns) {
        this.columns = columns;
    }

    public VisualizationResults() {

    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getTimestampColumn() {
        return timestampColumn;
    }

    public void setTimestampColumn(String timestampColumn) {
        this.timestampColumn = timestampColumn;
    }

    public List<String> getFileNames() {
        return fileNames;
    }

    public void setFileNames(List<String> fileNames) {
        this.fileNames = fileNames;
    }

    
}
