package com.checkin.ingestion.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    /** One thread per file being ingested: reads + splits the CSV into batches. */
    @Bean("ingestionExecutor")
    public Executor ingestionExecutor(@Value("${ingestion.worker-threads:2}") int threads) {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(threads);
        ex.setMaxPoolSize(threads);
        ex.setQueueCapacity(50);
        ex.setThreadNamePrefix("ingest-");
        ex.setWaitForTasksToCompleteOnShutdown(true);
        ex.setAwaitTerminationSeconds(30);
        ex.initialize();
        return ex;
    }

    /**
     * Parses rows and writes batches. Fixed size (core == max) so the thread count is predictable
     * and never exceeds the DB connection pool. The producer already limits in-flight batches with a
     * semaphore, so the queue is only a buffer; CallerRuns is just a safety net.
     */
    @Bean("dbWorkerExecutor")
    public Executor dbWorkerExecutor(@Value("${ingestion.db-worker-threads:6}") int threads) {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(threads);
        ex.setMaxPoolSize(threads);
        ex.setQueueCapacity(64);
        ex.setThreadNamePrefix("db-worker-");
        ex.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        ex.setWaitForTasksToCompleteOnShutdown(true);
        ex.setAwaitTerminationSeconds(30);
        ex.initialize();
        return ex;
    }
}