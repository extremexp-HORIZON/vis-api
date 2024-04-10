package gr.imsi.athenarc.xtremexpvisapi.domain;

import java.util.Map;


public class VisualExplainabilityRequest {

    private String modelId;
    private String explainabilityType;
    private String explainabilityMethod;
    private String visualizationType;

    private Map<String, Object> constraints;
    private Object additionalParams;

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getExplainabilityType() {
        return explainabilityType;
    }

    public void setExplainabilityType(String explainabilityType) {
        this.explainabilityType = explainabilityType;
    }

    public String getExplainabilityMethod() {
        return explainabilityMethod;
    }

    public void setExplainabilityMethod(String explainabilityMethod) {
        this.explainabilityMethod = explainabilityMethod;
    }

    public String getVisualizationType() {
        return visualizationType;
    }

    public void setVisualizationType(String visualizationType) {
        this.visualizationType = visualizationType;
    }


    public Map<String, Object> getConstraints() {
        return constraints;
    }

    public void setConstraints(Map<String, Object> constraints) {
        this.constraints = constraints;
    }

    public Object getAdditionalParams() {
        return additionalParams;
    }

    public void setAdditionalParams(Object additionalParams) {
        this.additionalParams = additionalParams;
    }

    @Override
    public String toString() {
        return "VisualExplainabilityRequest [additionalParams=" + additionalParams + ", constraints=" + constraints
                + ", explainabilityMethod=" + explainabilityMethod + ", explainabilityType=" + explainabilityType
                + ", modelId=" + modelId + ", visualizationType=" + visualizationType + "]";
    }

    
   
}
