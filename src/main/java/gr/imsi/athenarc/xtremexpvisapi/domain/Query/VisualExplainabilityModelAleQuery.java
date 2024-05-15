package gr.imsi.athenarc.xtremexpvisapi.domain.Query;

import gr.imsi.athenarc.xtremexpvisapi.domain.VisualExplainability.Model.ModelAleParameters;

public class VisualExplainabilityModelAleQuery {
    String modelId;
    ModelAleParameters modelAleParameters;

    public VisualExplainabilityModelAleQuery(String modelId, ModelAleParameters modelAleParameters) {
        this.modelId = modelId;
        this.modelAleParameters = modelAleParameters;
    }

    public String getModelId() {
        return modelId;
    }

    public ModelAleParameters getModelAleParameters() {
        return modelAleParameters;
    }
    
}
