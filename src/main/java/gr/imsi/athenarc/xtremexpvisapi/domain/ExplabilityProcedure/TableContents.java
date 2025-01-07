package gr.imsi.athenarc.xtremexpvisapi.domain.ExplabilityProcedure;

import java.util.List;

import lombok.Data;

@Data
public class TableContents {
    private int index;
    private List<String> values;   
}