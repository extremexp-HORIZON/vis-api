package gr.imsi.athenarc.xtremexpvisapi.domain.InitializeProcedure;

import gr.imsi.athenarc.xtremexpvisapi.domain.VisualizationDataRequest;
import lombok.Data;

@Data
public class InitializationReq {
    private String modelName;
    private VisualizationDataRequest pipelineQuery;
    private VisualizationDataRequest modelInstancesQuery;
    private VisualizationDataRequest modelConfusionQuery;
    
}


