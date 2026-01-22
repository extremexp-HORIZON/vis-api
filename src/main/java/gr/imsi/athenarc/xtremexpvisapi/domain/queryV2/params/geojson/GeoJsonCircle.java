package gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.geojson;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GeoJsonCircle implements GeoJsonGeometry {
    private String type = "Circle";
    private List<Double> coordinates;
    private Double radius;
    
    public GeoJsonCircle(List<Double> coordinates, Double radius) {
        this.type = "Circle";
        this.coordinates = coordinates;
        this.radius = radius;
    }
}
