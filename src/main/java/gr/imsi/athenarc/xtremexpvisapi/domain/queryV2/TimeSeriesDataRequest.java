package gr.imsi.athenarc.xtremexpvisapi.domain.queryV2;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Map;

import gr.imsi.athenarc.xtremexpvisapi.domain.Metadata.MetadataMapResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.Rectangle;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.geojson.GeoJsonFeature;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TimeSeriesDataRequest extends DataRequest {
    long frequency;
    Rectangle rect;
    Map<String, String> categoricalFilters;
    String measureCol;
    Long from;
    Long to;
    MetadataMapResponse mapMetadata;
    GeoJsonFeature feature;

    public TimeSeriesDataRequest(long frequency, Rectangle rect, Map<String, String> categoricalFilters, String measureCol, Long from, Long to, GeoJsonFeature feature) {
        this.frequency = frequency;
        this.rect = rect;
        this.categoricalFilters = categoricalFilters;
        this.measureCol = measureCol;
        this.from = from;
        this.to = to;
        this.feature = feature;
    }
}