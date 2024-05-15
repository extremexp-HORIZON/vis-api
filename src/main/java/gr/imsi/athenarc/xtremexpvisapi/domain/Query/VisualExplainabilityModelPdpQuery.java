package gr.imsi.athenarc.xtremexpvisapi.domain.Query;

import gr.imsi.athenarc.xtremexpvisapi.domain.VisualExplainability.Model.ModelPdpParameters;

public class VisualExplainabilityModelPdpQuery {
    String modelId;
    ModelPdpParameters modelPdpParameters;

    public VisualExplainabilityModelPdpQuery(String modelId, ModelPdpParameters modelPdpParameters) {
        this.modelId = modelId;
        this.modelPdpParameters = modelPdpParameters;
    }

    public String getModelId() {
        return modelId;
    }

    public ModelPdpParameters getModelPdpParameters() {
        return modelPdpParameters;
    }
    
}
