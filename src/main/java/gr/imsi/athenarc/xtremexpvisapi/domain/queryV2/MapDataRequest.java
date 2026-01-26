package gr.imsi.athenarc.xtremexpvisapi.domain.queryV2;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.Map;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.aggregation.AggregationFunction;
import gr.imsi.athenarc.xtremexpvisapi.domain.Metadata.MetadataMapResponse;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.Rectangle;
import lombok.EqualsAndHashCode;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.geojson.GeoJsonFeature;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MapDataRequest extends DataRequest {
    AggregationFunction aggType;
    Rectangle rect;
    Map<String, String> categoricalFilters;
    List<String> groupByCols;
    String measureCol;
    Long from;
    Long to;
    MetadataMapResponse mapMetadata;
    GeoJsonFeature feature;

    public MapDataRequest(AggregationFunction aggType, Rectangle rect, Map<String, String> categoricalFilters, List<String> groupByCols, String measureCol, Long from, Long to, GeoJsonFeature feature) {
        this.aggType = aggType;
        this.rect = rect;
        this.categoricalFilters = categoricalFilters;
        this.groupByCols = groupByCols;
        this.measureCol = measureCol;
        this.from = from;
        this.to = to;
        this.feature = feature;
    }
}