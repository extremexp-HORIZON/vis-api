package gr.imsi.athenarc.xtremexpvisapi.domain.InitializeProcedure;

import gr.imsi.athenarc.xtremexpvisapi.domain.VisualizationDataRequest;

public class InitializationReq {
    private String modelName;
    private VisualizationDataRequest pipelineQuery;
    private VisualizationDataRequest modelInstancesQuery;
    private VisualizationDataRequest modelConfusionQuery;

    public String getModelName() {
        return modelName;
    }



    public void setModelName(String modelName) {
        this.modelName = modelName;
    }



    public VisualizationDataRequest getPipelineQuery() {
        return pipelineQuery;
    }



    public void setPipelineQuery(VisualizationDataRequest pipelineQuery) {
        this.pipelineQuery = pipelineQuery;
    }



    public VisualizationDataRequest getModelInstancesQuery() {
        return modelInstancesQuery;
    }



    public void setModelInstancesQuery(VisualizationDataRequest modelInstancesQuery) {
        this.modelInstancesQuery = modelInstancesQuery;
    }



    public VisualizationDataRequest getModelConfusionQuery() {
        return modelConfusionQuery;
    }



    public void setModelConfusionQuery(VisualizationDataRequest modelConfusionQuery) {
        this.modelConfusionQuery = modelConfusionQuery;
    }

        

    @Override
    public String toString() {
        return "InitializationReq [modelName=" + modelName + ", pipelineQuery=" + pipelineQuery
                + ", modelInstancesQuery=" + modelInstancesQuery + ", modelConfusionQuery=" + modelConfusionQuery + "]";
    }
    
    
}


