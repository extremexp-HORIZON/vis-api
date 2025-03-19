package gr.imsi.athenarc.xtremexpvisapi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gr.imsi.athenarc.xtremexpvisapi.config.ApplicationFileProperties;
import lombok.extern.java.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Log
public class FileService {
    private static final List<String> UNITS = Arrays.asList("B", "KB", "MB", "GB", "TB", "PB", "EB");
    private final Map<String, String> fileCache = new HashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private final ApplicationFileProperties applicationFileProperties;
    private final ZenohService zenohService;

    @Autowired
    public FileService(ApplicationFileProperties applicationFileProperties, ZenohService zenohService) {
        this.applicationFileProperties = applicationFileProperties;
        this.zenohService = zenohService;
    }

    /**
     * Downloads a file from zenoh given a URI and schedules it for deletion after a
     * certain period.
     * If the file already exists in the cache, resets the deletion timer.
     *
     * @param uri the URI of the file to download
     * @throws Exception if an error occurs
     */
    public void downloadFileFromZenoh(String uri) throws Exception {
        // Check if uri has the correct form
        if (uri.chars().filter(ch -> ch == '/').count() < 3) {
            throw new IllegalArgumentException("Error: uri should have this form: \"UseCase/Folder/Subfolder/Filename\"");
        }
        // Extract UseCase, Folder, Subfolder, Filename from uri
        String[] uriParts = uri.split("/");
        String useCase = uriParts[0];
        String folder = uriParts[1];
        String subFolder = uriParts[2];
        String fileName = uriParts[uriParts.length - 1];
        if (fileCache.containsKey(fileName)) {
            log.info(fileName + " already exists in the cache");
            resetFileDeletion(fileName);
        } else {
            log.fine("Downloading " + fileName + " from " + uri);
            Path targetPath = Paths.get(applicationFileProperties.getDirectory(), uri);
            HttpResponse<InputStream> zenohResponse = zenohService.getSingleFile(useCase, folder, subFolder, fileName);
            log.fine("Download successful");
            
            // Get file from response body
            InputStream fileStream = zenohResponse.body();
            // Get file size from response headers
            long fileSize = zenohResponse.headers().firstValue("Content-Length").map(Long::parseLong).orElse(-1L);

            fileInsertionHandler(fileStream, fileSize, targetPath);
            fileCache.put(fileName, targetPath.toString());
            scheduleFileDeletion(targetPath);
        }
    }

    /**
     * Schedules the deletion of the file at the given path after X seconds.
     *
     * @param path the path of the file to delete
     */
    private void scheduleFileDeletion(Path path) {
        ScheduledFuture<?> scheduledTask = scheduler.schedule(() -> {
            try {
                Files.deleteIfExists(path);
                fileCache.remove(path.getFileName().toString());
                scheduledTasks.remove(path.getFileName().toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, applicationFileProperties.getDuration(),
                TimeUnit.valueOf(applicationFileProperties.getUnit()));

        scheduledTasks.put(path.getFileName().toString(), scheduledTask);
    }

    /**
     * Resets the deletion timer for the file with the given name.
     *
     * @param fileName the name of the file to reset the deletion timer for
     */
    private void resetFileDeletion(String fileName) {
        ScheduledFuture<?> scheduledTask = scheduledTasks.get(fileName);
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            Path path = Paths.get(fileCache.get(fileName));
            scheduleFileDeletion(path);
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
                log.warning("File insertion failed: File bigger than cache size limit");
            } else if (directoryCurrentSizeBytes + insertedFileSizeBytes > folderSizeLimitBytes) {
                log.warning("File insertion postponed: Deleting old files to make space");
                makeSpace(targetPath, directoryCurrentSizeBytes, insertedFileSizeBytes, folderSizeLimitBytes,
                        fileToBeInserted);
            } else {
                Files.createDirectories(targetPath.getParent());
                Files.copy(fileToBeInserted, targetPath, StandardCopyOption.REPLACE_EXISTING);
                log.finest("File inserted successfully");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Deletes files from the directory to make space for the new file (one by one).
     *
     * @param targetPath             path variable that defines the target directory.
     * @param directoryCurrentSizeBytes the current size of the directory in bytes.
     * @param insertedFileSizeBytes the size of the file to be inserted in bytes.
     * @param folderSizeLimitBytes   the size limit of the directory in bytes.
     * @param fileToBeInserted       the file to be inserted.
     * @throws IOException if an I/O error occurs
     */
    private void makeSpace(Path targetPath, long directoryCurrentSizeBytes, long insertedFileSizeBytes,
            long folderSizeLimitBytes, InputStream fileToBeInserted) throws IOException {
        // Get all files in the directory and sort them by last modified time (oldest -> newest)
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
            scheduledTasks.remove(file.getFileName().toString());
            spaceFreed += fileSize;
            log.info("Deleted file: " + file.getFileName() + " to free up space");

            if (directoryCurrentSizeBytes + insertedFileSizeBytes - spaceFreed <= folderSizeLimitBytes) {
                break;
            }
        }

        // Check if enough space was freed
        if (directoryCurrentSizeBytes + insertedFileSizeBytes - spaceFreed > folderSizeLimitBytes) {
            log.warning("File insertion failed: Not enough space could be freed");
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

}
