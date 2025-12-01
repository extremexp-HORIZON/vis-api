package gr.imsi.athenarc.xtremexpvisapi.controller;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gr.imsi.athenarc.xtremexpvisapi.domain.drone.DroneData;
import gr.imsi.athenarc.xtremexpvisapi.domain.drone.DroneTelemetryRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.drone.DroneTelemetryResponse;
import gr.imsi.athenarc.xtremexpvisapi.service.drone.DroneDataService;

@RestController
@CrossOrigin
@RequestMapping("/api/drones")
public class DroneDataController {

    private static final Logger LOG = LoggerFactory.getLogger(DroneDataController.class);

    private final DroneDataService droneDataService;

    public DroneDataController(DroneDataService droneDataService) {
        this.droneDataService = droneDataService;
    }

    /**
     * Query drone telemetry data with filters.
     * Supports filtering by drone ID, time range, geographic bounding box, and pagination.
     * 
     * @param droneId Optional: Filter by single drone ID
     * @param droneIds Optional: Filter by multiple drone IDs (can be repeated: ?droneIds=id1&droneIds=id2)
     * @param startTime Optional: Start time for time range filter (ISO 8601 format)
     * @param endTime Optional: End time for time range filter (ISO 8601 format)
     * @param minLat Optional: Minimum latitude for geographic bounding box
     * @param maxLat Optional: Maximum latitude for geographic bounding box
     * @param minLon Optional: Minimum longitude for geographic bounding box
     * @param maxLon Optional: Maximum longitude for geographic bounding box
     * @param limit Optional: Maximum number of results (default: 1000, max: 10000)
     * @param offset Optional: Number of results to skip (default: 0)
     * @param sortOrder Optional: Sort order - "timestamp_asc" or "timestamp_desc" (default: "timestamp_desc")
     * @return DroneTelemetryResponse containing the telemetry data and pagination metadata
     */
    @GetMapping("/telemetry")
    public ResponseEntity<DroneTelemetryResponse> getTelemetry(
            @RequestParam(required = false) String droneId,
            @RequestParam(required = false) List<String> droneIds,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @RequestParam(required = false) Double minLat,
            @RequestParam(required = false) Double maxLat,
            @RequestParam(required = false) Double minLon,
            @RequestParam(required = false) Double maxLon,
            @RequestParam(required = false, defaultValue = "1000") Integer limit,
            @RequestParam(required = false, defaultValue = "0") Integer offset,
            @RequestParam(required = false, defaultValue = "timestamp_desc") String sortOrder) {
        
        LOG.info("Request for drone telemetry data - droneId: {}, droneIds: {}, startTime: {}, endTime: {}, " +
                "boundingBox: [{}, {}] x [{}, {}], limit: {}, offset: {}, sortOrder: {}",
                droneId, droneIds, startTime, endTime, minLat, maxLat, minLon, maxLon, limit, offset, sortOrder);
        
        try {
            // Validate and set defaults
            if (limit == null || limit <= 0) {
                limit = 1000; // Default limit
            }
            if (limit > 10000) {
                limit = 10000; // Maximum limit
            }
            if (offset == null || offset < 0) {
                offset = 0; // Default offset
            }
            
            // Build request object
            DroneTelemetryRequest request = DroneTelemetryRequest.builder()
                    .droneId(droneId)
                    .droneIds(droneIds)
                    .startTime(startTime)
                    .endTime(endTime)
                    .minLat(minLat)
                    .maxLat(maxLat)
                    .minLon(minLon)
                    .maxLon(maxLon)
                    .limit(limit)
                    .offset(offset)
                    .sortOrder(sortOrder)
                    .build();
            
            // Get data and count
            List<DroneData> data = droneDataService.getDroneData(request);
            int totalItems = droneDataService.countDroneData(request);
            
            // Build response
            DroneTelemetryResponse response = DroneTelemetryResponse.builder()
                    .data(data)
                    .totalItems(totalItems)
                    .querySize(data.size())
                    .offset(offset)
                    .limit(limit)
                    .hasMore((offset + data.size()) < totalItems)
                    .build();
            
            LOG.info("Returning {} telemetry records (total: {})", data.size(), totalItems);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            LOG.error("Error retrieving drone telemetry data", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(DroneTelemetryResponse.builder()
                            .data(List.of())
                            .totalItems(0)
                            .querySize(0)
                            .build());
        }
    }

    /**
     * Get the latest telemetry record for a specific drone.
     * 
     * @param droneId The drone ID
     * @return DroneData containing the latest telemetry, or 404 if not found
     */
    @GetMapping("/telemetry/{droneId}/latest")
    public ResponseEntity<DroneData> getLatestTelemetry(@PathVariable String droneId) {
        LOG.info("Request for latest telemetry for drone: {}", droneId);
        
        try {
            Optional<DroneData> latestData = droneDataService.getLatestDroneData(droneId);
            
            if (latestData.isPresent()) {
                LOG.info("Found latest telemetry for drone: {}", droneId);
                return ResponseEntity.ok(latestData.get());
            } else {
                LOG.warn("No telemetry data found for drone: {}", droneId);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            LOG.error("Error retrieving latest telemetry for drone: {}", droneId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get list of all unique drone IDs in the system.
     * 
     * @return List of drone IDs
     */
    @GetMapping("/list")
    public ResponseEntity<List<String>> getAllDroneIds() {
        LOG.info("Request for all drone IDs");
        
        try {
            List<String> droneIds = droneDataService.getAllDroneIds();
            LOG.info("Found {} unique drone IDs", droneIds.size());
            return ResponseEntity.ok(droneIds);
        } catch (Exception e) {
            LOG.error("Error retrieving drone IDs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Query telemetry data within a geographic bounding box.
     * 
     * @param minLat Minimum latitude
     * @param maxLat Maximum latitude
     * @param minLon Minimum longitude
     * @param maxLon Maximum longitude
     * @param limit Optional limit (default: 1000)
     * @param offset Optional offset (default: 0)
     * @return DroneTelemetryResponse containing telemetry data within the bounding box
     */
    @GetMapping("/telemetry/bounding-box")
    public ResponseEntity<DroneTelemetryResponse> getTelemetryByBoundingBox(
            @RequestParam Double minLat,
            @RequestParam Double maxLat,
            @RequestParam Double minLon,
            @RequestParam Double maxLon,
            @RequestParam(required = false, defaultValue = "1000") Integer limit,
            @RequestParam(required = false, defaultValue = "0") Integer offset) {
        
        LOG.info("Request for telemetry in bounding box: lat=[{}, {}], lon=[{}, {}]", 
                minLat, maxLat, minLon, maxLon);
        
        try {
            DroneTelemetryRequest request = DroneTelemetryRequest.builder()
                    .minLat(minLat)
                    .maxLat(maxLat)
                    .minLon(minLon)
                    .maxLon(maxLon)
                    .limit(limit)
                    .offset(offset)
                    .build();
            
            List<DroneData> data = droneDataService.getDroneData(request);
            int totalItems = droneDataService.countDroneData(request);
            
            DroneTelemetryResponse response = DroneTelemetryResponse.builder()
                    .data(data)
                    .totalItems(totalItems)
                    .querySize(data.size())
                    .offset(offset)
                    .limit(limit)
                    .hasMore((offset + data.size()) < totalItems)
                    .build();
            
            LOG.info("Returning {} telemetry records in bounding box (total: {})", data.size(), totalItems);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            LOG.error("Error retrieving telemetry by bounding box", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(DroneTelemetryResponse.builder()
                            .data(List.of())
                            .totalItems(0)
                            .querySize(0)
                            .build());
        }
    }

    /**
     * Export telemetry data to CSV.
     * @param droneId Optional: Filter by single drone ID
     * @param droneIds Optional: Filter by multiple drone IDs (can be repeated: ?droneIds=id1&droneIds=id2)
     * @param startTime Optional: Start time for time range filter (ISO 8601 format)
     * @param endTime Optional: End time for time range filter (ISO 8601 format)
     * @param minLat Optional: Minimum latitude for geographic bounding box
     * @param maxLat Optional: Maximum latitude for geographic bounding box
     * @param minLon Optional: Minimum longitude for geographic bounding box
     * @param maxLon Optional: Maximum longitude for geographic bounding box
     * @param limit Optional: Maximum number of results (default: 1000, max: 10000)
     * @param offset Optional: Number of results to skip (default: 0)
     * @param sortOrder Optional: Sort order - "timestamp_asc" or "timestamp_desc" (default: "timestamp_desc")
     * @return ResponseEntity with success message if successful, otherwise internal server error
     */
    @GetMapping("/telemetry/export")
    public ResponseEntity<String> exportTelemetryToCsv(
            @RequestParam(required = false) String droneId,
            @RequestParam(required = false) List<String> droneIds,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @RequestParam(required = false) Double minLat,
            @RequestParam(required = false) Double maxLat,
            @RequestParam(required = false) Double minLon,
            @RequestParam(required = false) Double maxLon,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset,
            @RequestParam(required = false) String sortOrder   ) {
        LOG.info("Request for exporting telemetry to CSV - droneId: {}, droneIds: {}, startTime: {}, endTime: {}, " +
                "boundingBox: [{}, {}] x [{}, {}], limit: {}, offset: {}, sortOrder: {}",
                droneId, droneIds, startTime, endTime, minLat, maxLat, minLon, maxLon, limit, offset, sortOrder);
        
        try {
            // Validate and set defaults - for export, we want all rows
            if (limit == null || limit <= 0) {
                limit = null; // Set to null so no LIMIT clause is added
            }
            if (offset == null || offset < 0) {
                offset = 0; // Default offset
            }
            DroneTelemetryRequest request = DroneTelemetryRequest.builder()
                    .droneId(droneId)
                    .droneIds(droneIds)
                    .startTime(startTime)
                    .endTime(endTime)
                    .minLat(minLat)
                    .maxLat(maxLat)
                    .minLon(minLon)
                    .maxLon(maxLon)
                    .limit(limit)
                    .offset(offset)
                    .sortOrder(sortOrder)
                    .build();
            Boolean success = droneDataService.exportTelemetryToCsv(request);
            if (success) {
                return ResponseEntity.ok("Telemetry exported to CSV successfully");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to export telemetry to CSV");
            }
        } catch (Exception e) {
            LOG.error("Error exporting telemetry to CSV", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
