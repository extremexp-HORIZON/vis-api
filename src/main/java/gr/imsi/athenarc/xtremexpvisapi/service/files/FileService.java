package gr.imsi.athenarc.xtremexpvisapi.service.files;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.core.io.Resource;

import gr.imsi.athenarc.xtremexpvisapi.config.ApplicationFileProperties;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryv2.params.DataSource;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Log4j2
public class FileService {

    @Value("${mlflow.tracking.url}")
    private String mlflowTrackingUrl;

    @Value("${mlflow.tracking.token:}")
    private String mlflowTrackingToken;

    private final RestTemplate restTemplate;

    private static final List<String> UNITS = Arrays.asList("B", "KB", "MB", "GB", "TB", "PB", "EB");
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private final ApplicationFileProperties applicationFileProperties;
    private final FileHelper fileHelper;

    @Autowired
    public FileService(ApplicationFileProperties applicationFileProperties, FileHelper fileHelper, RestTemplate restTemplate) {
        this.applicationFileProperties = applicationFileProperties;
        this.fileHelper = fileHelper;
        this.restTemplate = restTemplate;
    }

    /**
     * Saves a file from an external source and schedules it for deletion after a
     * certain period.
     * If the file already exists in the cache, resets the deletion timer.
     *
     * @param experimentId the experiment ID to organize files by experiment
     * @param runId the run ID within the experiment
     * @param dataSource the data source information
     * @param authorization authorization token for downloading
     * @throws Exception if an error occurs
     */
    public String downloadAndCacheDataAsset(String experimentId, String runId, DataSource dataSource, String authorization)
            throws Exception {
        // Use experimentId from parameter if provided, otherwise try to get from dataSource
        String effectiveExperimentId = experimentId != null ? experimentId : 
                                     (dataSource.getExperimentId() != null ? dataSource.getExperimentId() : "unknown-experiment");
        String effectiveRunId = runId != null ? runId : 
                               (dataSource.getRunId() != null ? dataSource.getRunId() : "unknown-run");
        
        Path targetPath = getTargetPathForDataAsset(effectiveExperimentId, effectiveRunId, dataSource);
        // System.out.println("Target path: " + targetPath.toString());

        // Check if file is already cached and reset timer
        if (isFileCached(targetPath.toString())) {
            return targetPath.toString();
        }

        // Get input stream from the file source
        InputStream fileContent = getInputStreamForDataAsset(dataSource, authorization);
        if (fileContent == null) {
            throw new IOException("Failed to download file from: " + dataSource.getSource());
        }

        fileInsertionHandler(fileContent, fileContent.available(), targetPath);
        return targetPath.toString();
    }

    /**
     * Convenience method that extracts experiment and run IDs from the DataSource object.
     * If experimentId is not available in DataSource, uses "unknown-experiment" as default.
     * 
     * @param dataSource the data source information containing runId and optionally experimentId
     * @param authorization authorization token for downloading
     * @throws Exception if an error occurs
     */
    public String downloadAndCacheDataAsset(DataSource dataSource, String authorization) throws Exception {
        String experimentId = dataSource.getExperimentId() != null ? dataSource.getExperimentId() : "unknown-experiment";
        String runId = dataSource.getRunId() != null ? dataSource.getRunId() : "unknown-run";
        return downloadAndCacheDataAsset(experimentId, runId, dataSource, authorization);
    }

    /**
     * @deprecated Use {@link #downloadAndCacheDataAsset(String, String, DataSource, String)} instead.
     * This method is kept for backward compatibility but will organize files under
     * a default "unknown-experiment" folder.
     */
    @Deprecated
    public String downloadAndCacheDataAsset(String runId, DataSource dataSource, String authorization)
            throws Exception {
        return downloadAndCacheDataAsset("unknown-experiment", runId, dataSource, authorization);
    }

    private Path getTargetPathForDataAsset(String experimentId, String runId, DataSource dataSource) {
        String fileCacheDirectory = applicationFileProperties.getDirectory();

        if (dataSource.getFileName() != null && !dataSource.getFileName().isEmpty()) {
            // If a file name is provided, ensure it has an extension
            int lastDotIndex = dataSource.getFileName().lastIndexOf('.');
            String fileName = dataSource.getFileName();
            if (lastDotIndex <= 0 || lastDotIndex >= fileName.length() - 1) {
                // No valid extension, check if format is provided in DataSource
                String format = dataSource.getFormat();
                if (format != null && !format.isEmpty()) {
                    // Use the format from DataSource, ensure it starts with a dot
                    if (!format.startsWith(".")) {
                        format = "." + format;
                    }
                    fileName = fileName + format;
                } else {
                    // Default to .json if format not set
                    fileName = fileName + ".json";
                }
            }
            return Paths.get(fileCacheDirectory, experimentId, runId, fileName);
        } else {
            // No file name provided
            String sanitizedSource = dataSource.getSource().replaceAll("[^a-zA-Z0-9._-]", "_");
            String format = dataSource.getFormat();
            if (format == null || format.isEmpty()) {
                // Default to .json if format not set
                format = ".json";
            } else if (!format.startsWith(".")) {
                // Ensure format starts with a dot
                format = "." + format;
            }
            return Paths.get(fileCacheDirectory, experimentId, runId, sanitizedSource + format);
        }
    }



    private InputStream getInputStreamForDataAsset(DataSource dataSource, String authorization) throws Exception {
        // TODO: Add support for other data source types if needed
        switch (dataSource.getSourceType()) {
            case http:
                return fileHelper.downloadFromHTTP(dataSource, authorization).get();
            default:
                throw new UnsupportedOperationException("Source type not supported: " + dataSource.getSourceType());
        }
    }

    /**
     * Schedules the deletion of the file at the given path after X seconds.
     *
     * @param path the path of the file to delete
     */
    private void scheduleFileDeletion(Path targetPath) {
        ScheduledFuture<?> scheduledTask = scheduler.schedule(() -> {
            try {
                Files.deleteIfExists(targetPath);
                scheduledTasks.remove(targetPath.toString());
                // Delete empty parent directories recursively up to the cache directory
                deleteEmptyParentDirectories(targetPath.getParent(),
                        Paths.get(applicationFileProperties.getDirectory()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, applicationFileProperties.getDuration(),
                TimeUnit.valueOf(applicationFileProperties.getUnit()));

        scheduledTasks.put(targetPath.toString(), scheduledTask);
    }

    /**
     * Resets the deletion timer for the file with the given name.
     *
     * @param fileName the name of the file to reset the deletion timer for
     */
    private void resetFileDeletion(Path targetPath) {
        ScheduledFuture<?> scheduledTask = scheduledTasks.get(targetPath.toString());
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            scheduleFileDeletion(targetPath);
        }
    }

    /**
     * Recursively deletes empty parent directories up to (but not including) the
     * cache directory.
     * 
     * @param currentDir the current directory to check and potentially delete
     * @param cacheDir   the cache directory (stopping point)
     * @throws IOException if an I/O error occurs
     */
    private void deleteEmptyParentDirectories(Path currentDir, Path cacheDir) throws IOException {
        // Stop if we've reached the cache directory or gone beyond it
        if (currentDir == null || currentDir.equals(cacheDir) || !currentDir.startsWith(cacheDir)) {
            return;
        }

        // Check if the current directory is empty
        if (Files.isDirectory(currentDir) && Files.list(currentDir).count() == 0) {
            Files.delete(currentDir);
            log.info("Deleted empty parent directory: " + currentDir);

            // Recursively check the parent of the current directory
            deleteEmptyParentDirectories(currentDir.getParent(), cacheDir);
        }
    }

    /**
     * Handles File insertion based on size limitations.
     *
     * @param fileToBeInserted the file to be inserted.
     * @param targetPath       path variable that defines the target directory.
     */
    private void fileInsertionHandler(InputStream fileToBeInserted, long insertedFileSizeBytes, Path targetPath) {
        try {
            // Extract numbers and units from the folder size limit
            long limitNumber = Long.parseLong(applicationFileProperties.getSize().replaceAll("[^0-9]", ""));
            String limitUnit = applicationFileProperties.getSize().replaceAll("[^a-zA-Z]", "").toUpperCase();
            long directoryCurrentSizeBytes = getDirectorySize(Paths.get(applicationFileProperties.getDirectory()));
            long folderSizeLimitBytes = convertToBytes(limitNumber, limitUnit);

            log.info("Directory size: " + directoryCurrentSizeBytes + " bytes");
            log.info("File size: " + insertedFileSizeBytes + " bytes");
            log.info("Directory size limit: " + folderSizeLimitBytes + " bytes");

            if (insertedFileSizeBytes > folderSizeLimitBytes) {
                log.error("File insertion failed: File bigger than cache size limit");
            } else if (directoryCurrentSizeBytes + insertedFileSizeBytes > folderSizeLimitBytes) {
                log.info("File insertion postponed: Deleting old files to make space");
                makeSpaceAndInsertFile(targetPath, directoryCurrentSizeBytes, insertedFileSizeBytes,
                        folderSizeLimitBytes,
                        fileToBeInserted);
            } else {
                insertFile(fileToBeInserted, targetPath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Inserts the file into the target path and schedules it for deletion.
     *
     * @param fileToBeInserted the file to be inserted.
     * @param targetPath       path variable that defines the target directory.
     * @throws IOException if an I/O error occurs
     */
    private void insertFile(InputStream fileToBeInserted, Path targetPath) throws IOException {
        Files.createDirectories(targetPath.getParent());
        Files.copy(fileToBeInserted, targetPath, StandardCopyOption.REPLACE_EXISTING);
        scheduleFileDeletion(targetPath);
        log.info("File inserted successfully");
    }

    /**
     * Deletes files from the directory to make space for the new file (one by one).
     *
     * @param targetPath                path variable that defines the target
     *                                  directory.
     * @param directoryCurrentSizeBytes the current size of the directory in bytes.
     * @param insertedFileSizeBytes     the size of the file to be inserted in
     *                                  bytes.
     * @param folderSizeLimitBytes      the size limit of the directory in bytes.
     * @param fileToBeInserted          the file to be inserted.
     * @throws IOException if an I/O error occurs
     */
    private void makeSpaceAndInsertFile(Path targetPath, long directoryCurrentSizeBytes, long insertedFileSizeBytes,
            long folderSizeLimitBytes, InputStream fileToBeInserted) throws IOException {
        // Get all files in the directory and sort them by last modified time (oldest ->
        // newest)
        List<Path> files = Files.list(targetPath.getParent())
                .filter(Files::isRegularFile)
                .sorted((p1, p2) -> {
                    try {
                        FileTime p1LastAccess = Files.readAttributes(p1, BasicFileAttributes.class).lastAccessTime();
                        FileTime p2LastAccess = Files.readAttributes(p2, BasicFileAttributes.class).lastAccessTime();
                        return p1LastAccess.compareTo(p2LastAccess);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();

        long spaceFreed = 0;
        for (Path file : files) {
            long fileSize = Files.size(file);
            Files.delete(file);
            scheduledTasks.remove(targetPath.toString());
            spaceFreed += fileSize;
            log.info("Deleted file: " + file.getFileName() + " to free up space");

            if (directoryCurrentSizeBytes + insertedFileSizeBytes - spaceFreed <= folderSizeLimitBytes) {
                break;
            }
        }

        // Check if enough space was freed
        if (directoryCurrentSizeBytes + insertedFileSizeBytes - spaceFreed > folderSizeLimitBytes) {
            log.error("File insertion failed: Not enough space could be freed");
        } else {
            Files.copy(fileToBeInserted, targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("File inserted successfully after freeing up space");
        }
    }

    /**
     * Caclulates the size of a directory.
     *
     * @param path the name of the file to delete
     * @throws IOException if an I/O error occurs
     */
    private long getDirectorySize(Path path) throws IOException {
        try {
            return Files.walk(path)
                    .filter(p -> p.toFile().isFile())
                    .mapToLong(p -> p.toFile().length())
                    .sum();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Converts a size from a specific unit to bytes.
     *
     * @param value the numeric value to be converted.
     * @param unit  the unit of the value (e.g., "KB", "MB").
     * @return the size in bytes.
     */
    private static long convertToBytes(long value, String unit) {
        int unitIndex = UNITS.indexOf(unit);
        if (unitIndex == -1) {
            throw new IllegalArgumentException("Invalid size unit: " + unit);
        }
        return value * (long) Math.pow(1024, unitIndex);
    }

    /**
     * Checks if a file is cached and resets its deletion timer if it exists.
     *
     * @param targetPath the path of the file to check
     * @return true if the file is cached (timer reset), false if not cached
     */
    public boolean isFileCached(String targetPath) {
        if (scheduledTasks.containsKey(targetPath)) {
            log.info("File is cached, resetting deletion timer for: " + targetPath);
            resetFileDeletion(Paths.get(targetPath));
            return true;
        }

        // Also check if file physically exists
        if (Files.exists(Paths.get(targetPath))) {
            log.info("File exists but not in cache, scheduling deletion: " + targetPath);
            scheduleFileDeletion(Paths.get(targetPath));
            return true;
        }

        log.info("File is not cached: " + targetPath);
        return false;
    }

    public String downloadMlflowArtifact(DataSource ds, Path targetPath, String authorization) {
        try {
            if (ds == null) throw new IllegalArgumentException("DataSource is null");
            if (ds.getSource() == null || ds.getSource().isBlank()) {
                throw new IllegalArgumentException("DataSource.source is empty");
            }

            if (targetPath.getParent() != null) Files.createDirectories(targetPath.getParent());

            String source = ds.getSource().replace('\\', '/');

            String runId = ds.getRunId();
            if (runId == null || runId.isBlank()) {
                throw new IllegalStateException("Cannot download MLflow artifact: runId missing");
            }

            String artifactPath = extractArtifactPath(source);
            if (artifactPath == null || artifactPath.isBlank()) {
                throw new IllegalStateException("Cannot download MLflow artifact: artifact path empty. source=" + source);
            }

            HttpHeaders headers = new HttpHeaders();
            if (authorization != null && !authorization.isBlank()) {
                headers.set("Authorization", authorization);
            } else if (mlflowTrackingToken != null && !mlflowTrackingToken.isBlank()) {
                headers.set("Authorization", "Bearer " + mlflowTrackingToken);
            }

            String runsGetUrl = UriComponentsBuilder
                    .fromHttpUrl(mlflowTrackingUrl)
                    .path("/api/2.0/mlflow/runs/get")
                    .queryParam("run_id", runId)
                    .build()
                    .toUriString();

            ResponseEntity<Map> runResp = restTemplate.exchange(
                    runsGetUrl, HttpMethod.GET, new HttpEntity<>(headers), Map.class);

            if (!runResp.getStatusCode().is2xxSuccessful() || runResp.getBody() == null) {
                throw new RuntimeException("MLflow runs/get failed: " + runResp.getStatusCode());
            }

            String artifactUri = extractArtifactUri(runResp.getBody());
            if (artifactUri == null || artifactUri.isBlank()) {
                throw new RuntimeException("MLflow runs/get returned empty artifact_uri for runId=" + runId);
            }

            log.info("MLflow artifact_uri for run {}: {}", runId, artifactUri);

            if (artifactUri.startsWith("file:")) {
                Path root = Paths.get(URI.create(artifactUri));
                Path sourcePath = root.resolve(artifactPath).normalize();

                log.info("Copying MLflow artifact from filesystem: {} -> {}", sourcePath, targetPath);

                if (!Files.exists(sourcePath)) {
                    throw new RuntimeException("Artifact not found on filesystem at: " + sourcePath);
                }

                Path tmp = targetPath.resolveSibling(targetPath.getFileName().toString() + ".part");
                Files.copy(sourcePath, tmp, StandardCopyOption.REPLACE_EXISTING);
                Files.move(tmp, targetPath, StandardCopyOption.REPLACE_EXISTING);

                return targetPath.toString();
            }

            if (artifactUri.startsWith("mlflow-artifacts:")) {
                String p = artifactPath.replace('\\', '/');

                String downloadUrl = UriComponentsBuilder.fromHttpUrl(mlflowTrackingUrl)
                        .path("/get-artifact")
                        .queryParam("run_id", runId)
                        .queryParam("path", p)
                        .build()
                        .toUriString();

                log.info("Downloading MLflow artifact: {}", downloadUrl);

                ResponseEntity<Resource> resp = restTemplate.exchange(
                        downloadUrl, HttpMethod.GET, new HttpEntity<>(headers), Resource.class);

                if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                    throw new RuntimeException("MLflow get-artifact failed: status=" + resp.getStatusCode() + " url=" + downloadUrl);
                }

                writeResourceToFile(resp.getBody(), targetPath);
                return targetPath.toString();
            }

            throw new RuntimeException("Unsupported artifact_uri scheme: " + artifactUri);

        } catch (Exception e) {
            log.error("Failed to download MLflow artifact. source={} target={}",
                    ds != null ? ds.getSource() : null, targetPath, e);
            throw new RuntimeException("Failed to download MLflow artifact for source="
                    + (ds != null ? ds.getSource() : "null"), e);
        }
    }

    private String extractArtifactPath(String source) {
        String s = source.replace('\\', '/');
        int idx = s.indexOf("/artifacts/");
        if (idx >= 0) return s.substring(idx + "/artifacts/".length());
        if (s.startsWith("artifacts/")) return s.substring("artifacts/".length());
        return s;
    }

    private String extractArtifactUri(Map body) {
        Object runObj = body.get("run");
        if (!(runObj instanceof Map)) return null;
        Map run = (Map) runObj;

        Object infoObj = run.get("info");
        if (!(infoObj instanceof Map)) return null;
        Map info = (Map) infoObj;

        Object uri = info.get("artifact_uri");
        return uri != null ? uri.toString() : null;
    }
    private void writeResourceToFile(Resource resource, Path targetPath) throws Exception {
        Path tmp = targetPath.resolveSibling(targetPath.getFileName().toString() + ".part");
        try (InputStream in = resource.getInputStream()) {
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.move(tmp, targetPath, StandardCopyOption.REPLACE_EXISTING);
    }
}
