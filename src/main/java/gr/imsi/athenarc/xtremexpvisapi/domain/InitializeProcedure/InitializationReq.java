package gr.imsi.athenarc.xtremexpvisapi.domain.InitializeProcedure;

import gr.imsi.athenarc.xtremexpvisapi.domain.Query.VisualQuery;

public class InitializationReq {
    private String modelName;
    private VisualQuery pipelineQuery;
    private VisualQuery modelQuery;

    // Getters and Setters
    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    @Override
    public String toString() {
        return "InitializationRequest{" +
                "modelName='" + modelName + '\'' +
                '}';
    }

    public VisualQuery getPipelineQuery() {
        return pipelineQuery;
    }

    public void setPipelineQuery(VisualQuery pipelineQuery) {
        this.pipelineQuery = pipelineQuery;
    }

    public VisualQuery getModelQuery() {
        return modelQuery;
    }

    public void setModelQuery(VisualQuery modelQuery) {
        this.modelQuery = modelQuery;
    }
    
}


