package gr.imsi.athenarc.xtremexpvisapi.controller;

import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.protobuf.InvalidProtocolBufferException;

import gr.imsi.athenarc.xtremexpvisapi.domain.Metadata.RawVisDataset;
import gr.imsi.athenarc.xtremexpvisapi.domain.Metadata.MetadataRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.Metadata.MetadataResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.MapDataRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TabularRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TabularResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TimeSeriesQuery;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TimeSeriesRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.TimeSeriesResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.DataRequest;
import gr.imsi.athenarc.xtremexpvisapi.service.DataService;
import gr.imsi.athenarc.xtremexpvisapi.service.MapQueryService;
import gr.imsi.athenarc.xtremexpvisapi.service.RawVisDatasetService;
import gr.imsi.athenarc.xtremexpvisapi.service.TabularQueryService;
import jakarta.validation.Valid;

@RestController
@CrossOrigin
@RequestMapping("/api/data")
public class DataController {

    private static final Logger LOG = LoggerFactory.getLogger(DataController.class);

    private final DataService dataService;
    private final TabularQueryService tabularQueryService;
    private final MapQueryService mapQueryService;
    private final RawVisDatasetService rawVisDatasetService;

    public DataController(DataService dataService,
            TabularQueryService tabularQueryService,
            MapQueryService mapQueryService,
            RawVisDatasetService rawVisDatasetService) {
        this.dataService = dataService;
        this.tabularQueryService = tabularQueryService;
        this.mapQueryService = mapQueryService;
        this.rawVisDatasetService = rawVisDatasetService;
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

    @PostMapping("/map")
    public CompletableFuture<ResponseEntity<Object>> executeMapDataQuery(@RequestBody MapDataRequest mapDataRequest) throws SQLException, Exception {
        LOG.info("Executing MapDataRequest {} via DuckDB.", mapDataRequest);

        RawVisDataset rawVisDataset = rawVisDatasetService.findById(mapDataRequest.getDatasetId()).get();

        return mapQueryService.executeMapDataRequest(mapDataRequest, rawVisDataset)
                .thenApply(respone -> {
                    LOG.info("DuckDB query executed successfully. Returned {}");
                    return ResponseEntity.ok((Object) respone);
                })
                .exceptionally(throwable -> {
                    if (throwable.getCause() instanceof SQLException) {
                        SQLException e = (SQLException) throwable.getCause();
                        LOG.error("SQL Error executing DuckDB query", e);
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

    @PostMapping("/timeseries/{id}")
    public CompletableFuture<ResponseEntity<Object>> executeTimeSeriesQuery(@PathVariable String id, @RequestBody  TimeSeriesQuery timeSeriesQuery) throws SQLException, Exception {
        LOG.info("Executing TimeSeriesQuery : {} via DuckDB for RawVisDataset : {}", timeSeriesQuery, id);

        RawVisDataset rawVisDataset = rawVisDatasetService.findById(id).get();
        return mapQueryService.executeTimesSeriesQuery(rawVisDataset, timeSeriesQuery)
                .thenApply(response -> {
                    LOG.info("DuckDB timeSeries query executed successfully. Returned {}", response);
                    return ResponseEntity.ok((Object) response);
                })
                .exceptionally(throwable -> {
                    if (throwable.getCause() instanceof SQLException) {
                        SQLException e = (SQLException) throwable.getCause();
                        LOG.error("SQL Error executing DuckDB query", e);
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

    @PostMapping("/map/duckdb-sql")
    public CompletableFuture<ResponseEntity<Object>> getDuckDbMapSql(@RequestBody MapDataRequest mapDataRequest ) throws Exception {
        LOG.info("Generating DuckDB SQL for request: {}", mapDataRequest);

        RawVisDataset rawVisDataset = rawVisDatasetService.findById(mapDataRequest.getDatasetId()).get();

        return mapQueryService.buildQuery(mapDataRequest, rawVisDataset)
                .thenApply(sql -> {
                    LOG.info("Generated SQL: {}", sql);
                    return ResponseEntity.ok((Object) Map.of(
                            "sql", sql,
                            "request", mapDataRequest,
                            "timestamp", java.time.Instant.now().toString()));
                })
                .exceptionally(throwable -> {
                    LOG.error("Error generating DuckDB SQL", throwable);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of(
                                    "error", "Error generating SQL",
                                    "message", throwable.getMessage(),
                                    "request", mapDataRequest));
                });
    }

    @PostMapping("/tabular/duckdb-test")
    public CompletableFuture<ResponseEntity<Object>> testDuckDbTabularQuery(@Valid @RequestBody DataRequest dataRequest) throws SQLException, Exception {
        LOG.info("Testing DuckDB tabular query with request: {}", dataRequest);

        return tabularQueryService.executeDataRequest(dataRequest)
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

    @PostMapping("/tabular/duckdb-sql")
    public CompletableFuture<ResponseEntity<Object>> getDuckDbSql(@Valid @RequestBody DataRequest dataRequest) throws Exception {
        LOG.info("Generating DuckDB SQL for request: {}", dataRequest);

        return tabularQueryService.buildQuery(dataRequest)
                .thenApply(sql -> {
                    LOG.info("Generated SQL: {}", sql);
                    return ResponseEntity.ok((Object) Map.of(
                            "sql", sql,
                            "request", dataRequest,
                            "timestamp", java.time.Instant.now().toString()));
                })
                .exceptionally(throwable -> {
                    LOG.error("Error generating DuckDB SQL", throwable);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of(
                                    "error", "Error generating SQL",
                                    "message", throwable.getMessage(),
                                    "request", dataRequest));
                });
    }

}