package gr.imsi.athenarc.xtremexpvisapi.domain.queryv2;

import gr.imsi.athenarc.xtremexpvisapi.domain.queryv2.params.Column;
import java.util.List;
import lombok.Data;

@Data
public class DataResponse {

  private String data;
  private List<Column> columns; // List to store column metadata
  private int totalItems; // New field for total item count
  private int querySize;
}
