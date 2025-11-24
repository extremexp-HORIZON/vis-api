package gr.imsi.athenarc.xtremexpvisapi.domain.drone;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * Response DTO for drone telemetry queries.
 * Contains the telemetry data along with pagination metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DroneTelemetryResponse {
    
    /**
     * List of drone telemetry records matching the query.
     */
    private List<DroneData> data;
    
    /**
     * Total number of records matching the query (before pagination).
     */
    private Integer totalItems;
    
    /**
     * Number of records returned in this response (after pagination).
     */
    private Integer querySize;
    
    /**
     * Current page offset (from request).
     */
    private Integer offset;
    
    /**
     * Current page limit (from request).
     */
    private Integer limit;
    
    /**
     * Indicates if there are more results available.
     */
    private Boolean hasMore;
}
