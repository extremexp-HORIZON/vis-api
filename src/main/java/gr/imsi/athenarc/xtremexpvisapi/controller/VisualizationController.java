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
import gr.imsi.athenarc.xtremexpvisapi.domain.TabularResults;
import gr.imsi.athenarc.xtremexpvisapi.domain.TestReq;
import gr.imsi.athenarc.xtremexpvisapi.domain.TimeSeriesResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.ExplabilityProcedure.ExplanationsReq;
import gr.imsi.athenarc.xtremexpvisapi.domain.ExplabilityProcedure.ExplanationsRes;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TabularRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TimeSeriesRequest;
import gr.imsi.athenarc.xtremexpvisapi.service.DataService;
import gr.imsi.athenarc.xtremexpvisapi.service.ExplainabilityService;
import gr.imsi.athenarc.xtremexpvisapi.service.FileService;
import jakarta.validation.Valid;

@RestController
@CrossOrigin
@RequestMapping("/api")
public class VisualizationController {
 
    private static final Logger LOG = LoggerFactory.getLogger(VisualizationController.class);
    
    private final DataService dataService;
    private final ExplainabilityService explainabilityService;
    private final FileService fileService;

    public VisualizationController(DataService dataService, ExplainabilityService explainabilityService, FileService fileService) {
        this.dataService = dataService;
        this.explainabilityService = explainabilityService;
        this.fileService = fileService;
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
        
        // TimeSeriesQuery timeSeriesQuery = dataService.timeSeriesQueryPreperation(timeSeriesRequest);

        LOG.info("Tabular query before getdata: {}", timeSeriesRequest);
        try {
            timeSeriesResponse = dataService.getTimeSeriesData(timeSeriesRequest);
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
        
        // TabularQuery tabularQuery = dataService.tabularQueryPreperation(tabularRequest);

        LOG.info("Tabular query before getdata: {}", tabularRequest);
        try {
            tabularResults = dataService.getTabularData(tabularRequest);
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

    @PostMapping("/files/single")
    public void getSingleFile(@RequestBody TestReq path) {
        LOG.info("download file with uri: " + path.getUri());
        try {
            fileService.downloadFileFromZenoh(path.getUri());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}


