package gr.imsi.athenarc.xtremexpvisapi.domain.Explainability;

import java.util.List;

import lombok.Data;

@Data
public class Axis {
    private String axisName; // Corresponds to `string axis_name = 1;`
    private List<String> axisValues; // Corresponds to `repeated string axis_values = 2;`
    private String axisType; // Corresponds to `string axis_type = 3;`

}