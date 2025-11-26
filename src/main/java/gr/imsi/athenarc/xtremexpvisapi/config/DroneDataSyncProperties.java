package gr.imsi.athenarc.xtremexpvisapi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for drone data synchronization.
 */
@Data
@Component
@ConfigurationProperties(prefix = "drone.data.sync")
public class DroneDataSyncProperties {

    /**
     * Enable/disable automatic synchronization from JSONL to DuckDB.
     * Default: true
     */
    private boolean enabled = true;

    /**
     * Sync interval in milliseconds.
     * Default: 300000 (5 minutes)
     */
    private long interval = 300000;

    /**
     * Maximum number of records to process per sync cycle.
     * 0 = unlimited
     * Default: 0
     */
    private int maxRecordsPerSync = 0;

    /**
     * Enable detailed logging for sync operations.
     * Default: false
     */
    private boolean verboseLogging = false;

    /**
     * Retry configuration
     */
    private Retry retry = new Retry();

    @Data
    public static class Retry {
        /**
         * Maximum number of retry attempts on failure.
         * Default: 3
         */
        private int maxAttempts = 3;

        /**
         * Delay between retries in milliseconds.
         * Default: 60000 (1 minute)
         */
        private long delayMs = 60000;
    }
}
