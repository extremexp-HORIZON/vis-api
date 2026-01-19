package gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Zone {
    private String id; // Unique identifier for the zone - OPTIONAL (will be auto-generated if not provided)
    
    @NotBlank(message = "File name is required")
    private String fileName; // Name of the file that the zone was created from - REQUIRED
    
    @NotBlank(message = "Zone name is required")
    private String name; // Name of the zone - REQUIRED
    
    private String type; // Type of the zone - OPTIONAL
    
    private String description; // Description of the zone - OPTIONAL
    
    private String status; // Status of the zone - OPTIONAL
    
    private String createdAt; // Date and time the zone was created - OPTIONAL
    
    private Double[] heights; // Heights of the zone - OPTIONAL

    private String[] geohashes; // Geohashes of the zone - OPTIONAL

    private List<GeoPoint> coordinates; // Coordinates of the zone - OPTIONAL

    private Float radius; // Radius of the zone in meters (if the zone is a circle) - OPTIONAL

    private GeoPoint center; // Center of the zone (if the zone is a circle) - OPTIONAL
}
