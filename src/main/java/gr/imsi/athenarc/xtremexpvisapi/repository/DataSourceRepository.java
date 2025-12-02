package gr.imsi.athenarc.xtremexpvisapi.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Repository
public class DataSourceRepository {

    @Value("${app.dataSource.directory}")
    private String dataSourceDirectory;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    private void init() {
        if (dataSourceDirectory == null || dataSourceDirectory.isBlank()) {
            throw new IllegalStateException("DataSource directory is not configured properly.");
        }

        // Create the base directory if it doesn't exist
        try {
            Path basePath = Paths.get(dataSourceDirectory);
            if (!Files.exists(basePath)) {
                Files.createDirectories(basePath);
                System.out.println("Created DataSource base directory: " + basePath);
            }
        } catch (IOException e) {
            System.err.println("Failed to create DataSource base directory: " + dataSourceDirectory);
            e.printStackTrace();
            throw new IllegalStateException("Cannot create DataSource directory: " + dataSourceDirectory, e);
        }

        System.out.println("DataSource directory initialized: " + dataSourceDirectory);
    }

    /**
     * Find a DataSource by its fileName
     * 
     * @param fileName the fileName to search for
     * @return Optional containing the DataSource if found, empty otherwise
     */
    public Optional<DataSource> findByFileName(String fileName) {
        Path dataSourcePath = Paths.get(dataSourceDirectory, fileName);

        if (!Files.exists(dataSourcePath) || !Files.isDirectory(dataSourcePath)) {
            return Optional.empty();
        }

        Path metadataPath = dataSourcePath.resolve("metadata").resolve(fileName + ".meta.json");

        if (!Files.exists(metadataPath)) {
            return Optional.empty();
        }

        try {
            DataSource dataSource = objectMapper.readValue(metadataPath.toFile(), DataSource.class);
            return Optional.of(dataSource);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read metadata for fileName: " + fileName, e);
        }
    }

    /**
     * Find all DataSources in the configured directory
     * 
     * @return List of all DataSources
     */
    public List<DataSource> findAll() {
        List<DataSource> dataSources = new ArrayList<>();

        Path basePath = Paths.get(dataSourceDirectory);
        if (!Files.exists(basePath) || !Files.isDirectory(basePath)) {
            return dataSources;
        }

        try (Stream<Path> paths = Files.list(basePath)) {
            paths.filter(Files::isDirectory)
                    .forEach(dirPath -> {
                        String fileName = dirPath.getFileName().toString();
                        findByFileName(fileName).ifPresent(dataSources::add);
                    });
        } catch (IOException e) {
            throw new RuntimeException("Failed to read data source directory", e);
        }

        return dataSources;
    }

    /**
     * Delete a DataSource by its fileName
     * 
     * @param fileName the fileName to delete
     * @return true if deletion was successful, false if the DataSource doesn't
     *         exist
     */
    public boolean deleteByFileName(String fileName) {
        Path dataSourcePath = Paths.get(dataSourceDirectory, fileName);

        if (!Files.exists(dataSourcePath) || !Files.isDirectory(dataSourcePath)) {
            return false;
        }

        try {
            // Delete the entire directory and its contents
            deleteDirectoryRecursively(dataSourcePath);
            return true;
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete data source: " + fileName, e);
        }
    }

    /**
     * Insert a new DataSource
     * 
     * @param dataSource the DataSource to insert
     * @return the inserted DataSource
     */
    public DataSource insert(DataSource dataSource) {
        if (dataSource.getFileName() == null || dataSource.getFileName().trim().isEmpty()) {
            throw new IllegalArgumentException("DataSource fileName cannot be null or empty");
        }

        String fileName = dataSource.getFileName();
        Path dataSourcePath = Paths.get(dataSourceDirectory, fileName);

        try {
            // Create the main directory
            Files.createDirectories(dataSourcePath);

            // Create dataset directory
            Path datasetPath = dataSourcePath.resolve("dataset");
            Files.createDirectories(datasetPath);

            // Create metadata directory
            Path metadataPath = dataSourcePath.resolve("metadata");
            Files.createDirectories(metadataPath);

            // Step 1: Copy the source file to the dataset directory (if it exists)
            Path sourceFilePath = Paths.get(dataSource.getSource());
            Path targetFilePath = datasetPath.resolve(fileName + "." + dataSource.getFormat());

            // Only try to copy if the source is a valid file path and exists
            if (dataSource.getSource() != null && !dataSource.getSource().startsWith("Uploaded file:")
                    && Files.exists(sourceFilePath)) {
                Files.copy(sourceFilePath, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Copied source file from " + sourceFilePath + " to " + targetFilePath);
                dataSource.setSource(targetFilePath.toString());
            } else if (dataSource.getSource() != null && dataSource.getSource().startsWith("Uploaded file:")) {
                // This is an upload case, the file will be copied by the service layer
                System.out.println("Upload case detected, file will be handled by service layer");
            } else {
                System.out.println(
                        "Warning: Source file does not exist or is not a valid path: " + dataSource.getSource());
            }

            // Write the DataSource object as metadata
            Path metadataFile = metadataPath.resolve(fileName + ".meta.json");
            objectMapper.writeValue(metadataFile.toFile(), dataSource);
            System.out.println("Saved DataSource metadata for: " + fileName);

            return dataSource;
        } catch (IOException e) {
            throw new RuntimeException("Failed to insert data source: " + fileName, e);
        }
    }

    /**
     * Check if a DataSource exists by fileName
     * 
     * @param fileName the fileName to check
     * @return true if the DataSource exists, false otherwise
     */
    public boolean existsByFileName(String fileName) {
        Path dataSourcePath = Paths.get(dataSourceDirectory, fileName);
        return Files.exists(dataSourcePath) && Files.isDirectory(dataSourcePath);
    }

    /**
     * Get the dataset file path for a given fileName
     * 
     * @param fileName the fileName
     * @return Path to the dataset CSV file
     */
    public Path getDatasetFilePath(String fileName) {
        return Paths.get(dataSourceDirectory, fileName, "dataset", fileName + ".csv");
    }

    /**
     * Get the metadata file path for a given fileName
     * 
     * @param fileName the fileName
     * @return Path to the metadata JSON file
     */
    public Path getMetadataFilePath(String fileName) {
        return Paths.get(dataSourceDirectory, fileName, "metadata", fileName + ".meta.json");
    }

    /**
     * Helper method to recursively delete a directory and its contents
     * 
     * @param path the path to delete
     * @throws IOException if deletion fails
     */
    private void deleteDirectoryRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (Stream<Path> entries = Files.list(path)) {
                entries.forEach(entry -> {
                    try {
                        deleteDirectoryRecursively(entry);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to delete directory entry: " + entry, e);
                    }
                });
            }
        }
        Files.delete(path);
    }
}