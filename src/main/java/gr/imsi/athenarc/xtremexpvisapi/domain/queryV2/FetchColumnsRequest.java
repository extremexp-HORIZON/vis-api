package gr.imsi.athenarc.xtremexpvisapi.domain.queryV2;

import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.Rectangle;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FetchColumnsRequest {
    private Rectangle rectangle;
    private String[] columnNames;
    private String latCol;
    private String lonCol;
}
