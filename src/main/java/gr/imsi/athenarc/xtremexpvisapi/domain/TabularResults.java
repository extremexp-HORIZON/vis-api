package gr.imsi.athenarc.xtremexpvisapi.domain;

import java.util.List;
import java.util.Map;

import tech.tablesaw.selection.Selection;

public class TabularResults {

    private String data;
    private List<String> fileNames;
    private List<VisualColumn> columns; // List to store column metadata
    private List<VisualColumn> originalColumns;
    
    

    private String timestampColumn; 
    private int totalItems; // New field for total item count
    private int querySize;
    private Map<String, List<Object>> uniqueColumnValues;  // Add this field to store unique values



    public Map<String, List<Object>> getUniqueColumnValues() {
        return uniqueColumnValues;
    }

    public void setUniqueColumnValues(Map<String, List<Object>> uniqueColumnValues) {
        this.uniqueColumnValues = uniqueColumnValues;
    }

    public int getQuerySize() {
        return querySize;
    }

    public void setQuerySize(int querySize) {
        this.querySize = querySize;
    }

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
    public List<VisualColumn> getOriginalColumns() {
        return originalColumns;
    }

    public void setOriginalColumns(List<VisualColumn> originalColumns) {
        this.originalColumns = originalColumns;
    }
    public TabularResults() {

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
