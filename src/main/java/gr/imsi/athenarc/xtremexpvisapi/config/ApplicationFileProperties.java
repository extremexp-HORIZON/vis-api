package gr.imsi.athenarc.xtremexpvisapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@Data
@ConfigurationProperties(prefix = "app.file.cache")
public class ApplicationFileProperties {
    private String directory;
    private int duration;
    private String unit;
    private String size;
}
