package gr.imsi.athenarc.xtremexpvisapi.domain.Query;

import gr.imsi.athenarc.xtremexpvisapi.domain.VisualExplainability.Pipeline.PipelinePdpParameters;

public class VisualExplainabilityPipelinePdpQuery {
    String modelId;
    PipelinePdpParameters pipelinePdpParameters;

    public VisualExplainabilityPipelinePdpQuery(String modelId, PipelinePdpParameters pipelinePdpParameters) {
        this.modelId = modelId;
        this.pipelinePdpParameters = pipelinePdpParameters;
    }

    public String getModelId() {
        return modelId;
    }

    public PipelinePdpParameters getPipelinePdpParameters() {
        return pipelinePdpParameters;
    }
    
}
