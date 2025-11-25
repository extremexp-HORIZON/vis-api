package gr.imsi.athenarc.xtremexpvisapi.domain.sync;

import lombok.Data;

/**
 * Result object for sync operations.
 */
@Data
public class SyncResult {
    private boolean success;
    private int rowsInserted;
    private String errorMessage;
    private long startTime;
    private long endTime;
    private long durationMs;
}
