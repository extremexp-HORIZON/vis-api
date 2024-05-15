package gr.imsi.athenarc.xtremexpvisapi.domain.Query;

import gr.imsi.athenarc.xtremexpvisapi.domain.VisualExplainability.Model.Model2DPdpParameters;

public class VisualExplainabilityModel2DPdpQuery {
    String modelId;
    Model2DPdpParameters modelPdpParameters;

    public VisualExplainabilityModel2DPdpQuery(String modelId, Model2DPdpParameters modelPdpParameters) {
        this.modelId = modelId;
        this.modelPdpParameters = modelPdpParameters;
    }

    public String getModelId() {
        return modelId;
    }

    public Model2DPdpParameters getModel2DPdpParameters() {
        return modelPdpParameters;
    }
    
}
