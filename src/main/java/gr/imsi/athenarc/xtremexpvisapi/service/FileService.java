package gr.imsi.athenarc.xtremexpvisapi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gr.imsi.athenarc.xtremexpvisapi.config.ApplicationFileProperties;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.io.InputStream;
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
    private static final List<String> UNITS = Arrays.asList("B", "KB", "MB", "GB", "TB", "PB", "EB");
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private final ApplicationFileProperties applicationFileProperties;

    @Autowired
    public FileService(ApplicationFileProperties applicationFileProperties) {
        this.applicationFileProperties = applicationFileProperties;
    }

    /**
     * Saves a file from an external source and schedules it for deletion after a
     * certain period.
     * If the file already exists in the cache, resets the deletion timer.
     *
     * @param uri the URI of the file to download
     * @throws Exception if an error occurs
     */
    public String saveFile(String folderStructure, String fileName, InputStream fileContent) throws Exception {

        String fileCacheDirectory = applicationFileProperties.getDirectory();
        Path targetPath = Paths.get(fileCacheDirectory, folderStructure, fileName);

        // Check if file is already cached and reset timer
        if (isFileCached(targetPath.toString())) {
            return targetPath.toString();
        }

        fileInsertionHandler(fileContent, fileContent.available(), targetPath);
        return targetPath.toString();
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

//             if (directoryCurrentSizeBytes + insertedFileSizeBytes - spaceFreed <= folderSizeLimitBytes) {
//                 break;
//             }
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

}
