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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.protobuf.InvalidProtocolBufferException;

import gr.imsi.athenarc.xtremexpvisapi.domain.Metadata.MetadataRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.Metadata.MetadataResponseV1;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryv1.TabularRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryv1.TabularResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryv1.TimeSeriesRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryv1.TimeSeriesResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.DataRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.DataSource;
import gr.imsi.athenarc.xtremexpvisapi.service.dataService.v1.DataServiceV1;
import gr.imsi.athenarc.xtremexpvisapi.service.dataService.v2.DataServiceV2;
import jakarta.validation.Valid;

@RestController
@CrossOrigin
@RequestMapping("/api/data")
public class DataController {

    private static final Logger LOG = LoggerFactory.getLogger(DataController.class);

    private final DataServiceV1 dataServiceV1;
    private final DataServiceV2 dataServiceV2;

    public DataController(DataServiceV1 dataServiceV1,
            DataServiceV2 dataServiceV2) {
        this.dataServiceV1 = dataServiceV1;
        this.dataServiceV2 = dataServiceV2;
    }

    @PostMapping("/umap")
    public float[][] dimensionalityReduction(@RequestBody float[][] data)
            throws JsonProcessingException, InvalidProtocolBufferException {
        LOG.info("Request for dimensionality reduction");
        return dataServiceV2.getUmap(data);
    }

    @PostMapping("/timeseries")
    public TimeSeriesResponse getTimeSeriesData(@Valid @RequestBody TimeSeriesRequest timeSeriesRequest) {
        LOG.info("Request for time series data {}", timeSeriesRequest);
        return dataServiceV1.getTimeSeriesData(timeSeriesRequest);
    }

    @PostMapping("/tabular")
    public TabularResponse tabulardata(@Valid @RequestBody TabularRequest tabularRequest) {
        LOG.info("Request for tabular data {}", tabularRequest);
        return dataServiceV1.getTabularData(tabularRequest);
    }

    @PostMapping("/metadata")
    public MetadataResponseV1 getFileMetadata(@RequestBody MetadataRequest metadataRequest) {
        LOG.info("Getting metadata for file {}", metadataRequest.getDatasetId());
        return dataServiceV1.getFileMetadata(metadataRequest);
    }

    @PostMapping("/fetch")
    public CompletableFuture<ResponseEntity<Object>> fetchData(@Valid @RequestBody DataRequest dataRequest,
            @RequestHeader(value = "Authorization", required = false) String authorization)
            throws SQLException, Exception {
        LOG.info("Received request for data: {}", dataRequest);

        return dataServiceV2.executeDataRequest(dataRequest, authorization)
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
    public CompletableFuture<ResponseEntity<Object>> getFileMeta(@RequestBody DataSource dataSource,
            @RequestHeader(value = "Authorization", required = false) String authorization)
            throws SQLException, Exception {
        LOG.info("Getting metadata for file {}", dataSource.getSource());
        return dataServiceV2.getFileMetadata(dataSource, authorization)
                .thenApply(response -> ResponseEntity.ok((Object) response))
                .exceptionally(throwable -> {
                    LOG.error("Error getting file metadata", throwable);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body((Object) ("Error getting file metadata: " + throwable.getMessage()));
                });
    }
}