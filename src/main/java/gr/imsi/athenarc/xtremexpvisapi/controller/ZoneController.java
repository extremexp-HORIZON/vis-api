package gr.imsi.athenarc.xtremexpvisapi.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import gr.imsi.athenarc.xtremexpvisapi.service.ZoneService;
import lombok.extern.java.Log;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.Zone;

@Log
@RestController
@RequestMapping("/api/zones")
@CrossOrigin(origins = "*")
public class ZoneController {

    private final ZoneService zoneService;

    public ZoneController(ZoneService zoneService) {
        this.zoneService = zoneService;
    }

    /**
     * Create or update a zone
     * POST /api/zones
     */
    @PostMapping
    public ResponseEntity<Zone> createZone(@Valid @RequestBody Zone zone) {
        try {
            log.info("REST: Creating/updating zone: " + zone);
            
            if (zone == null) {
                log.warning("REST: Zone creation failed: Request body is null");
                return ResponseEntity.badRequest().build();
            }
            
            Zone savedZone = zoneService.save(zone);
            log.info("REST: Successfully created/updated zone with id: " + savedZone.getId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(savedZone);
            
        } catch (IllegalArgumentException e) {
            log.warning("REST: Zone creation failed due to validation error: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.severe("REST: Zone creation failed due to system error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all zones across all files
     * GET /api/zones
     */
    @GetMapping
    public ResponseEntity<List<Zone>> getAllZones() {
        try {
            log.info("REST: Retrieving all zones");
            
            List<Zone> zones = zoneService.findAll();
            log.info("REST: Successfully retrieved " + zones.size() + " zones");
            
            return ResponseEntity.ok(zones);
            
        } catch (Exception e) {
            log.severe("REST: Failed to retrieve all zones: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get zones by fileName
     * GET /api/zones/file/{fileName}
     */
    @GetMapping("/file/{fileName}")
    public ResponseEntity<List<Zone>> getZonesByFileName(@PathVariable String fileName) {
        try {
            log.info("REST: Retrieving zones for fileName: " + fileName);
            
            if (fileName == null || fileName.trim().isEmpty()) {
                log.warning("REST: Invalid fileName parameter: " + fileName);
                return ResponseEntity.badRequest().build();
            }
            
            List<Zone> zones = zoneService.findByFileName(fileName);
            log.info("REST: Successfully retrieved " + zones.size() + " zones for fileName: " + fileName);
            
            return ResponseEntity.ok(zones);
            
        } catch (IllegalArgumentException e) {
            log.warning("REST: Failed to retrieve zones by fileName due to validation error: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.severe("REST: Failed to retrieve zones by fileName: " + fileName + ". Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get zones by type
     * GET /api/zones/type/{type}
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<Zone>> getZonesByType(@PathVariable String type) {
        try {
            log.info("REST: Retrieving zones by type: " + type);
            
            if (type == null || type.trim().isEmpty()) {
                log.warning("REST: Invalid type parameter: " + type);
                return ResponseEntity.badRequest().build();
            }
            
            List<Zone> zones = zoneService.findByType(type);
            log.info("REST: Successfully retrieved " + zones.size() + " zones with type: " + type);
            
            return ResponseEntity.ok(zones);
            
        } catch (IllegalArgumentException e) {
            log.warning("REST: Failed to retrieve zones by type due to validation error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.severe("REST: Failed to retrieve zones by type: " + type + ". Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get zones by status
     * GET /api/zones/status/{status}
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Zone>> getZonesByStatus(@PathVariable String status) {
        try {
            log.info("REST: Retrieving zones by status: " + status);
            
            if (status == null || status.trim().isEmpty()) {
                log.warning("REST: Invalid status parameter: " + status);
                return ResponseEntity.badRequest().build();
            }
            
            List<Zone> zones = zoneService.findByStatus(status);
            log.info("REST: Successfully retrieved " + zones.size() + " zones with status: " + status);
            
            return ResponseEntity.ok(zones);
            
        } catch (IllegalArgumentException e) {
            log.warning("REST: Failed to retrieve zones by status due to validation error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.severe("REST: Failed to retrieve zones by status: " + status + ". Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get a specific zone by fileName and id
     * GET /api/zones/file/{fileName}/id/{id}
     */
    @GetMapping("/file/{fileName}/id/{id}")
    public ResponseEntity<Zone> getZoneByFileNameAndId(
            @PathVariable String fileName, 
            @PathVariable String id) {
        try {
            log.info("REST: Retrieving zone with fileName: " + fileName + " and id: " + id);
            
            if (fileName == null || fileName.trim().isEmpty()) {
                log.warning("REST: Invalid fileName parameter: " + fileName);
                return ResponseEntity.badRequest().build();
            }
            if (id == null || id.trim().isEmpty()) {
                log.warning("REST: Invalid id parameter: " + id);
                return ResponseEntity.badRequest().build();
            }
            
            var zone = zoneService.findByFileNameAndId(fileName, id);
            
            if (zone.isPresent()) {
                log.info("REST: Successfully retrieved zone with id: " + id + " for fileName: " + fileName);
                return ResponseEntity.ok(zone.get());
            } else {
                log.info("REST: Zone not found with fileName: " + fileName + " and id: " + id);
                return ResponseEntity.notFound().build();
            }
            
        } catch (IllegalArgumentException e) {
            log.warning("REST: Failed to retrieve zone due to validation error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.severe("REST: Failed to retrieve zone with fileName: " + fileName + " and id: " + id + ". Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update a zone
     * PUT /api/zones/file/{fileName}/id/{id}
     */
    @PutMapping("/file/{fileName}/id/{id}")
    public ResponseEntity<Zone> updateZone(
            @PathVariable String fileName, 
            @PathVariable String id, 
            @RequestBody Zone zone) {
        try {
            log.info("REST: Updating zone with fileName: " + fileName + " and id: " + id);
            
            if (fileName == null || fileName.trim().isEmpty()) {
                log.warning("REST: Invalid fileName parameter: " + fileName);
                return ResponseEntity.badRequest().build();
            }
            if (id == null || id.trim().isEmpty()) {
                log.warning("REST: Invalid id parameter: " + id);
                return ResponseEntity.badRequest().build();
            }
            if (zone == null) {
                log.warning("REST: Zone update failed: Request body is null");
                return ResponseEntity.badRequest().build();
            }
            
            // Ensure the zone has the correct fileName and id
            zone.setFileName(fileName);
            zone.setId(id);
            
            Zone updatedZone = zoneService.save(zone);
            log.info("REST: Successfully updated zone with id: " + id + " for fileName: " + fileName);
            
            return ResponseEntity.ok(updatedZone);
            
        } catch (IllegalArgumentException e) {
            log.warning("REST: Zone update failed due to validation error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.severe("REST: Zone update failed due to system error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete a specific zone
     * DELETE /api/zones/file/{fileName}/id/{id}
     */
    @DeleteMapping("/file/{fileName}/id/{id}")
    public ResponseEntity<Void> deleteZone(
            @PathVariable String fileName, 
            @PathVariable String id) {
        try {
            log.info("REST: Deleting zone with fileName: " + fileName + " and id: " + id);
            
            if (fileName == null || fileName.trim().isEmpty()) {
                log.warning("REST: Invalid fileName parameter: " + fileName);
                return ResponseEntity.badRequest().build();
            }
            if (id == null || id.trim().isEmpty()) {
                log.warning("REST: Invalid id parameter: " + id);
                return ResponseEntity.badRequest().build();
            }
            
            boolean deleted = zoneService.deleteByFileNameAndId(fileName, id);
            
            if (deleted) {
                log.info("REST: Successfully deleted zone with id: " + id + " for fileName: " + fileName);
                return ResponseEntity.noContent().build();
            } else {
                log.info("REST: Zone not found for deletion with fileName: " + fileName + " and id: " + id);
                return ResponseEntity.notFound().build();
            }
            
        } catch (IllegalArgumentException e) {
            log.warning("REST: Zone deletion failed due to validation error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.severe("REST: Zone deletion failed due to system error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete all zones for a specific fileName
     * DELETE /api/zones/file/{fileName}
     */
    @DeleteMapping("/file/{fileName}")
    public ResponseEntity<Void> deleteAllZonesByFileName(@PathVariable String fileName) {
        try {
            log.info("REST: Deleting all zones for fileName: " + fileName);
            
            if (fileName == null || fileName.trim().isEmpty()) {
                log.warning("REST: Invalid fileName parameter: " + fileName);
                return ResponseEntity.badRequest().build();
            }
            
            boolean deleted = zoneService.deleteByFileName(fileName);
            
            if (deleted) {
                log.info("REST: Successfully deleted all zones for fileName: " + fileName);
                return ResponseEntity.noContent().build();
            } else {
                log.info("REST: No zones found for deletion with fileName: " + fileName);
                return ResponseEntity.notFound().build();
            }
            
        } catch (IllegalArgumentException e) {
            log.warning("REST: Zone deletion failed due to validation error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.severe("REST: Zone deletion failed due to system error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Check if zones exist for a specific fileName
     * GET /api/zones/exists/{fileName}
     */
    @GetMapping("/exists/{fileName}")
    public ResponseEntity<Boolean> checkZonesExist(@PathVariable String fileName) {
        try {
            log.info("REST: Checking if zones exist for fileName: " + fileName);
            
            if (fileName == null || fileName.trim().isEmpty()) {
                log.warning("REST: Invalid fileName parameter: " + fileName);
                return ResponseEntity.badRequest().build();
            }
            
            boolean exists = zoneService.existsByFileName(fileName);
            log.info("REST: Zones exist for fileName " + fileName + ": " + exists);
            
            return ResponseEntity.ok(exists);
            
        } catch (IllegalArgumentException e) {
            log.warning("REST: Failed to check zones existence due to validation error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.severe("REST: Failed to check zones existence for fileName: " + fileName + ". Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all unique fileNames that have zones
     * GET /api/zones/fileNames
     */
    @GetMapping("/fileNames")
    public ResponseEntity<List<String>> getAllFileNames() {
        try {
            log.info("REST: Retrieving all unique fileNames");
            
            List<String> fileNames = zoneService.getAllFileNames();
            log.info("REST: Successfully retrieved " + fileNames.size() + " unique fileNames");
            
            return ResponseEntity.ok(fileNames);
            
        } catch (Exception e) {
            log.severe("REST: Failed to retrieve all fileNames: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get the file path for a given fileName
     * GET /api/zones/filePath/{fileName}
     */
    @GetMapping("/filePath/{fileName}")
    public ResponseEntity<String> getZoneFilePath(@PathVariable String fileName) {
        try {
            log.info("REST: Getting zone file path for fileName: " + fileName);
            
            if (fileName == null || fileName.trim().isEmpty()) {
                log.warning("REST: Invalid fileName parameter: " + fileName);
                return ResponseEntity.badRequest().build();
            }
            
            String filePath = zoneService.getZoneFilePath(fileName);
            log.info("REST: Zone file path for fileName " + fileName + ": " + filePath);
            
            return ResponseEntity.ok(filePath);
            
        } catch (IllegalArgumentException e) {
            log.warning("REST: Failed to get zone file path due to validation error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.severe("REST: Failed to get zone file path for fileName: " + fileName + ". Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Health check endpoint
     * GET /api/zones/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        log.info("REST: Health check requested");
        return ResponseEntity.ok("Zone service is running");
    }
}
