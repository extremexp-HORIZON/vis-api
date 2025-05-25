package gr.imsi.athenarc.xtremexpvisapi.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.InvalidProtocolBufferException;

import gr.imsi.athenarc.xtremexpvisapi.domain.Metadata.MetadataRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.Metadata.MetadataResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TabularRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TabularResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TimeSeriesRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TimeSeriesResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.experiment.DataAsset;
import gr.imsi.athenarc.xtremexpvisapi.service.DataService;
import jakarta.validation.Valid;

@RestController
@CrossOrigin
@RequestMapping("/api/data")
public class DataController {
 
    private static final Logger LOG = LoggerFactory.getLogger(DataController.class);
    
    private final DataService dataService;

    public DataController(DataService dataService) {
        this.dataService = dataService;
    }
    
    @PostMapping("/umap")
    public float[][] dimensionalityReduction(@RequestBody float[][] data) throws JsonProcessingException, InvalidProtocolBufferException {
        LOG.info("Request for dimensionality reduction");
        return dataService.getUmap(data);
    }

    @PostMapping("/timeseries")
    public TimeSeriesResponse getTimeSeriesData(@Valid @RequestBody TimeSeriesRequest timeSeriesRequest) {
        LOG.info("Request for time series data {}", timeSeriesRequest);    
        return dataService.getTimeSeriesData(timeSeriesRequest);
    }
   
    @PostMapping("/tabular")
    public TabularResponse tabulardata(@Valid @RequestBody TabularRequest tabularRequest) {
        LOG.info("Request for tabular data {}", tabularRequest);
        return dataService.getTabularData(tabularRequest);
    }

    @PostMapping("/metadata")
    public MetadataResponse getFileMetadata(@RequestBody MetadataRequest metadataRequest) {
        LOG.info("Getting metadata for file {}", metadataRequest.getDatasetId());
        return dataService.getFileMetadata(metadataRequest);
    }

    @GetMapping("/catalog-assets")
    public ResponseEntity<List<DataAsset>> fetchRemoteAssets() {
        LOG.info("Fetching remote data assets from external catalog");
        String remoteUrl = "http://146.124.106.200/api/catalog/";

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity(remoteUrl, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            LOG.error("Failed to fetch remote assets: HTTP {}", response.getStatusCode());
            return ResponseEntity.status(response.getStatusCode()).build();
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            JsonNode dataArray = root.path("data");

            List<DataAsset> dataAssets = new ArrayList<>();
            for (JsonNode fileNode : dataArray) {
                DataAsset asset = new DataAsset();
                asset.setName(fileNode.path("upload_filename").asText(""));
                asset.setSourceType("http");
                asset.setSource("http://146.124.106.200/" + fileNode.path("path").asText());
                asset.setFormat(fileNode.path("file_type").asText(""));
                asset.setRole(DataAsset.Role.INPUT); // Adjust as needed
                asset.setTask(fileNode.path("description").asText(""));

                Map<String, String> tags = new HashMap<>();
                tags.put("created", fileNode.path("created").asText(""));
                tags.put("projectId", fileNode.path("project_id").asText(""));
                tags.put("id", fileNode.path("id").asText(""));
                asset.setTags(tags);

                dataAssets.add(asset);
            }

            LOG.info("Fetched {} data assets", dataAssets.size());
            return ResponseEntity.ok(dataAssets);

        } catch (Exception e) {
            LOG.error("Error processing data assets", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}