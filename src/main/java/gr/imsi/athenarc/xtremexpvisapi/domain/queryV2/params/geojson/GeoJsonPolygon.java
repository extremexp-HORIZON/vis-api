package gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.geojson;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GeoJsonPolygon implements GeoJsonGeometry {
    private String type = "Polygon";

    /**
     * Polygon coordinates as a list of LinearRings.
     * Each LinearRing is a list of positions (must be closed).
     *
     * coordinates[ring][vertex] = [lon,lat]
     */
    private List<List<List<Double>>> coordinates;

    public GeoJsonPolygon(List<List<List<Double>>> coordinates) {
        this.type = "Polygon";
        this.coordinates = coordinates;
    }
}
