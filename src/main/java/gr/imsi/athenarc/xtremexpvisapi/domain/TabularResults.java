package gr.imsi.athenarc.xtremexpvisapi.domain;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class TabularResults {

    private String data;
    private List<String> fileNames;
    private List<TabularColumn> columns; // List to store column metadata
    private List<TabularColumn> originalColumns;
    
    private int totalItems; // New field for total item count
    private int querySize;
    private Map<String, List<Object>> uniqueColumnValues;  // Add this field to store unique values

}
