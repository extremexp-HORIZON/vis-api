package gr.imsi.athenarc.xtremexpvisapi.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.InvalidProtocolBufferException;

import gr.imsi.athenarc.xtremexpvisapi.domain.Metadata.MetadataRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.Metadata.MetadataResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TabularRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TabularResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TimeSeriesRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TimeSeriesResponse;
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

    @PostMapping("/explainability")
    public JsonNode getExplanation(@RequestBody String jsonRequest) throws JsonProcessingException, InvalidProtocolBufferException {
        LOG.info("Received explainability request: \n{}", jsonRequest);
        return explainabilityService.GetExplains(jsonRequest);
    }
    
    @GetMapping("/affected")
    public JsonNode applyAffectedActions() throws JsonProcessingException, InvalidProtocolBufferException {
        LOG.info("Request for apply affected actions");
        return explainabilityService.ApplyAffectedActions();
    }
    
    @PostMapping("/umap")
    public float[][] dimensionalityReduction(@RequestBody float[][] data) throws JsonProcessingException, InvalidProtocolBufferException {
        LOG.info("Request for dimensionality reduction");
        return dataService.getUmap(data);
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


