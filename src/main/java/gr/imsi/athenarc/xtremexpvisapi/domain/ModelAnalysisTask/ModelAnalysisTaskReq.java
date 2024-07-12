package gr.imsi.athenarc.xtremexpvisapi.domain.ModelAnalysisTask;

public class ModelAnalysisTaskReq {
    String ModelName;
    Integer ModelId;

    public String getModelName() {
        return ModelName;
    }

    public void setModelName(String modelName) {
        ModelName = modelName;
    }

    public Integer getModelId() {
        return ModelId;
    }

    public void setModelId(Integer modelId) {
        ModelId = modelId;
    }

}
