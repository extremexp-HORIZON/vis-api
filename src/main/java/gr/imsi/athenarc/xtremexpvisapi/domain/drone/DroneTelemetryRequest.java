package gr.imsi.athenarc.xtremexpvisapi.domain.drone;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * Request DTO for querying drone telemetry data.
 * Supports filtering by drone ID, time range, geographic bounding box, and pagination.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DroneTelemetryRequest {
    
    /**
     * Optional: Filter by specific drone ID(s).
     * If null or empty, returns data for all drones.
     */
    private List<String> droneIds;
    
    /**
     * Optional: Filter by single drone ID (alternative to droneIds list).
     * If both droneId and droneIds are provided, droneId takes precedence.
     */
    private String droneId;
    
    /**
     * Optional: Start time for time range filter (ISO 8601 format).
     * If null, no lower time bound is applied.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant startTime;
    
    /**
     * Optional: End time for time range filter (ISO 8601 format).
     * If null, no upper time bound is applied.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant endTime;
    
    /**
     * Optional: Minimum latitude for geographic bounding box filter.
     * Must be used together with maxLat, minLon, and maxLon.
     */
    private Double minLat;
    
    /**
     * Optional: Maximum latitude for geographic bounding box filter.
     * Must be used together with minLat, minLon, and maxLon.
     */
    private Double maxLat;
    
    /**
     * Optional: Minimum longitude for geographic bounding box filter.
     * Must be used together with minLat, maxLat, and maxLon.
     */
    private Double minLon;
    
    /**
     * Optional: Maximum longitude for geographic bounding box filter.
     * Must be used together with minLat, maxLat, and minLon.
     */
    private Double maxLon;
    
    /**
     * Optional: Maximum number of results to return.
     * Default: 1000, Maximum: 10000
     */
    @Min(value = 1, message = "Limit must be at least 1")
    private Integer limit;
    
    /**
     * Optional: Number of results to skip (for pagination).
     * Default: 0
     */
    @Min(value = 0, message = "Offset must be non-negative")
    private Integer offset;
    
    /**
     * Optional: Sort order for results.
     * Options: "timestamp_asc", "timestamp_desc" (default: "timestamp_desc")
     */
    private String sortOrder;
}
