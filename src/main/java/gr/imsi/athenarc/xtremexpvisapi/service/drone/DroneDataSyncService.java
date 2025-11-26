package gr.imsi.athenarc.xtremexpvisapi.service.drone;

import gr.imsi.athenarc.xtremexpvisapi.config.DroneDataSyncProperties;
import gr.imsi.athenarc.xtremexpvisapi.repository.drone.DroneDataRepository;
import gr.imsi.athenarc.xtremexpvisapi.domain.sync.SyncResult;
import gr.imsi.athenarc.xtremexpvisapi.domain.sync.SyncStatistics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for synchronizing drone telemetry data from JSONL to DuckDB.
 * Handles scheduled incremental syncs and error recovery.
 */
@Slf4j
@Service
@ConditionalOnProperty(
    prefix = "drone.data.sync",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class DroneDataSyncService {

    private final DroneDataRepository droneDataRepository;
    private final DroneDataSyncProperties syncProperties;

    // Statistics
    private final AtomicInteger totalSyncs = new AtomicInteger(0);
    private final AtomicInteger successfulSyncs = new AtomicInteger(0);
    private final AtomicInteger failedSyncs = new AtomicInteger(0);
    private final AtomicLong totalRowsSynced = new AtomicLong(0);
    private volatile SyncResult lastSyncResult;
    private volatile long lastSyncTime = 0;

    @Autowired
    public DroneDataSyncService(
            DroneDataRepository droneDataRepository,
            DroneDataSyncProperties syncProperties) {
        this.droneDataRepository = droneDataRepository;
        this.syncProperties = syncProperties;
    }

    /**
     * Scheduled task to perform incremental sync.
     * Runs at fixed interval configured in properties.
     */
    @Scheduled(fixedDelayString = "${drone.data.sync.interval:300000}")
    public void performIncrementalSync() {
        if (!syncProperties.isEnabled()) {
            log.debug("Sync is disabled, skipping");
            return;
        }

        log.info("Starting incremental sync from JSONL to DuckDB...");

        int attempt = 0;
        boolean success = false;

        while (attempt < syncProperties.getRetry().getMaxAttempts() && !success) {
            attempt++;

            try {
                SyncResult result = droneDataRepository.syncJsonlToDuckDB();
                lastSyncResult = result;
                lastSyncTime = System.currentTimeMillis();

                if (result.isSuccess()) {
                    success = true;
                    totalSyncs.incrementAndGet();
                    successfulSyncs.incrementAndGet();
                    totalRowsSynced.addAndGet(result.getRowsInserted());

                    if (syncProperties.isVerboseLogging() || result.getRowsInserted() > 0) {
                        log.info("Sync completed successfully: {} rows inserted in {} ms",
                                result.getRowsInserted(), result.getDurationMs());
                    }
                } else {
                    throw new RuntimeException("Sync failed: " + result.getErrorMessage());
                }

            } catch (SQLException e) {
                failedSyncs.incrementAndGet();
                log.error("Sync attempt {} failed: {}", attempt, e.getMessage());

                if (attempt < syncProperties.getRetry().getMaxAttempts()) {
                    try {
                        long delay = syncProperties.getRetry().getDelayMs();
                        log.info("Retrying sync in {} ms...", delay);
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Sync retry interrupted", ie);
                        break;
                    }
                } else {
                    log.error("All sync attempts failed", e);
                }
            } catch (Exception e) {
                failedSyncs.incrementAndGet();
                log.error("Unexpected error during sync", e);
                break;
            }
        }
    }

    /**
     * Get sync statistics.
     */
    public SyncStatistics getStatistics() {
        return new SyncStatistics(
            totalSyncs.get(),
            successfulSyncs.get(),
            failedSyncs.get(),
            totalRowsSynced.get(),
            lastSyncResult,
            lastSyncTime
        );
    }
}