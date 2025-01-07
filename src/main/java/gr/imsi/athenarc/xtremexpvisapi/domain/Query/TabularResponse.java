package gr.imsi.athenarc.xtremexpvisapi.domain.Query;

import java.util.List;
import java.util.Map;

import gr.imsi.athenarc.xtremexpvisapi.domain.QueryParams.TabularColumn;
import lombok.Data;

@Data
public class TabularResponse {

    private String data;
    private List<String> fileNames;
    private List<TabularColumn> columns; // List to store column metadata
    private List<TabularColumn> originalColumns; // List to store original column metadata
    private int totalItems; // New field for total item count
    private int querySize; 
    private Map<String, List<?>> uniqueColumnValues;  // Add this field to store unique values

}
