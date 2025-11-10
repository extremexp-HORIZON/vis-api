package gr.imsi.athenarc.xtremexpvisapi.config;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConcurrencyConfig {

    // Tunable parameters — adjust based on your machine and load testing
    private static final int DOWNLOAD_WORKER_THREADS = 20;
    private static final int DOWNLOAD_WORKER_QUEUE = 200;
    private static final int DUCKDB_CONCURRENT_PERMITS = 4; // limit concurrent DuckDB connections/queries

    @Bean(name = "dataProcessingExecutor")
    public Executor dataProcessingExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                DOWNLOAD_WORKER_THREADS,
                DOWNLOAD_WORKER_THREADS,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(DOWNLOAD_WORKER_QUEUE),
                new ThreadPoolExecutor.AbortPolicy() // reject when queue full
        );

        executor.allowCoreThreadTimeOut(false);
        return executor;
    }

    @Bean(name = "duckDbSemaphore")
    public Semaphore duckDbSemaphore() {
        return new Semaphore(DUCKDB_CONCURRENT_PERMITS);
    }
}
