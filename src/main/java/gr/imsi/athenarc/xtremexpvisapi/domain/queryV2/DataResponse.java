package gr.imsi.athenarc.xtremexpvisapi.domain.queryV2;

import java.util.List;

import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.Column;
import lombok.Data;

@Data
public class DataResponse {

    private String data;
    private List<Column> columns; // List to store column metadata
    private int totalItems; // New field for total item count
    private int querySize;
}
