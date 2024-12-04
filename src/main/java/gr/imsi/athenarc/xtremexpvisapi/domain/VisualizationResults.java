package gr.imsi.athenarc.xtremexpvisapi.domain;

import java.util.List;

import lombok.Data;

@Data
public class VisualizationResults {

    private String data;
    private List<String> fileNames;
    private List<TabularColumn> columns; // List to store column metadata
    private TabularColumn timestampColumn; 
    private int totalItems; // New field for total item count
}
