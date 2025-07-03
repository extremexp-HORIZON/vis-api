package gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GroupedStats {

    private List<String> group;
    private Double value;

}
