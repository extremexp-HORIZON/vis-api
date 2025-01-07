package gr.imsi.athenarc.xtremexpvisapi.domain.Query;

import lombok.AllArgsConstructor;
import lombok.Data;
import tech.tablesaw.api.Table;

@Data
@AllArgsConstructor
public class QueryResult {
    private Table resultTable;
    private int rowCount;
}
