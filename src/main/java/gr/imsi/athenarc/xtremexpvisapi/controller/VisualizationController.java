package gr.imsi.athenarc.xtremexpvisapi.controller;

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

import gr.imsi.athenarc.xtremexpvisapi.domain.Explainability.ApplyAffectedActionsRes;
import gr.imsi.athenarc.xtremexpvisapi.domain.Explainability.ExplanationsReq;
import gr.imsi.athenarc.xtremexpvisapi.domain.Explainability.ExplanationsRes;
import gr.imsi.athenarc.xtremexpvisapi.domain.Metadata.MetadataRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.Metadata.MetadataResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TabularRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TabularResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TimeSeriesRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TimeSeriesResponse;
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

    public VisualizationController(DataService dataService, ExplainabilityService explainabilityService, FileService fileService) {
        this.dataService = dataService;
        this.explainabilityService = explainabilityService;
    }

    @PostMapping("/explainability")
    public ResponseEntity<ExplanationsRes> getExplanation(@RequestBody ExplanationsReq request) throws JsonProcessingException, InvalidProtocolBufferException {
        LOG.info("Request for explainability {}{}{}{}", request.getExplanationType(),request.getExplanationMethod(),request.getFeature1(), request.getModel());
        return ResponseEntity.ok(explainabilityService.GetExplains(request));
    }
    
    @GetMapping("/affected")
    public ResponseEntity<ApplyAffectedActionsRes> applyAffectedActions() throws JsonProcessingException, InvalidProtocolBufferException {
        LOG.info("Request for apply affected actions");
        return ResponseEntity.ok(explainabilityService.ApplyAffectedActions());
    }


    @PostMapping("/visualization/timeseries")
    public TimeSeriesResponse getTimeSeriesData(@Valid @RequestBody TimeSeriesRequest timeSeriesRequest) {
        LOG.info("Request for visualization data {}", timeSeriesRequest);    
        return dataService.getTimeSeriesData(timeSeriesRequest);
    }
   
    @PostMapping("/visualization/tabular")
    public TabularResponse tabulardata(@Valid @RequestBody TabularRequest tabularRequest) {
        LOG.info("Request for visualization data {}", tabularRequest);
        return dataService.getTabularData(tabularRequest);
    }

    @PostMapping("/visualization/metadata")
    public MetadataResponse getFileMetadata(@RequestBody MetadataRequest metadataRequest) {
        LOG.info("Getting metadata for file {}", metadataRequest.getDatasetId());
        return dataService.getFileMetadata(metadataRequest);
    }
    
}


