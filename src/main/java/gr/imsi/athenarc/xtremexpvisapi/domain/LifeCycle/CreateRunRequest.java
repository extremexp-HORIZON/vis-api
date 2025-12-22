package gr.imsi.athenarc.xtremexpvisapi.domain.lifecycle;

import lombok.Data;
import java.util.Map;

@Data
public class CreateRunRequest {
    private String experimentId;
    private String runName;
    private Map<String, String> params;
}
