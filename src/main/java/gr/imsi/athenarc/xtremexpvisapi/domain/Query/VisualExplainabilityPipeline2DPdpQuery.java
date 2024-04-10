package gr.imsi.athenarc.xtremexpvisapi.domain.Query;

import gr.imsi.athenarc.xtremexpvisapi.domain.VisualExplainability.Pipeline.Pipeline2DPdpParameters;

public class VisualExplainabilityPipeline2DPdpQuery {
    String modelId;
    Pipeline2DPdpParameters pipelinePdpParameters;

    public VisualExplainabilityPipeline2DPdpQuery(String modelId, Pipeline2DPdpParameters pipelinePdpParameters) {
        this.modelId = modelId;
        this.pipelinePdpParameters = pipelinePdpParameters;
    }

    public String getModelId() {
        return modelId;
    }

    public Pipeline2DPdpParameters getPipelinePdpParameters() {
        return pipelinePdpParameters;
    }
    
}
