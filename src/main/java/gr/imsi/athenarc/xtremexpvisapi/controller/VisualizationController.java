package gr.imsi.athenarc.xtremexpvisapi.controller;

import java.util.List;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.protobuf.InvalidProtocolBufferException;

import gr.imsi.athenarc.xtremexpvisapi.domain.TabularRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.TabularResults;
import gr.imsi.athenarc.xtremexpvisapi.domain.TimeSeriesRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.TimeSeriesResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.VisualColumn;
import gr.imsi.athenarc.xtremexpvisapi.domain.VisualizationDataRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.VisualizationResults;
import gr.imsi.athenarc.xtremexpvisapi.domain.ExplabilityProcedure.ExplanationsReq;
import gr.imsi.athenarc.xtremexpvisapi.domain.ExplabilityProcedure.ExplanationsRes;
import gr.imsi.athenarc.xtremexpvisapi.domain.InitializeProcedure.FeatureExplanation;
import gr.imsi.athenarc.xtremexpvisapi.domain.InitializeProcedure.InitializationReq;
import gr.imsi.athenarc.xtremexpvisapi.domain.InitializeProcedure.InitializationRes;
import gr.imsi.athenarc.xtremexpvisapi.domain.ModelAnalysisTask.ModelAnalysisTaskReq;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.VisualQuery;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TabularQuery;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TimeSeriesQuery;
import gr.imsi.athenarc.xtremexpvisapi.service.DataService;
import gr.imsi.athenarc.xtremexpvisapi.service.ExplainabilityService;

import jakarta.validation.Valid;

@RestController
@CrossOrigin
@RequestMapping("/api")
public class VisualizationController {
 
    private static final Logger LOG = LoggerFactory.getLogger(VisualizationController.class);
    
    private final DataService dataService;
    private final ExplainabilityService explainabilityService;

    public VisualizationController(DataService dataService, ExplainabilityService explainabilityService) {
        this.dataService = dataService;
        this.explainabilityService = explainabilityService;
    }
    
    @PostMapping("/initialization")
    public ResponseEntity<InitializationRes> data(@RequestBody InitializationReq request) throws JsonProcessingException, InvalidProtocolBufferException {
        LOG.info("Request for explainability initialization for dataset {}", request.getModelName());
        return ResponseEntity.ok(explainabilityService.GetInitialization(request));
    
    }

    @PostMapping("/task/modelAnalysis")
    public ResponseEntity<FeatureExplanation> getModelAnalysisTask(@RequestBody ModelAnalysisTaskReq request) throws JsonProcessingException, InvalidProtocolBufferException {
        LOG.info("Request for model Analysis model_name: {} model_id {}", request.getModelName(),request.getModelId());
        return ResponseEntity.ok(explainabilityService.GetModelAnalysisTask(request));
    }

    @PostMapping("/explainability")
    public ResponseEntity<ExplanationsRes> data(@RequestBody ExplanationsReq request) throws JsonProcessingException, InvalidProtocolBufferException {
        LOG.info("Request for explainability {}{}{}{}", request.getExplanationType(),request.getExplanationMethod(),request.getFeature1(), request.getModel());
        return ResponseEntity.ok(explainabilityService.GetExplains(request));
    }


    @PostMapping("/visualization/timeseries")
    public ResponseEntity<TimeSeriesResponse> getTimeSeriesData(@Valid @RequestBody TimeSeriesRequest timeSeriesRequest) {
        LOG.info("Request for visualization data {}", timeSeriesRequest);

        if (timeSeriesRequest.getDatasetId() == null) {
            LOG.error("Dataset ID is missing");
            return ResponseEntity.badRequest().body(new TimeSeriesResponse());
        }
        TimeSeriesResponse timeSeriesResponse = new TimeSeriesResponse();
        
        TimeSeriesQuery timeSeriesQuery = dataService.timeSeriesQueryPreperation(timeSeriesRequest);

        LOG.info("Tabular query before getdata: {}", timeSeriesQuery);
        try {
            timeSeriesResponse = dataService.getTimeSeriesData(timeSeriesQuery);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
        if (timeSeriesResponse.getData() == null) {
            LOG.warn("No data found for the request");
            return ResponseEntity.badRequest().body(timeSeriesResponse);
        }        LOG.info("Visualization data retrieval successful");
    
        return ResponseEntity.ok(timeSeriesResponse);
    }
   
    @PostMapping("/visualization/data")
    public ResponseEntity<TabularResults> tabulardata(@Valid @RequestBody TabularRequest tabularRequest) {
        LOG.info("Request for visualization data {}", tabularRequest);

        if (tabularRequest.getDatasetId() == null) {
            LOG.error("Dataset ID is missing");
            return ResponseEntity.badRequest().body(new TabularResults());
        }
        TabularResults tabularResults = new TabularResults();
        
        TabularQuery tabularQuery = dataService.tabularQueryPreperation(tabularRequest);

        LOG.info("Tabular query before getdata: {}", tabularQuery);
        try {
            tabularResults = dataService.getTabularData(tabularQuery);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
        if (tabularResults.getData() == null) {
            LOG.warn("No data found for the request");
            return ResponseEntity.badRequest().body(tabularResults);
        }        
        LOG.info("Visualization data retrieval successful");
    
        return ResponseEntity.ok(tabularResults);
    }

    @PostMapping("/visualization/olddata")
    public ResponseEntity<VisualizationResults> data(@Valid @RequestBody VisualizationDataRequest visualizationDataRequest) {
        LOG.info("Request for visualization data {}", visualizationDataRequest);

        if (visualizationDataRequest.getDatasetId() == null) {
            LOG.error("Dataset ID is missing");
            return ResponseEntity.badRequest().body(new VisualizationResults());
        }
        VisualizationResults visualizationResults = new VisualizationResults();
        
        VisualQuery visualQuery = dataService.queryPreperation(visualizationDataRequest);

        LOG.info("Visualization query before getdata: {}", visualQuery);
        try {
            visualizationResults = dataService.getData(visualQuery);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
        if (visualizationResults.getData() == null) {
            LOG.warn("No data found for the request");
            return ResponseEntity.badRequest().body(visualizationResults);
        }        LOG.info("Visualization data retrieval successful");
    
        return ResponseEntity.ok(visualizationResults);
    }

    @GetMapping("/visualization/data/columns")
    public ResponseEntity<List<VisualColumn>> getColumns(String datasetId) {
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


    @GetMapping("/visualization/data/columns/{columnName}")
    public ResponseEntity<String> getColumn(String datasetId, @PathVariable String columnName) {
    
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
    
}


