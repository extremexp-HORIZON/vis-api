package gr.imsi.athenarc.xtremexpvisapi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gr.imsi.athenarc.xtremexpvisapi.config.ApplicationFileProperties;
import lombok.extern.java.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
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

    @Autowired
    public FileService(ApplicationFileProperties applicationFileProperties) {
        this.applicationFileProperties = applicationFileProperties;
    }

    /**
     * Downloads a file from the given URI and schedules it for deletion after a
     * certain period.
     * If the file already exists in the cache, deletion timer gets reseted.
     *
     * @param uri the URI of the file to download
     * @throws IOException if an I/O error occurs
     */
    public void downloadFile(URI uri) throws IOException {
        downloadFile(uri.toURL());
    }

    /**
     * Downloads a file from the given URL and schedules it for deletion after a
     * certain period.
     * If the file already exists in the cache, the deletion is reset.
     *
     * @param url the URL of the file to download
     * @throws IOException if an I/O error occurs
     */
    public void downloadFile(URL url) throws IOException {
        String fileName = getFileNameFromURL(url);
        if (fileCache.containsKey(fileName)) {
            log.info(fileName + " already exists in the cache");
            resetFileDeletion(fileName);
        } else {
            log.fine("Downloading " + fileName + " from " + url);
            Path targetPath = Paths.get(applicationFileProperties.getDirectory(), fileName);
            try (InputStream incomingFile = url.openStream()) {
                fileInsertionHandler(incomingFile, targetPath);
            }
            fileCache.put(fileName, targetPath.toString());
            scheduleFileDeletion(targetPath);
        }
    }

    /**
     * Extracts the name of the file from the URL.
     * If there is no filename in the URL, a default name is used
     * ("downloaded_file").
     *
     * @param url the URL of the file to download
     * @throws IOException if an I/O error occurs
     */
    private String getFileNameFromURL(URL url) throws IOException {
        String fileName;
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setInstanceFollowRedirects(false);
        String disposition = connection.getHeaderField("Content-Disposition");
        if (disposition != null && disposition.contains("filename=")) {
            fileName = disposition.split("filename=")[1].replace("\"", "").trim();
        } else {
            fileName = "downloaded_file.exe";
        }
        connection.disconnect();
        return fileName;
    }

    /**
     * Schedules the deletion of the file at the given path after 20 seconds.
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
    private void fileInsertionHandler(InputStream fileToBeInserted, Path targetPath) {
        try {
            // Extract numbers and units from the folder size limit
            long limitNumber = Long.parseLong(applicationFileProperties.getSize().replaceAll("[^0-9]", ""));
            String limitUnit = applicationFileProperties.getSize().replaceAll("[^a-zA-Z]", "").toUpperCase();
            long directoryCurrentSizeBytes = getDirectorySize(targetPath.getParent());
            long folderSizeLimitBytes = convertToBytes(limitNumber, limitUnit);
            long insertedFileSizeBytes = fileToBeInserted.available();

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
