package gr.imsi.athenarc.xtremexpvisapi.domain.Query;

import java.util.List;

import lombok.Data;

@Data
public class GroupedStats {

    private List<String> group;
    private Double value;

}
