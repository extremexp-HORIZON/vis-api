package gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.geojson;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GeoJsonCircle implements GeoJsonGeometry {
    private String type = "Circle";
    private List<Double> coordinates;
    
    public GeoJsonCircle(List<Double> coordinates) {
        this.type = "Circle";
        this.coordinates = coordinates;
    }
}
