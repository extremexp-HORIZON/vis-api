package gr.imsi.athenarc.xtremexpvisapi.domain.queryv1;

import java.util.List;

import gr.imsi.athenarc.xtremexpvisapi.domain.queryv1.params.TabularColumn;
import lombok.Data;

@Data
public class TabularResponse {

    private String data;
    private List<TabularColumn> columns; // List to store column metadata
    private int totalItems; // New field for total item count
    private int querySize;
}
