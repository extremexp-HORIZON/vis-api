package gr.imsi.athenarc.xtremexpvisapi.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.FetchColumnsRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.DataSource;
import gr.imsi.athenarc.xtremexpvisapi.service.DataSourceService;
import gr.imsi.athenarc.xtremexpvisapi.service.dataService.v1.DataServiceV1;
import gr.imsi.athenarc.xtremexpvisapi.service.dataService.v2.DataServiceV2;
import jakarta.validation.Valid;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.io.File;

@RestController
@CrossOrigin
@RequestMapping("/api/data")
public class DataController {

    private static final Logger LOG = LoggerFactory.getLogger(DataController.class);

    @Autowired
    private DataSourceService dataSourceService;

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

    @GetMapping("/fetch/{datasetId}/row/{objectId}")
    public ResponseEntity<String[]> fetchRow(@PathVariable String datasetId, @PathVariable String objectId)
            throws IOException, SQLException {
        LOG.info("REST request to retrieve object {} from dataset {}", objectId, datasetId);
        try {
            DataSource dataSource = dataSourceService.findByFileName(datasetId).get();

            String[] row = dataServiceV2.fetchRow(dataSource, objectId);
            return ResponseEntity.ok(row);
        } catch (SQLException e) {
            LOG.error("SQL error retrieving row", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(null);
        } catch (Exception e) {
            LOG.error("Error retrieving row", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @PostMapping("/fetch/{datasetId}/columns")
    public ResponseEntity<Map<String, Object[]>> fetchColumnsValues(
            @PathVariable String datasetId,
            @RequestBody FetchColumnsRequest fetchColumnsRequest) {
        LOG.info("Request for columns values for dataset {}, rectangle {}, columnNames {}",
                datasetId,
                fetchColumnsRequest.getRectangle(),
                fetchColumnsRequest.getColumnNames());
        try {
            DataSource dataSource = dataSourceService.findByFileName(datasetId)
                    .orElseThrow(() -> new IllegalArgumentException("Dataset not found: " + datasetId));
            Map<String, Object[]> result = dataServiceV2.fetchColumnsValues(
                    dataSource,
                    fetchColumnsRequest.getRectangle(),
                    fetchColumnsRequest.getLatCol(),
                    fetchColumnsRequest.getLonCol(),
                    fetchColumnsRequest.getColumnNames()
            );
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            LOG.error("Invalid dataset: {}", datasetId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        } catch (SQLException e) {
            LOG.error("SQL error fetching columns values", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(null);
        } catch (Exception e) {
            LOG.error("Error fetching columns values", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @PostMapping("/meta")
    public CompletableFuture<ResponseEntity<Object>> getFileMeta(@RequestBody DataSource dataSource,
            @RequestHeader(value = "Authorization", required = false) String authorization)
            throws SQLException, Exception {
        LOG.info("Getting metadata for file {}", dataSource.getSource());
        
        // Check if this is an image file NEW NEW
        if (isImageFile(dataSource)) {
            LOG.info("Detected image file, processing with download for: {}", dataSource.getSource());
            return dataServiceV2.getImageMetadata(dataSource, authorization)
                .thenApply(response -> ResponseEntity.ok((Object) response))
                .exceptionally(throwable -> {
                    LOG.error("Error getting image metadata", throwable);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body((Object) ("Error getting image metadata: " + throwable.getMessage()));
                });
        }
        
        // For non-image files, use the existing metadata service TILL HERE TILL HERE
        return dataServiceV2.getFileMetadata(dataSource, authorization)
                .thenApply(response -> ResponseEntity.ok((Object) response))
                .exceptionally(throwable -> {
                    LOG.error("Error getting file metadata", throwable);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body((Object) ("Error getting file metadata: " + throwable.getMessage()));
                });
    }


    @GetMapping("/file")
    public ResponseEntity<Resource> getFile(@RequestParam String path) {
        File file = new File(path);

        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        FileSystemResource resource = new FileSystemResource(file);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + file.getName())
                .contentType(MediaType.parseMediaType(getContentType(file.getName())))
                .body(resource);
    }

    private String getContentType(String filename) {
        if (filename.endsWith(".png")) return "image/png";
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "image/jpeg";
        if (filename.endsWith(".gif")) return "image/gif";
        
        return "application/octet-stream";
    }
// NEW NEW
    private boolean isImageFile(DataSource dataSource) {
        String source = dataSource.getFormat();
        if (source == null) return false;
        
        // Check by file extension in source URL or filename
        String lowerSource = source.toLowerCase();
        return lowerSource.matches(".*\\.(png|jpg|jpeg|gif|webp|bmp|tiff?|svg)($|\\?.*)");
    }
    // Till here
}