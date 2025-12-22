package gr.imsi.athenarc.xtremexpvisapi.domain.kubeflow;

import java.util.List;

import lombok.Data;

@Data
public class KfpPipelineResponse {
    public List<KfpPipeline> pipelines;
}

