package gr.imsi.athenarc.xtremexpvisapi.service;


import java.nio.file.Path;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import gr.imsi.athenarc.xtremexpvisapi.datasource.QueryExecutor;
import gr.imsi.athenarc.xtremexpvisapi.domain.VisualColumn;
import gr.imsi.athenarc.xtremexpvisapi.domain.VisualExplainabilityResults;
import gr.imsi.athenarc.xtremexpvisapi.domain.VisualizationResults;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.VisualExplainabilityPipeline2DPdpQuery;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.VisualExplainabilityPipelinePdpQuery;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.VisualExplainabilityPipelinePdpQuery;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.VisualQuery;

@Service
public class DataService {

    @Value("${app.schema.path}")
    String schemaPath = "";
    

    public VisualizationResults getData(VisualQuery visualQuery) {
        VisualizationResults visualizationResults = new VisualizationResults();
        String datasetId = visualQuery.getDatasetId();
        if(visualQuery.getFilters().contains(null)){
            visualizationResults.setMessage("500");
            return visualizationResults;
        }
        QueryExecutor queryExecutor = new QueryExecutor(datasetId, Path.of(schemaPath, datasetId + ".csv").toString());
        return queryExecutor.executeQuery(visualQuery);    
    }

    public List<VisualColumn> getColumns(String datasetId) {
        QueryExecutor queryExecutor = new QueryExecutor(datasetId, Path.of(schemaPath, datasetId + ".csv").toString());
        return queryExecutor.getColumns(datasetId);    
    }


    public VisualExplainabilityResults getPipelineExplainabilityPdpData(VisualExplainabilityPipelinePdpQuery visualExplainabilityQuery) {
        VisualExplainabilityResults visualExplainabilityResults = new VisualExplainabilityResults();
        String feature = visualExplainabilityQuery.getPipelinePdpParameters().getFeature();
        if(feature == null){
            visualExplainabilityResults.setMessage("400");
        }
        else visualExplainabilityResults.setMessage("200");
        return visualExplainabilityResults;
    }
    public VisualExplainabilityResults getPipelineExplainability2DPdpData(VisualExplainabilityPipeline2DPdpQuery visualExplainabilityQuery) {
        VisualExplainabilityResults visualExplainabilityResults = new VisualExplainabilityResults();
        String feature1 = visualExplainabilityQuery.getPipelinePdpParameters().getFeature1();
        String feature2 = visualExplainabilityQuery.getPipelinePdpParameters().getFeature2();
        if(feature1 == null || feature2 == null){
            visualExplainabilityResults.setMessage("400");
        }
        else visualExplainabilityResults.setMessage("200");
        return visualExplainabilityResults;
    }

    
}