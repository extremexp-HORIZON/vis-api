package gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.geojson;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Minimal GeoJSON geometry model (RFC 7946).
 *
 * Note: We only model the geometry types we currently need for zones.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = GeoJsonPoint.class, name = "Point"),
        @JsonSubTypes.Type(value = GeoJsonPolygon.class, name = "Polygon"),
        @JsonSubTypes.Type(value = GeoJsonCircle.class, name = "Circle")
})
public interface GeoJsonGeometry {
    String getType();
}

