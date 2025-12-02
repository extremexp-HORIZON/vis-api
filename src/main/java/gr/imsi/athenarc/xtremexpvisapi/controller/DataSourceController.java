package gr.imsi.athenarc.xtremexpvisapi.controller;

import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.DataSource;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.SourceType;
import gr.imsi.athenarc.xtremexpvisapi.service.DataSourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@CrossOrigin
@RequestMapping("/api/datasources")
public class DataSourceController {

    private static final Logger LOG = LoggerFactory.getLogger(DataSourceController.class);

    private final DataSourceService dataSourceService;

    @Autowired
    public DataSourceController(DataSourceService dataSourceService) {
        this.dataSourceService = dataSourceService;
    }

    /**
     * Get all DataSources
     * 
     * @return ResponseEntity containing list of all DataSources
     */
    @GetMapping
    public ResponseEntity<List<DataSource>> getAllDataSources() {
        LOG.info("Request to get all DataSources");
        try {
            List<DataSource> dataSources = dataSourceService.findAll();
            LOG.info("Found {} DataSources", dataSources.size());
            return ResponseEntity.ok(dataSources);
        } catch (Exception e) {
            LOG.error("Error retrieving all DataSources", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get a DataSource by fileName
     * 
     * @param fileName the fileName of the DataSource to retrieve
     * @return ResponseEntity containing the DataSource if found, 404 otherwise
     */
    @GetMapping("/{fileName}")
    public ResponseEntity<DataSource> getDataSourceByFileName(@PathVariable String fileName) {
        LOG.info("Request to get DataSource with fileName: {}", fileName);
        try {
            Optional<DataSource> dataSource = dataSourceService.findByFileName(fileName);
            if (dataSource.isPresent()) {
                LOG.info("Found DataSource: {}", fileName);
                return ResponseEntity.ok(dataSource.get());
            } else {
                LOG.info("DataSource not found: {}", fileName);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            LOG.error("Error retrieving DataSource: {}", fileName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create a new DataSource
     * 
     * @param dataSource the DataSource to create
     * @return ResponseEntity containing the created DataSource
     */
    @PostMapping
    public ResponseEntity<DataSource> createDataSource(@Valid @RequestBody DataSource dataSource) {
        LOG.info("Request to create DataSource: {}", dataSource.getFileName());
        try {
            // Check if DataSource already exists
            if (dataSourceService.existsByFileName(dataSource.getFileName())) {
                LOG.warn("DataSource already exists: {}", dataSource.getFileName());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(null);
            }

            DataSource createdDataSource = dataSourceService.insert(dataSource);
            LOG.info("Created DataSource: {}", createdDataSource.getFileName());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdDataSource);
        } catch (IllegalArgumentException e) {
            LOG.error("Invalid DataSource data: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            LOG.error("Error creating DataSource: {}", dataSource.getFileName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update an existing DataSource
     * 
     * @param fileName   the fileName of the DataSource to update
     * @param dataSource the updated DataSource data
     * @return ResponseEntity containing the updated DataSource
     */
    @PutMapping("/{fileName}")
    public ResponseEntity<DataSource> updateDataSource(@PathVariable String fileName,
            @Valid @RequestBody DataSource dataSource) {
        LOG.info("Request to update DataSource: {}", fileName);
        try {
            // Check if DataSource exists
            if (!dataSourceService.existsByFileName(fileName)) {
                LOG.warn("DataSource not found for update: {}", fileName);
                return ResponseEntity.notFound().build();
            }

            // Ensure the fileName in the path matches the one in the body
            dataSource.setFileName(fileName);

            // Delete the old one and create the new one
            dataSourceService.deleteByFileName(fileName);
            DataSource updatedDataSource = dataSourceService.insert(dataSource);

            LOG.info("Updated DataSource: {}", fileName);
            return ResponseEntity.ok(updatedDataSource);
        } catch (Exception e) {
            LOG.error("Error updating DataSource: {}", fileName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete a DataSource by fileName
     * 
     * @param fileName the fileName of the DataSource to delete
     * @return ResponseEntity indicating success or failure
     */
    @DeleteMapping("/{fileName}")
    public ResponseEntity<Void> deleteDataSource(@PathVariable String fileName) {
        LOG.info("Request to delete DataSource: {}", fileName);
        try {
            if (!dataSourceService.existsByFileName(fileName)) {
                LOG.warn("DataSource not found for deletion: {}", fileName);
                return ResponseEntity.notFound().build();
            }

            boolean deleted = dataSourceService.deleteByFileName(fileName);
            if (deleted) {
                LOG.info("Deleted DataSource: {}", fileName);
                return ResponseEntity.noContent().build();
            } else {
                LOG.error("Failed to delete DataSource: {}", fileName);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            LOG.error("Error deleting DataSource: {}", fileName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Check if a DataSource exists
     * 
     * @param fileName the fileName to check
     * @return ResponseEntity with boolean indicating existence
     */
    @GetMapping("/{fileName}/exists")
    public ResponseEntity<Map<String, Boolean>> checkDataSourceExists(@PathVariable String fileName) {
        LOG.info("Request to check if DataSource exists: {}", fileName);
        try {
            boolean exists = dataSourceService.existsByFileName(fileName);
            LOG.info("DataSource '{}' exists: {}", fileName, exists);
            return ResponseEntity.ok(Map.of("exists", exists));
        } catch (Exception e) {
            LOG.error("Error checking DataSource existence: {}", fileName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Upload a file and create a new DataSource
     * 
     * @param file       the CSV file to upload
     * @param fileName   optional fileName (if not provided, uses original filename)
     * @param source     optional source description
     * @param format     optional format (defaults to csv)
     * @param sourceType optional source type (defaults to local)
     * @return ResponseEntity containing the created DataSource
     */
    @PostMapping("/upload")
    public ResponseEntity<Object> uploadFileAndCreateDataSource(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "fileName", required = false) String fileName,
            @RequestParam(value = "source", required = false) String source,
            @RequestParam(value = "measure0", required = false) String measure0,
            @RequestParam(value = "measure1", required = false) String measure1,
            @RequestParam(value = "isRawVis", required = false) Boolean isRawVis,
            @RequestParam(value = "format", required = false, defaultValue = "csv") String format,
            @RequestParam(value = "sourceType", required = false, defaultValue = "local") String sourceType) {

        LOG.info("Request to upload file and create DataSource");

        try {
            // Validate file
            if (file.isEmpty()) {
                LOG.warn("Empty file uploaded");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "File is empty"));
            }

            // Determine fileName if not provided
            if (fileName == null || fileName.trim().isEmpty()) {
                String originalFilename = file.getOriginalFilename();
                if (originalFilename != null && originalFilename.contains(".")) {
                    fileName = originalFilename.substring(0, originalFilename.lastIndexOf("."));
                } else {
                    fileName = "uploaded_dataset_" + System.currentTimeMillis();
                }
            }

            // Check if DataSource already exists
            if (dataSourceService.existsByFileName(fileName)) {
                LOG.warn("DataSource already exists: {}", fileName);
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "DataSource with fileName '" + fileName + "' already exists"));
            }

            // Create DataSource object
            DataSource dataSource = new DataSource();
            dataSource.setFileName(fileName);
            dataSource.setSource(source != null ? source : "Uploaded file: " + file.getOriginalFilename());
            dataSource.setFormat(format);
            dataSource.setSourceType(SourceType.valueOf(sourceType));
            dataSource.setMeasure0(measure0);
            dataSource.setMeasure1(measure1);
            dataSource.setIsRawVis(isRawVis);

            // Create the DataSource and upload the file
            DataSource createdDataSource = dataSourceService.createDataSourceFromUpload(dataSource, file);

            LOG.info("Successfully created DataSource from upload: {}", fileName);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdDataSource);

        } catch (Exception e) {
            LOG.error("Error uploading file and creating DataSource", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error uploading file: " + e.getMessage()));
        }
    }

    /**
     * Get the dataset file path for a DataSource
     * 
     * @param fileName the fileName of the DataSource
     * @return ResponseEntity containing the file path
     */
    @GetMapping("/{fileName}/dataset-path")
    public ResponseEntity<Map<String, String>> getDatasetFilePath(@PathVariable String fileName) {
        LOG.info("Request to get dataset file path for DataSource: {}", fileName);
        try {
            if (!dataSourceService.existsByFileName(fileName)) {
                LOG.warn("DataSource not found: {}", fileName);
                return ResponseEntity.notFound().build();
            }

            Path datasetPath = dataSourceService.getDatasetFilePath(fileName);
            LOG.info("Dataset file path for DataSource '{}': {}", fileName, datasetPath);
            return ResponseEntity.ok(Map.of("datasetPath", datasetPath.toString()));
        } catch (Exception e) {
            LOG.error("Error getting dataset file path for DataSource: {}", fileName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get the metadata file path for a DataSource
     * 
     * @param fileName the fileName of the DataSource
     * @return ResponseEntity containing the metadata file path
     */
    @GetMapping("/{fileName}/metadata-path")
    public ResponseEntity<Map<String, String>> getMetadataFilePath(@PathVariable String fileName) {
        LOG.info("Request to get metadata file path for DataSource: {}", fileName);
        try {
            if (!dataSourceService.existsByFileName(fileName)) {
                LOG.warn("DataSource not found: {}", fileName);
                return ResponseEntity.notFound().build();
            }

            Path metadataPath = dataSourceService.getMetadataFilePath(fileName);
            LOG.info("Metadata file path for DataSource '{}': {}", fileName, metadataPath);
            return ResponseEntity.ok(Map.of("metadataPath", metadataPath.toString()));
        } catch (Exception e) {
            LOG.error("Error getting metadata file path for DataSource: {}", fileName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}