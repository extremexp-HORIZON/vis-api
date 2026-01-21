package gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.geojson;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GeoJsonPoint implements GeoJsonGeometry {
    private String type = "Point";
    private List<Double> coordinates;

    public GeoJsonPoint(List<Double> coordinates) {
        this.type = "Point";
        this.coordinates = coordinates;
    }
}

