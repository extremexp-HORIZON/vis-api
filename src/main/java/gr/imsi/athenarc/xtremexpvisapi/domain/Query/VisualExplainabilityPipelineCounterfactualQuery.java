package gr.imsi.athenarc.xtremexpvisapi.domain.Query;

import gr.imsi.athenarc.xtremexpvisapi.domain.VisualExplainability.Pipeline.PipelineCounterfactualParameters;

public class VisualExplainabilityPipelineCounterfactualQuery {
    String modelId;
    PipelineCounterfactualParameters pipelineCounterfactualParameters;

    public VisualExplainabilityPipelineCounterfactualQuery(String modelId, PipelineCounterfactualParameters pipelineCounterfactualParameters) {
        this.modelId = modelId;
        this.pipelineCounterfactualParameters = pipelineCounterfactualParameters;
    }

    public String getModelId() {
        return modelId;
    }

    public PipelineCounterfactualParameters getPipelineCounterfactualParameters() {
        return pipelineCounterfactualParameters;
    }
    
}
