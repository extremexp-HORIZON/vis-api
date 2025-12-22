package gr.imsi.athenarc.xtremexpvisapi.domain.lifecycle;

import lombok.Data;

@Data
public class CreateRunResponse {
    private String message;
    private String kfpRunId;
    private String runName;
}
