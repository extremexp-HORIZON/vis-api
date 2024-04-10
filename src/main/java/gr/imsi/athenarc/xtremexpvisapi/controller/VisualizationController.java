package gr.imsi.athenarc.xtremexpvisapi.controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gr.imsi.athenarc.xtremexpvisapi.domain.VisualColumn;
import gr.imsi.athenarc.xtremexpvisapi.domain.VisualExplainabilityRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.VisualExplainabilityResults;
import gr.imsi.athenarc.xtremexpvisapi.domain.VisualizationDataRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.VisualizationResults;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.VisualExplainabilityPipeline2DPdpQuery;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.VisualExplainabilityPipelinePdpQuery;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.VisualQuery;
import gr.imsi.athenarc.xtremexpvisapi.domain.VisualExplainability.Pipeline.Pipeline2DPdpParameters;
import gr.imsi.athenarc.xtremexpvisapi.domain.VisualExplainability.Pipeline.PipelinePdpParameters;
import gr.imsi.athenarc.xtremexpvisapi.service.DataService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class VisualizationController {
 
    private static final Logger LOG = LoggerFactory.getLogger(VisualizationController.class);
    
    private final DataService dataService;


    public VisualizationController(DataService dataService){
        this.dataService = dataService;
    }

    @PostMapping("/visualization/data/{datasetId}")
    public ResponseEntity<VisualizationResults> data(@PathVariable String datasetId, @Valid @RequestBody VisualizationDataRequest visualizationDataRequest) {
        LOG.info("Request for visualization data {}", visualizationDataRequest);
        VisualizationResults visualizationResults = new VisualizationResults();
        VisualQuery visualQuery = new VisualQuery(datasetId, visualizationDataRequest.getViewPort(), visualizationDataRequest.getColumns(), visualizationDataRequest.getLimit());
        visualQuery.instantiateFilters(visualizationDataRequest.getFilters(), dataService.getColumns(datasetId)); // TODO: Cache dataset
        try {
            visualizationResults = dataService.getData(visualQuery);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
        if(visualizationResults.getMessage().equals("400"))  return ResponseEntity.badRequest().body(visualizationResults);
        return ResponseEntity.ok(visualizationResults);
    }

    @GetMapping("/visualization/data/{datasetId}/columns")
    public ResponseEntity<List<VisualColumn>> getColumns(@PathVariable String datasetId) {
        return ResponseEntity.ok(dataService.getColumns(datasetId));
    }

    @PostMapping("/visualization/explainability/{datasetId}")
    public ResponseEntity<VisualExplainabilityResults> explainability(@PathVariable String datasetId, @Valid @RequestBody VisualExplainabilityRequest visualExplainabilityRequest) {
        String explainabilityType = visualExplainabilityRequest.getExplainabilityType().toLowerCase();
        String explainabilityMethod = visualExplainabilityRequest.getExplainabilityMethod().toLowerCase();
        String modelId = visualExplainabilityRequest.getModelId();
        VisualExplainabilityResults visualExplainabilityResults = new VisualExplainabilityResults();
        if(explainabilityType.equals("pipeline")) {
            if(explainabilityMethod.equals("pdp")) {
                Map<String, String> rawPipelinePdpParameters = null;
                try {
                    rawPipelinePdpParameters = (Map<String, String>) visualExplainabilityRequest.getAdditionalParams();
                } catch(ClassCastException e){
                    LOG.error("Error: additionalParams must be a dictionary of 1 feature {feature: \"\"}");
                    e.printStackTrace();
                }
                String feature = rawPipelinePdpParameters.get("feature");
                PipelinePdpParameters pipelinePdpParameters = new PipelinePdpParameters(feature);
                VisualExplainabilityPipelinePdpQuery visualExplainabilityPipelinePdpQuery = new VisualExplainabilityPipelinePdpQuery(modelId, pipelinePdpParameters);
                visualExplainabilityResults = dataService.getPipelineExplainabilityPdpData(visualExplainabilityPipelinePdpQuery);
            }
            else if(explainabilityMethod.equals("pdp2d")){
                Map<String, String> rawPipeline2DPdpParameters = null;
                try {
                    rawPipeline2DPdpParameters = (Map<String, String>) visualExplainabilityRequest.getAdditionalParams();
                } 
                catch(ClassCastException e){
                    LOG.error("Error: additionalParams must be a dictionary of 2 features {feature1: \"\", feauture2: \"\"}");
                    e.printStackTrace();
                }
                String feature1 = rawPipeline2DPdpParameters.get("feature1");
                String feature2 = rawPipeline2DPdpParameters.get("feature2");
                Pipeline2DPdpParameters pipeline2DPdpParameters = new Pipeline2DPdpParameters(feature1, feature2);
                VisualExplainabilityPipeline2DPdpQuery visualExplainabilityPipeline2DPdpQuery = new VisualExplainabilityPipeline2DPdpQuery(modelId, pipeline2DPdpParameters);
                visualExplainabilityResults = dataService.getPipelineExplainability2DPdpData(visualExplainabilityPipeline2DPdpQuery);
            }
            else {

            }
        }
        else if(explainabilityType.equals("model")){

        }
        else{

        }
        if(visualExplainabilityResults.getMessage().equals("400"))  return ResponseEntity.badRequest().body(visualExplainabilityResults);
        return ResponseEntity.ok(visualExplainabilityResults);
    }
}
