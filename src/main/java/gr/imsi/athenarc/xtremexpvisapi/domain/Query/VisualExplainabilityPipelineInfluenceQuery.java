package gr.imsi.athenarc.xtremexpvisapi.domain.Query;

import gr.imsi.athenarc.xtremexpvisapi.domain.VisualExplainability.Pipeline.PipelineInfluenceParameters;

public class VisualExplainabilityPipelineInfluenceQuery {
    String modelId;
    PipelineInfluenceParameters pipelineInfluenceParameters;

    public VisualExplainabilityPipelineInfluenceQuery(String modelId, PipelineInfluenceParameters pipelineInfluenceParameters) {
        this.modelId = modelId;
        this.pipelineInfluenceParameters = pipelineInfluenceParameters;
    }

    public String getModelId() {
        return modelId;
    }

    public PipelineInfluenceParameters getPipelineInfluenceParameters() {
        return pipelineInfluenceParameters;
    }
    
}
