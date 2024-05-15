package gr.imsi.athenarc.xtremexpvisapi.controller;

import java.util.List;
import java.util.Map;

import org.apache.commons.math3.analysis.function.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
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
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.VisualExplainabilityModel2DPdpQuery;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.VisualExplainabilityModelAleQuery;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.VisualExplainabilityModelPdpQuery;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.VisualExplainabilityPipeline2DPdpQuery;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.VisualExplainabilityPipelineAleQuery;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.VisualExplainabilityPipelineCounterfactualQuery;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.VisualExplainabilityPipelineInfluenceQuery;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.VisualExplainabilityPipelinePdpQuery;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.VisualQuery;
import gr.imsi.athenarc.xtremexpvisapi.domain.VisualExplainability.Model.Model2DPdpParameters;
import gr.imsi.athenarc.xtremexpvisapi.domain.VisualExplainability.Model.ModelAleParameters;
import gr.imsi.athenarc.xtremexpvisapi.domain.VisualExplainability.Model.ModelPdpParameters;
import gr.imsi.athenarc.xtremexpvisapi.domain.VisualExplainability.Pipeline.Pipeline2DPdpParameters;
import gr.imsi.athenarc.xtremexpvisapi.domain.VisualExplainability.Pipeline.PipelineAleParameters;
import gr.imsi.athenarc.xtremexpvisapi.domain.VisualExplainability.Pipeline.PipelineCounterfactualParameters;
import gr.imsi.athenarc.xtremexpvisapi.domain.VisualExplainability.Pipeline.PipelineInfluenceParameters;
import gr.imsi.athenarc.xtremexpvisapi.domain.VisualExplainability.Pipeline.PipelinePdpParameters;
import gr.imsi.athenarc.xtremexpvisapi.service.DataService;
import jakarta.validation.Valid;

@RestController
@CrossOrigin
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
        
        VisualQuery visualQuery = new VisualQuery(
            datasetId,
         visualizationDataRequest.getViewPort(), 
         visualizationDataRequest.getColumns(),
          visualizationDataRequest.getLimit()
          );

        visualQuery.instantiateFilters(
            visualizationDataRequest.getFilters(),
         dataService.getColumns(datasetId)
         ); // TODO: Cache dataset
        try {
            visualizationResults = dataService.getData(visualQuery);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
        if(visualizationResults.getMessage().equals("400"))  return ResponseEntity.badRequest().body(visualizationResults);
        LOG.info("Visualization data retrieval successful");
    
        return ResponseEntity.ok(visualizationResults);
    }

    @GetMapping("/visualization/data/{datasetId}/columns")
    public ResponseEntity<List<VisualColumn>> getColumns(@PathVariable String datasetId) {

        try {
            // Retrieve columns for the specified datasetId
            List<VisualColumn> columns = dataService.getColumns(datasetId);

            // Log successful retrieval of columns
            LOG.info("Retrieved columns for datasetId {}: {}", datasetId, columns);

            // Return a successful response with the columns
            return ResponseEntity.ok(columns);
        } catch (Exception e) {
            // Log the error and return an internal server error response
            LOG.error("Error retrieving columns for datasetId {}", datasetId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/visualization/data/{datasetId}/column/{columnName}")
    public ResponseEntity<String> getColumn(@PathVariable String datasetId, @PathVariable String columnName) {
    
        try {
            // Retrieve the specified column value for the datasetId and columnName
            String columnValue = dataService.getColumn(datasetId, columnName);

            // Log successful retrieval of column value
            LOG.info("Retrieved column '{}' value for datasetId {}: {}", columnName, datasetId, columnValue);

            // Return a successful response with the column value
            return ResponseEntity.ok(columnValue);
        } catch (Exception e) {
            // Log the error and return an internal server error response
            LOG.error("Error retrieving column '{}' value for datasetId {}", columnName, datasetId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/visualization/explainability/{datasetId}")
    public ResponseEntity<VisualExplainabilityResults> explainability(@PathVariable String datasetId, @Valid @RequestBody VisualExplainabilityRequest visualExplainabilityRequest) {
        String explainabilityType = visualExplainabilityRequest.getExplainabilityType().toLowerCase();
        String explainabilityMethod = visualExplainabilityRequest.getExplainabilityMethod().toLowerCase();
        String modelId = visualExplainabilityRequest.getModelId();
        LOG.info("Explainability type: {}, Explainability method: {}, Model ID: {}", explainabilityType, explainabilityMethod, modelId);


        
        VisualExplainabilityResults visualExplainabilityResults = new VisualExplainabilityResults();
        if(explainabilityType.equals("pipeline")) {
            if(explainabilityMethod.equals("pdp")) {
                LOG.info("Performing Pipeline PDP explainability analysis");
                Map<String, String> rawPipelinePdpParameters = null;
                try {
                    rawPipelinePdpParameters = (Map<String, String>) visualExplainabilityRequest.getAdditionalParams();
                } catch(ClassCastException e){
                    LOG.error("Error: additionalParams must be a dictionary of 1 feature {feature: \"\"}");
                    e.printStackTrace();
                }
                String feature = rawPipelinePdpParameters.get("feature1");
                PipelinePdpParameters pipelinePdpParameters = new PipelinePdpParameters(feature);
                VisualExplainabilityPipelinePdpQuery visualExplainabilityPipelinePdpQuery = new VisualExplainabilityPipelinePdpQuery(modelId, pipelinePdpParameters);
                visualExplainabilityResults = dataService.getPipelineExplainabilityPdpData(visualExplainabilityPipelinePdpQuery);

            }
            else if(explainabilityMethod.equals("pdp2d")){
                LOG.info("Performing Pipeline 2D PDP explainability analysis");
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

            }else if(explainabilityMethod.equals("ale")) {
                LOG.info("Performing Pipeline Ale explainability analysis");
                Map<String, String> rawPipelineAleParameters = null;
                try {
                    rawPipelineAleParameters = (Map<String, String>) visualExplainabilityRequest.getAdditionalParams();
                } catch(ClassCastException e){
                    LOG.error("Error: additionalParams must be a dictionary of 1 feature {feature: \"\"}");
                    e.printStackTrace();
                }
                String feature = rawPipelineAleParameters.get("feature1");
                PipelineAleParameters pipelineAleParameters = new PipelineAleParameters(feature);
                VisualExplainabilityPipelineAleQuery visualExplainabilityPipelinePdpQuery = new VisualExplainabilityPipelineAleQuery(modelId, pipelineAleParameters);
                visualExplainabilityResults = dataService.getPipelineExplainabilityAleData(visualExplainabilityPipelinePdpQuery);

            }
            else if (explainabilityMethod.equals("counterfactual")){
                LOG.info("Performing Pipeline Counterfactual explainability analysis");
                Map<String, String> rawPipelineCounterfactualParameters = null;
                try {
                    rawPipelineCounterfactualParameters = (Map<String, String>) visualExplainabilityRequest.getAdditionalParams();
                } catch(ClassCastException e){
                    LOG.error("Error: additionalParams must be a dictionary of 1 feature {feature: \"\"}");
                    e.printStackTrace();
                }
                String feature = rawPipelineCounterfactualParameters.get("feature1");
                PipelineCounterfactualParameters pipelineCounterfactualParameters = new PipelineCounterfactualParameters(feature);
                VisualExplainabilityPipelineCounterfactualQuery visualExplainabilityPipelineCounterfactualQuery = new VisualExplainabilityPipelineCounterfactualQuery(modelId, pipelineCounterfactualParameters);
                visualExplainabilityResults = dataService.getPipelineExplainabilityCounterFactualData(visualExplainabilityPipelineCounterfactualQuery);

            }
            else if (explainabilityMethod.equals("influence")){
                LOG.info("Performing Pipeline Influence explainability analysis");
                Map<String, String> rawPipelineInfluenceParameters = null;
                try {
                    rawPipelineInfluenceParameters = (Map<String, String>) visualExplainabilityRequest.getAdditionalParams();
                } catch(ClassCastException e){
                    LOG.error("Error: additionalParams must be a dictionary of 1 feature {feature: \"\"}");
                    e.printStackTrace();
                }
                Integer noOfInfluential = Integer.parseInt(rawPipelineInfluenceParameters.get("noOfInfluential"));
                PipelineInfluenceParameters pipelineInfluenceParameters = new PipelineInfluenceParameters(noOfInfluential);
                VisualExplainabilityPipelineInfluenceQuery visualExplainabilityPipelinePdpQuery = new VisualExplainabilityPipelineInfluenceQuery(modelId, pipelineInfluenceParameters);
                visualExplainabilityResults = dataService.getPipelineExplainabilityInfluenceData(visualExplainabilityPipelinePdpQuery);
            }
        }
        else if(explainabilityType.equals("model")){
            LOG.info("Performing model-level explainability analysis");
            if(explainabilityMethod.equals("pdp")) {
                LOG.info("Performing Model PDP explainability analysis");

                Map<String, String> rawModelPdpParameters = null;
                try {
                    rawModelPdpParameters = (Map<String, String>) visualExplainabilityRequest.getAdditionalParams();
                } catch(ClassCastException e){
                    LOG.error("Error: additionalParams must be a dictionary of 1 feature {feature: \"\"}");
                    e.printStackTrace();
                }
                String feature = rawModelPdpParameters.get("feature1");
                ModelPdpParameters modelPdpParameters = new ModelPdpParameters(feature);
                VisualExplainabilityModelPdpQuery visualExplainabilityModelPdpQuery = new VisualExplainabilityModelPdpQuery(modelId, modelPdpParameters);
                visualExplainabilityResults = dataService.getModelExplainabilityPdpData(visualExplainabilityModelPdpQuery);
            }
            else if(explainabilityMethod.equals("pdp2d")){
                LOG.info("Performing Model 2D PDP explainability analysis");
                Map<String, String> rawModel2DPdpParameters = null;
                try {
                    rawModel2DPdpParameters = (Map<String, String>) visualExplainabilityRequest.getAdditionalParams();
                } 
                catch(ClassCastException e){
                    LOG.error("Error: additionalParams must be a dictionary of 2 features {feature1: \"\", feauture2: \"\"}");
                    e.printStackTrace();
                }
                String feature1 = rawModel2DPdpParameters.get("feature1");
                String feature2 = rawModel2DPdpParameters.get("feature2");
                Model2DPdpParameters model2dPdpParameters = new Model2DPdpParameters(feature1, feature2);
                VisualExplainabilityModel2DPdpQuery visualExplainabilityModel2DPdpQuery = new VisualExplainabilityModel2DPdpQuery(modelId, model2dPdpParameters);
                visualExplainabilityResults = dataService.getModelExplainability2DPdpData(visualExplainabilityModel2DPdpQuery);
            }
            else if(explainabilityMethod.equals("counterfactual")){
                LOG.info("Performing Model Counterfactual explainability analysis-Not implemented yet");

            }
            else if(explainabilityMethod.equals("ale")) {
                LOG.info("Performing Model Ale explainability analysis");
                Map<String, String> rawModelAleParameters = null;
                try {
                    rawModelAleParameters = (Map<String, String>) visualExplainabilityRequest.getAdditionalParams();
                } catch(ClassCastException e){
                    LOG.error("Error: additionalParams must be a dictionary of 1 feature {feature: \"\"}");
                    e.printStackTrace();
                }
                String feature = rawModelAleParameters.get("feature1");
                ModelAleParameters modelAleParameters = new ModelAleParameters(feature);
                VisualExplainabilityModelAleQuery visualExplainabilityPipelinePdpQuery = new VisualExplainabilityModelAleQuery(modelId, modelAleParameters);
                visualExplainabilityResults = dataService.getModelExplainabilityAleData(visualExplainabilityPipelinePdpQuery);
            }
        }
        else{
            LOG.error("Unsupported explainability type: {}", explainabilityType);
        }
        if (visualExplainabilityResults.getMessage().equals("400")) {
            LOG.warn("Bad request: {}", visualExplainabilityResults.getMessage());
            return ResponseEntity.badRequest().body(visualExplainabilityResults);
        }
        LOG.info("Explainability analysis successful");
        return ResponseEntity.ok(visualExplainabilityResults);

    }
}
