package gr.imsi.athenarc.xtremexpvisapi.domain.sync;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Sync statistics DTO.
 */
@Data
@AllArgsConstructor
public  class SyncStatistics {
    private final int totalSyncs;
    private final int successfulSyncs;
    private final int failedSyncs;
    private final long totalRowsSynced;
    private final SyncResult lastSyncResult;
    private final long lastSyncTime;
}