package gr.imsi.athenarc.xtremexpvisapi.domain.Metadata;

import gr.imsi.athenarc.xtremexpvisapi.domain.QueryParams.SourceType;
import lombok.Data;

@Data
public class MetadataRequest {

    private String datasetId;
    private SourceType type;

}
