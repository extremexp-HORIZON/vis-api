package gr.imsi.athenarc.xtremexpvisapi.domain.queryv2.params;

import lombok.Data;

@Data
public class DataSource {
    private String source;
    private String format;
    private SourceType sourceType;
    private String fileName;
    private String runId;
    private String experimentId;
    // Optional metadata tuning flags (null = use default behavior)
    // When used in /api/data/meta requests, these can be set by the client
    // to skip heavy computations on large datasets.
    private Boolean includeSummary;      // default true
    private Boolean includeTotalItems;   // default true
    private Boolean detectDatasetType;   // default true
}
