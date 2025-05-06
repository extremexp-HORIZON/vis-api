package gr.imsi.athenarc.xtremexpvisapi.domain.Query;

import java.util.List;

import gr.imsi.athenarc.xtremexpvisapi.domain.QueryParams.TabularColumn;
import lombok.Data;

@Data
public class TabularResponse {

    private String data;
    private List<TabularColumn> columns; // List to store column metadata
    private int totalItems; // New field for total item count
    private int querySize;
}
