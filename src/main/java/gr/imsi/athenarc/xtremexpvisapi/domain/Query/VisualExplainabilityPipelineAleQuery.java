package gr.imsi.athenarc.xtremexpvisapi.domain.Query;

import gr.imsi.athenarc.xtremexpvisapi.domain.VisualExplainability.Pipeline.PipelineAleParameters;

public class VisualExplainabilityPipelineAleQuery {
    String modelId;
    PipelineAleParameters pipelineAleParameters;

    public VisualExplainabilityPipelineAleQuery(String modelId, PipelineAleParameters pipelineAleParameters) {
        this.modelId = modelId;
        this.pipelineAleParameters = pipelineAleParameters;
    }

    public String getModelId() {
        return modelId;
    }

    public PipelineAleParameters getPipelineAleParameters() {
        return pipelineAleParameters;
    }
    
}
