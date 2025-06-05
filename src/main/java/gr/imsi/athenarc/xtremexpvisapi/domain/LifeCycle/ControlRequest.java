package gr.imsi.athenarc.xtremexpvisapi.domain.LifeCycle;

import lombok.Data;

@Data
public class ControlRequest {
    private String experimentId;
    private String runId;
    private String action; // "kill", "pause", "resume"
}
