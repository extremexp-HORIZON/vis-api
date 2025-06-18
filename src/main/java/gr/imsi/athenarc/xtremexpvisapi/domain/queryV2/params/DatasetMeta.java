package gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params;

import lombok.Data;

@Data
public class DatasetMeta {
    private String source; // e.g. "https://example.com/dataset.csv" || "/datasets/dataset.csv" || "datasetID"
    private String projectId; // e.g. "project123/test"
    private String fileName; // e.g. "dataset.csv"
    private SourceType type; // e.g. INTERNAL, EXTERNAL
}
