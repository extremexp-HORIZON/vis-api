package gr.imsi.athenarc.xtremexpvisapi.controller;

import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.protobuf.InvalidProtocolBufferException;

import gr.imsi.athenarc.xtremexpvisapi.domain.Metadata.MetadataRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.Metadata.MetadataResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.Metadata.MetadataResponse2;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TabularRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TabularResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TimeSeriesRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TimeSeriesResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.DataRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.DatasetMeta;
import gr.imsi.athenarc.xtremexpvisapi.service.DataService;
import gr.imsi.athenarc.xtremexpvisapi.service.dataquery.DataQueryService;
import jakarta.validation.Valid;

@RestController
@CrossOrigin
@RequestMapping("/api/data")
public class DataController {

    private static final Logger LOG = LoggerFactory.getLogger(DataController.class);

    private final DataService dataService;
    private final DataQueryService dataQueryService;

    public DataController(DataService dataService,
            DataQueryService dataQueryService) {
        this.dataService = dataService;
        this.dataQueryService = dataQueryService;
    }

    @PostMapping("/umap")
    public float[][] dimensionalityReduction(@RequestBody float[][] data)
            throws JsonProcessingException, InvalidProtocolBufferException {
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

    @PostMapping("/fetch")
    public CompletableFuture<ResponseEntity<Object>> fetchData(@Valid @RequestBody DataRequest dataRequest) throws SQLException, Exception {
        LOG.info("Received request for data: {}", dataRequest);

        return dataQueryService.executeDataRequest(dataRequest)
                .thenApply(response -> {
                    LOG.info("DuckDB query executed successfully. Returned {} rows", response.getQuerySize());
                    return ResponseEntity.ok((Object) response);
                })
                .exceptionally(throwable -> {
                    if (throwable.getCause() instanceof SQLException) {
                        SQLException e = (SQLException) throwable.getCause();
                        LOG.error("SQL error executing DuckDB query", e);
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of(
                                        "error", "SQL Error",
                                        "message", e.getMessage(),
                                        "sqlState", e.getSQLState() != null ? e.getSQLState() : "Unknown"));
                    } else {
                        LOG.error("Error executing DuckDB tabular query", throwable);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Map.of(
                                        "error", "Internal Server Error",
                                        "message", throwable.getMessage()));
                    }
                });
    }

    @PostMapping("/meta")
    public CompletableFuture<ResponseEntity<Object>> getFileMeta(@RequestBody DatasetMeta datasetMeta) throws SQLException, Exception {
        LOG.info("Getting metadata for file {}", datasetMeta.getSource());
        return dataQueryService.getFileMetadata(datasetMeta)
                .thenApply(response -> ResponseEntity.ok((Object) response))
                .exceptionally(throwable -> {
                    LOG.error("Error getting file metadata", throwable);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body((Object) ("Error getting file metadata: " + throwable.getMessage()));
                });
    }
}