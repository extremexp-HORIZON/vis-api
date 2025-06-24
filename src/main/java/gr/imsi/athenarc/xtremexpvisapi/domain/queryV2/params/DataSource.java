package gr.imsi.athenarc.xtremexpvisapi.domain.queryv2.params;

import java.util.Map;

import lombok.Data;

@Data
public class DataSource {
    private String source;
    private String format;
    private SourceType type;
    private Map<String, String> credentials;
}
