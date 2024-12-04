package gr.imsi.athenarc.xtremexpvisapi.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.protobuf.InvalidProtocolBufferException;

import gr.imsi.athenarc.xtremexpvisapi.domain.TabularColumn;
import gr.imsi.athenarc.xtremexpvisapi.domain.TabularRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.TabularResults;
import gr.imsi.athenarc.xtremexpvisapi.domain.TimeSeriesRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.TimeSeriesResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.ExplabilityProcedure.ExplanationsReq;
import gr.imsi.athenarc.xtremexpvisapi.domain.ExplabilityProcedure.ExplanationsRes;
import gr.imsi.athenarc.xtremexpvisapi.domain.InitializeProcedure.FeatureExplanation;
import gr.imsi.athenarc.xtremexpvisapi.domain.ModelAnalysisTask.ModelAnalysisTaskReq;
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
   
    @PostMapping("/visualization/tabular")
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

    @GetMapping("/visualization/data/columns")
    public ResponseEntity<List<TabularColumn>> getColumns(String datasetId) {
        try {
            // Retrieve columns for the specified datasetId
            List<TabularColumn> columns = dataService.getColumns(datasetId);

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
    
}


