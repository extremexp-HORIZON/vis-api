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
}
