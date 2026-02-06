package gr.imsi.athenarc.xtremexpvisapi.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import gr.imsi.athenarc.xtremexpvisapi.repository.ZoneRepository;
import lombok.extern.java.Log;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.Zone;

@Log
@Service
public class ZoneService {
    private final ZoneRepository zoneRepository;

    public ZoneService(ZoneRepository zoneRepository) {
        this.zoneRepository = zoneRepository;
    }

    /**
     * Check if a zone ID is empty (null, empty string, or just whitespace)
     * 
     * @param id the ID to check
     * @return true if the ID is empty, false otherwise
     */
    private boolean isIdEmpty(String id) {
        return id == null || id.trim().isEmpty();
    }

    /**
     * Save a zone to the repository
     * 
     * @param zone the zone to save
     * @return the saved zone
     * @throws RuntimeException if saving fails
     */
    public Zone save(Zone zone) {
        try {
            log.info("Saving zone: " + zone);
            
            if (zone == null) {
                throw new IllegalArgumentException("Zone cannot be null");
            }
            
            // Validate required fields
            validateRequiredFields(zone);
            
            // Generate ID if not provided
            if (isIdEmpty(zone.getId())) {
                String generatedId = UUID.randomUUID().toString();
                zone.setId(generatedId);
                log.info("Generated unique ID for zone: " + generatedId);
            }
            
            // Set default values for optional fields if not provided
            setDefaultValues(zone);
            
            Zone savedZone = zoneRepository.save(zone);
            log.info("Successfully saved zone with id: " + zone.getId() + " for fileName: " + zone.getFileName());
            return savedZone;
            
        } catch (IllegalArgumentException e) {
            log.severe("Validation error while saving zone: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            log.severe("Failed to save zone: " + zone + ". Error: " + e.getMessage());
            throw new RuntimeException("Failed to save zone", e);
        }
    }

    /**
     * Validate that all required fields are present
     * 
     * @param zone the zone to validate
     * @throws IllegalArgumentException if required fields are missing
     */
    private void validateRequiredFields(Zone zone) {
        if (isIdEmpty(zone.getFileName())) {
            throw new IllegalArgumentException("Zone fileName is required");
        }
        if (isIdEmpty(zone.getName())) {
            throw new IllegalArgumentException("Zone name is required");
        }
    }

    /**
     * Set default values for optional fields if they are not provided
     * 
     * @param zone the zone to set defaults for
     */
    private void setDefaultValues(Zone zone) {
        if (isIdEmpty(zone.getStatus())) {
            zone.setStatus("active"); // Default status
            log.info("Setting default status 'active' for zone: " + zone.getId());
        }
        if (isIdEmpty(zone.getCreatedAt())) {
            zone.setCreatedAt(LocalDateTime.now().toString()); // Default creation time
            log.info("Setting default creation time for zone: " + zone.getId());
        }
        if (isIdEmpty(zone.getType())) {
            zone.setType("general"); // Default type
            log.info("Setting default type 'general' for zone: " + zone.getId());
        }
        if (isIdEmpty(zone.getDescription())) {
            zone.setDescription("Zone created for " + zone.getFileName()); // Default description
            log.info("Setting default description for zone: " + zone.getId());
        }
    }

    /**
     * Find all zones across all files
     * 
     * @return list of all zones
     * @throws RuntimeException if retrieval fails
     */
    public List<Zone> findAll() {
        try {
            log.info("Finding all zones");
            List<Zone> zones = zoneRepository.findAll();
            log.info("Successfully retrieved " + zones.size() + " zones");
            return zones;
        } catch (Exception e) {
            log.severe("Failed to retrieve all zones. Error: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve all zones", e);
        }
    }

    /**
     * Find zones by fileName
     * 
     * @param fileName the fileName to search for
     * @return list of zones for the specified fileName
     * @throws RuntimeException if retrieval fails
     */
    public List<Zone> findByFileName(String fileName) {
        try {
            log.info("Finding zones by fileName: " + fileName);
            if (isIdEmpty(fileName)) {
                throw new IllegalArgumentException("FileName cannot be null or empty");
            }
            
            List<Zone> zones = zoneRepository.findByFileName(fileName);
            log.info("Successfully retrieved " + zones.size() + " zones for fileName: " + fileName);
            return zones;
        } catch (IllegalArgumentException e) {
            log.severe("Validation error while finding zones by fileName: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            log.severe("Failed to find zones by fileName: " + fileName + ". Error: " + e.getMessage());
            throw new RuntimeException("Failed to find zones by fileName: " + fileName, e);
        }
    }

    /**
     * Find zones by type across all files
     * 
     * @param type the type to search for
     * @return list of zones with the specified type
     * @throws RuntimeException if retrieval fails
     */
    public List<Zone> findByType(String type) {
        try {
            log.info("Finding zones by type: " + type);
            if (isIdEmpty(type)) {
                throw new IllegalArgumentException("Type cannot be null or empty");
            }
            
            List<Zone> zones = zoneRepository.findByType(type);
            log.info("Successfully retrieved " + zones.size() + " zones with type: " + type);
            return zones;
        } catch (IllegalArgumentException e) {
            log.severe("Validation error while finding zones by type: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            log.severe("Failed to find zones by type: " + type + ". Error: " + e.getMessage());
            throw new RuntimeException("Failed to find zones by type: " + type, e);
        }
    }

    /**
     * Find zones by status across all files
     * 
     * @param status the status to search for
     * @return list of zones with the specified status
     * @throws RuntimeException if retrieval fails
     */
    public List<Zone> findByStatus(String status) {
        try {
            log.info("Finding zones by status: " + status);
            if (isIdEmpty(status)) {
                throw new IllegalArgumentException("Status cannot be null or empty");
            }
            
            List<Zone> zones = zoneRepository.findByStatus(status);
            log.info("Successfully retrieved " + zones.size() + " zones with status: " + status);
            return zones;
        } catch (IllegalArgumentException e) {
            log.severe("Validation error while finding zones by status: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            log.severe("Failed to find zones by status: " + status + ". Error: " + e.getMessage());
            throw new RuntimeException("Failed to find zones by status: " + status, e);
        }
    }

    /**
     * Find a zone by fileName and id
     * 
     * @param fileName the fileName to search in
     * @param id the zone id to search for
     * @return optional containing the zone if found
     * @throws RuntimeException if retrieval fails
     */
    public Optional<Zone> findByFileNameAndId(String fileName, String id) {
        try {
            log.info("Finding zone by fileName: " + fileName + " and id: " + id);
            if (isIdEmpty(fileName)) {
                throw new IllegalArgumentException("FileName cannot be null or empty");
            }
            if (isIdEmpty(id)) {
                throw new IllegalArgumentException("Zone id cannot be null or empty");
            }
            
            Optional<Zone> zone = zoneRepository.findByFileNameAndId(fileName, id);
            if (zone.isPresent()) {
                log.info("Successfully found zone with id: " + id + " for fileName: " + fileName);
            } else {
                log.info("Zone not found with id: " + id + " for fileName: " + fileName);
            }
            return zone;
        } catch (IllegalArgumentException e) {
            log.severe("Validation error while finding zone by fileName and id: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            log.severe("Failed to find zone by fileName: " + fileName + " and id: " + id + ". Error: " + e.getMessage());
            throw new RuntimeException("Failed to find zone by fileName: " + fileName + " and id: " + id, e);
        }
    }

    /**
     * Delete a zone by fileName and id
     * 
     * @param fileName the fileName to search in
     * @param id the zone id to delete
     * @return true if deletion was successful, false otherwise
     * @throws RuntimeException if deletion fails
     */
    public boolean deleteByFileNameAndId(String fileName, String id) {
        try {
            log.info("Deleting zone by fileName: " + fileName + " and id: " + id);
            if (isIdEmpty(fileName)) {
                throw new IllegalArgumentException("FileName cannot be null or empty");
            }
            if (isIdEmpty(id)) {
                throw new IllegalArgumentException("Zone id cannot be null or empty");
            }
            
            boolean deleted = zoneRepository.deleteByFileNameAndId(fileName, id);
            if (deleted) {
                log.info("Successfully deleted zone with id: " + id + " for fileName: " + fileName);
            } else {
                log.info("Zone not found for deletion with id: " + id + " for fileName: " + fileName);
            }
            return deleted;
        } catch (IllegalArgumentException e) {
            log.severe("Validation error while deleting zone by fileName and id: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            log.severe("Failed to delete zone by fileName: " + fileName + " and id: " + id + ". Error: " + e.getMessage());
            throw new RuntimeException("Failed to delete zone by fileName: " + fileName + " and id: " + id, e);
        }
    }

    /**
     * Delete all zones for a specific fileName
     * 
     * @param fileName the fileName to delete
     * @return true if deletion was successful, false otherwise
     * @throws RuntimeException if deletion fails
     */
    public boolean deleteByFileName(String fileName) {
        try {
            log.info("Deleting all zones for fileName: " + fileName);
            if (isIdEmpty(fileName)) {
                throw new IllegalArgumentException("FileName cannot be null or empty");
            }
            
            boolean deleted = zoneRepository.deleteByFileName(fileName);
            if (deleted) {
                log.info("Successfully deleted all zones for fileName: " + fileName);
            } else {
                log.info("No zones found for deletion with fileName: " + fileName);
            }
            return deleted;
        } catch (IllegalArgumentException e) {
            log.severe("Validation error while deleting zones by fileName: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            log.severe("Failed to delete zones by fileName: " + fileName + ". Error: " + e.getMessage());
            throw new RuntimeException("Failed to delete zones by fileName: " + fileName, e);
        }
    }

    /**
     * Check if zones exist for a specific fileName
     * 
     * @param fileName the fileName to check
     * @return true if zones exist, false otherwise
     * @throws RuntimeException if check fails
     */
    public boolean existsByFileName(String fileName) {
        try {
            log.info("Checking if zones exist for fileName: " + fileName);
            if (isIdEmpty(fileName)) {
                throw new IllegalArgumentException("FileName cannot be null or empty");
            }
            
            boolean exists = zoneRepository.existsByFileName(fileName);
            log.info("Zones exist for fileName " + fileName + ": " + exists);
            return exists;
        } catch (IllegalArgumentException e) {
            log.severe("Validation error while checking zones existence by fileName: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            log.severe("Failed to check zones existence for fileName: " + fileName + ". Error: " + e.getMessage());
            throw new RuntimeException("Failed to check zones existence for fileName: " + fileName, e);
        }
    }

    /**
     * Get all unique fileNames that have zones
     * 
     * @return list of unique fileNames
     * @throws RuntimeException if retrieval fails
     */
    public List<String> getAllFileNames() {
        try {
            log.info("Getting all unique fileNames");
            List<String> fileNames = zoneRepository.getAllFileNames();
            log.info("Successfully retrieved " + fileNames.size() + " unique fileNames: " + fileNames);
            return fileNames;
        } catch (Exception e) {
            log.severe("Failed to retrieve all fileNames. Error: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve all fileNames", e);
        }
    }

    public List<Zone> importZonesFromFile(MultipartFile file, String fileName) {
        try {
            log.info("Importing zones from file: " + file + " for fileName: " + fileName);
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("File cannot be null or empty");
            }
            if (isIdEmpty(fileName)) {
                throw new IllegalArgumentException("FileName cannot be null or empty");
            }
            String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
            if (extension.equals("json") || extension.equals("geojson")) {
                // TODO: Later add support for kml files
                List<Zone> zones = zoneRepository.importZonesFromFile(file, fileName);
                log.info("Successfully imported " + zones.size() + " zones from file: " + file + " for fileName: " + fileName);
                return zones;
            } else {
                log.severe("Unsupported file type: " + file.getName());
                throw new IllegalArgumentException("Unsupported file type: " + file.getName());
            }
        }
        catch (IllegalArgumentException e) {
            log.severe("Validation error while importing zones from file: " + e.getMessage());
            throw e;
        }
        catch (Exception e) {
            log.severe("Failed to import zones from file: " + file + " for fileName: " + fileName + ". Error: " + e.getMessage());
            throw new RuntimeException("Failed to import zones from file", e);
        }
    }
}
