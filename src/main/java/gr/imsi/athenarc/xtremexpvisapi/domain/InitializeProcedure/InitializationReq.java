package gr.imsi.athenarc.xtremexpvisapi.domain.InitializeProcedure;

import gr.imsi.athenarc.xtremexpvisapi.domain.Query.VisualQuery;

public class InitializationReq {
    private String modelName;
    private VisualQuery visualQuery;

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

    public VisualQuery getVisualQuery() {
        return visualQuery;
    }

    public void setVisualQuery(VisualQuery visualQuery) {
        this.visualQuery = visualQuery;
    }
}


