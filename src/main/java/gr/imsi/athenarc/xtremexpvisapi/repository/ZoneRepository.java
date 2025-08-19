package gr.imsi.athenarc.xtremexpvisapi.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.Zone;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Repository
public class ZoneRepository {

    @Value("${app.zone.directory:${user.home}/zones}")
    private String zoneDirectory;

    private final ObjectMapper objectMapper;

    public ZoneRepository() {
        this.objectMapper = new ObjectMapper();
        // Configure ObjectMapper to pretty-print JSON
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @PostConstruct
    private void init() {
        if (zoneDirectory == null || zoneDirectory.isBlank()) {
            throw new IllegalStateException("Zone directory is not configured properly.");
        }

        // Create the base directory if it doesn't exist
        try {
            Path basePath = Paths.get(zoneDirectory);
            if (!Files.exists(basePath)) {
                Files.createDirectories(basePath);
                System.out.println("Created Zone base directory: " + basePath);
            }
        } catch (IOException e) {
            System.err.println("Failed to create Zone base directory: " + zoneDirectory);
            e.printStackTrace();
            throw new IllegalStateException("Cannot create Zone directory: " + zoneDirectory, e);
        }

        System.out.println("Zone directory initialized: " + zoneDirectory);
    }

    /**
     * Find a Zone by its id within a specific fileName
     * 
     * @param fileName the fileName to search in
     * @param zoneId the zone id to search for
     * @return Optional containing the Zone if found, empty otherwise
     */
    public Optional<Zone> findByFileNameAndId(String fileName, String zoneId) {
        List<Zone> zones = findByFileName(fileName);
        return zones.stream()
                .filter(zone -> zoneId.equals(zone.getId()))
                .findFirst();
    }

    /**
     * Find all Zones for a specific fileName
     * 
     * @param fileName the fileName to search for
     * @return List of all Zones for the specified fileName
     */
    public List<Zone> findByFileName(String fileName) {
        Path zonePath = Paths.get(zoneDirectory, fileName + ".json");

        if (!Files.exists(zonePath)) {
            return new ArrayList<>();
        }

        try {
            Zone[] zones = objectMapper.readValue(zonePath.toFile(), Zone[].class);
            return List.of(zones);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read zones for fileName: " + fileName, e);
        }
    }

    /**
     * Find all Zones across all files
     * 
     * @return List of all Zones from all files
     */
    public List<Zone> findAll() {
        List<Zone> allZones = new ArrayList<>();

        Path basePath = Paths.get(zoneDirectory);
        if (!Files.exists(basePath) || !Files.isDirectory(basePath)) {
            return allZones;
        }

        try (Stream<Path> paths = Files.list(basePath)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(filePath -> {
                        try {
                            Zone[] zones = objectMapper.readValue(filePath.toFile(), Zone[].class);
                            allZones.addAll(List.of(zones));
                        } catch (IOException e) {
                            System.err.println("Failed to read zone file: " + filePath);
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException("Failed to read zone directory", e);
        }

        return allZones;
    }

    /**
     * Save a Zone to the appropriate fileName file
     * 
     * @param zone the Zone to save
     * @return the saved Zone
     */
    public Zone save(Zone zone) {
        if (zone.getFileName() == null || zone.getFileName().trim().isEmpty()) {
            throw new IllegalArgumentException("Zone fileName cannot be null or empty");
        }

        String fileName = zone.getFileName();
        Path zonePath = Paths.get(zoneDirectory, fileName + ".json");

        try {
            // Create the directory if it doesn't exist
            Files.createDirectories(zonePath.getParent());

            // Read existing zones or create new list
            List<Zone> zones = new ArrayList<>();
            if (Files.exists(zonePath)) {
                Zone[] existingZones = objectMapper.readValue(zonePath.toFile(), Zone[].class);
                zones = new ArrayList<>(List.of(existingZones));
            }

            // Update existing zone or add new one
            boolean updated = false;
            for (int i = 0; i < zones.size(); i++) {
                if (zone.getId() != null && zone.getId().equals(zones.get(i).getId())) {
                    zones.set(i, zone);
                    updated = true;
                    break;
                }
            }
            
            if (!updated) {
                zones.add(zone);
            }

            // Write the updated zones array back to file
            objectMapper.writeValue(zonePath.toFile(), zones);
            System.out.println("Saved Zone for fileName: " + fileName + ", zoneId: " + zone.getId());

            return zone;
        } catch (IOException e) {
            throw new RuntimeException("Failed to save zone for fileName: " + fileName, e);
        }
    }

    /**
     * Delete a Zone by its id within a specific fileName
     * 
     * @param fileName the fileName to search in
     * @param zoneId the zone id to delete
     * @return true if deletion was successful, false if the Zone doesn't exist
     */
    public boolean deleteByFileNameAndId(String fileName, String zoneId) {
        Path zonePath = Paths.get(zoneDirectory, fileName + ".json");

        if (!Files.exists(zonePath)) {
            return false;
        }

        try {
            // Read existing zones
            Zone[] existingZones = objectMapper.readValue(zonePath.toFile(), Zone[].class);
            List<Zone> zones = new ArrayList<>(List.of(existingZones));

            // Remove the zone with matching id
            boolean removed = zones.removeIf(zone -> zoneId.equals(zone.getId()));

            if (removed) {
                // Write the updated zones array back to file
                objectMapper.writeValue(zonePath.toFile(), zones);
                return true;
            }

            return false;
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete zone for fileName: " + fileName + ", zoneId: " + zoneId, e);
        }
    }

    /**
     * Delete all zones for a specific fileName
     * 
     * @param fileName the fileName to delete
     * @return true if deletion was successful, false if the file doesn't exist
     */
    public boolean deleteByFileName(String fileName) {
        Path zonePath = Paths.get(zoneDirectory, fileName + ".json");

        if (!Files.exists(zonePath)) {
            return false;
        }

        try {
            Files.delete(zonePath);
            return true;
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete zones file: " + fileName, e);
        }
    }

    /**
     * Check if zones exist for a specific fileName
     * 
     * @param fileName the fileName to check
     * @return true if zones exist for the fileName, false otherwise
     */
    public boolean existsByFileName(String fileName) {
        Path zonePath = Paths.get(zoneDirectory, fileName + ".json");
        return Files.exists(zonePath);
    }

    /**
     * Get the file path for a given fileName
     * 
     * @param fileName the fileName
     * @return Path to the zones JSON file
     */
    public Path getZoneFilePath(String fileName) {
        return Paths.get(zoneDirectory, fileName + ".json");
    }

    /**
     * Find zones by type across all files
     * 
     * @param type the type to search for
     * @return List of zones with the specified type
     */
    public List<Zone> findByType(String type) {
        return findAll().stream()
                .filter(zone -> type.equals(zone.getType()))
                .toList();
    }

    /**
     * Find zones by status across all files
     * 
     * @param status the status to search for
     * @return List of zones with the specified status
     */
    public List<Zone> findByStatus(String status) {
        return findAll().stream()
                .filter(zone -> status.equals(zone.getStatus()))
                .toList();
    }

    /**
     * Get all unique fileNames that have zones
     * 
     * @return List of unique fileNames
     */
    public List<String> getAllFileNames() {
        List<String> fileNames = new ArrayList<>();

        Path basePath = Paths.get(zoneDirectory);
        if (!Files.exists(basePath) || !Files.isDirectory(basePath)) {
            return fileNames;
        }

        try (Stream<Path> paths = Files.list(basePath)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(filePath -> {
                        String fileName = filePath.getFileName().toString().replace(".json", "");
                        fileNames.add(fileName);
                    });
        } catch (IOException e) {
            throw new RuntimeException("Failed to read zone directory", e);
        }

        return fileNames;
    }
}
