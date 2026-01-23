package gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.geojson;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Minimal GeoJSON Feature model (RFC 7946).
 *
 * Example:
 * {
 *   "type": "Feature",
 *   "geometry": { ... },
 *   "properties": { ... }
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GeoJsonFeature {
    /**
     * Must be "Feature" for a GeoJSON Feature.
     */
    @Builder.Default
    private String type = "Feature";

    /**
     * The GeoJSON geometry of this Feature.
     */
    private GeoJsonGeometry geometry;

    /**
     * Arbitrary properties associated with the Feature.
     */
    private Map<String, Object> properties;

    public GeoJsonFeature(GeoJsonGeometry geometry) {
        this.type = "Feature";
        this.geometry = geometry;
        this.properties = null;
    }

    public GeoJsonFeature(GeoJsonGeometry geometry, Map<String, Object> properties) {
        this.type = "Feature";
        this.geometry = geometry;
        this.properties = properties;
    }
}

