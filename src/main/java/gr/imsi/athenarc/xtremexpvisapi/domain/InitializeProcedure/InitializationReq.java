package gr.imsi.athenarc.xtremexpvisapi.domain.InitializeProcedure;

import gr.imsi.athenarc.xtremexpvisapi.domain.Query.VisualQuery;

public class InitializationReq {
    private String modelName;
    private VisualQuery pipelineQuery;
    private VisualQuery modelInstancesQuery;
    private VisualQuery modelConfusionQuery;

    public VisualQuery getPipelineQuery() {
        return pipelineQuery;
    }

    public void setPipelineQuery(VisualQuery pipelineQuery) {
        this.pipelineQuery = pipelineQuery;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public VisualQuery getModelInstancesQuery() {
        return modelInstancesQuery;
    }

    public void setModelInstancesQuery(VisualQuery modelInstancesQuery) {
        this.modelInstancesQuery = modelInstancesQuery;
    }

    public VisualQuery getModelConfusionQuery() {
        return modelConfusionQuery;
    }

    public void setModelConfusionQuery(VisualQuery modelConfusionQuery) {
        this.modelConfusionQuery = modelConfusionQuery;
    }

    @Override
    public String toString() {
        return "InitializationReq [modelName=" + modelName + ", pipelineQuery=" + pipelineQuery
                + ", modelInstancesQuery=" + modelInstancesQuery + ", modelConfusionQuery=" + modelConfusionQuery + "]";
    }
    
    
}


