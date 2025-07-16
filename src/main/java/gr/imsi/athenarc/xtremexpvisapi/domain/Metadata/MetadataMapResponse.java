package gr.imsi.athenarc.xtremexpvisapi.domain.Metadata;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MetadataMapResponse extends MetadataResponseV2 {
    public MetadataMapResponse(MetadataResponseV2 metadataResponse) {
        super(metadataResponse.getDatasetType(), metadataResponse.getFileNames(), metadataResponse.getOriginalColumns(),
                metadataResponse.getTotalItems(), metadataResponse.getUniqueColumnValues(),
                metadataResponse.isHasLatLonColumns(), metadataResponse.getTimeColumn(), metadataResponse.getSummary());
    }

    double xMin;
    double xMax;
    double yMin;
    double yMax;
    double queryXMin;
    double queryXMax;
    double queryYMin;
    double queryYMax;
    long timeMin;
    long timeMax;
    String measure0;
    String measure1;
    LinkedHashSet<String> dimensions = new LinkedHashSet<>();
    LinkedHashSet<String> measures = new LinkedHashSet<>();
    Map<String, List<String>> facets;
}
