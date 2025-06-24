package gr.imsi.athenarc.xtremexpvisapi.domain.lifecycle;

import lombok.Data;

@Data
public class ControlRequest {
    private String experimentId;
    private String runId;
    private String action; // "kill", "pause", "resume"
}
