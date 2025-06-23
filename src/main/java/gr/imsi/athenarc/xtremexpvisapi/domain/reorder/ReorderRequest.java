package gr.imsi.athenarc.xtremexpvisapi.domain.reorder;

import lombok.Data;

@Data
public class ReorderRequest {
    private String experimentId;
    private String precedingWorkflowId;
    private String workflowId;
}
