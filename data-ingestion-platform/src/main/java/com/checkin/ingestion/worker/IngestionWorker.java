package com.checkin.ingestion.worker;

import com.checkin.ingestion.files.TaskService;
import com.checkin.ingestion.files.UploadTask;
import com.checkin.ingestion.files.UploadTaskRepository;
import com.checkin.ingestion.stock.StockCacheEvictor;
import com.opencsv.CSVReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionWorker {

    private final TaskService taskService;
    private final CsvBatchProcessor batchProcessor;
    private final CsvRowParser parser;
    private final UploadTaskRepository uploadTaskRepository;
    private final StockCacheEvictor stockCacheEvictor; 

    @Value("${ingestion.batch-size:5000}")
    private int batchSize;

    @Value("${ingestion.db-worker-threads:6}")
    private int dbWorkerThreads;

    @Async("ingestionExecutor")
    public void process(UUID taskId) {
        String threadName = Thread.currentThread().getName();

        if (!taskService.claim(taskId)) {
            log.info("[{}] Task {} is not PENDING (already claimed?), skipping", threadName, taskId);
            return;
        }

        log.info("[{}] Task {} started processing.", threadName, taskId);
        long start = System.currentTimeMillis();

        try {
            UploadTask task = uploadTaskRepository.findById(taskId).orElse(null);
            if (task == null) {
                log.error("[{}] Task {} FAILED. Task not found.", threadName, taskId);
                taskService.markFailed(taskId, "Task not found");
                return;
            }
            Path path = Path.of(task.getFilePath());

            // Quick estimate for the progress bar (raw byte scan, no decoding).
            taskService.setTotalRows(taskId, countDataRows(path));

            IngestionCounters c = ingest(taskId, path, threadName);

            // Exact number (the estimate is off if quoted fields contain line breaks).
            taskService.setTotalRows(taskId, c.processed.get());
            taskService.markCompleted(taskId);
            Files.deleteIfExists(path);

            long ms = Math.max(1, System.currentTimeMillis() - start);
            log.info("[{}] Task {} COMPLETED: {} rows, {} errors, {} ms, {} rows/sec",
                    threadName, taskId, c.processed.get(), c.errors.get(), ms, (c.processed.get() * 1000) / ms);

        } catch (Exception e) {
            log.error("[{}] Task {} FAILED", threadName, taskId, e);
            taskService.markFailed(taskId,
                    truncate(Objects.toString(e.getMessage(), e.getClass().getSimpleName()), 500));
        }
        stockCacheEvictor.evictAll(); 
    }

    /**
     * Producer: reads the CSV on this thread and hands batches to the db-worker pool.
     * - At most maxInFlight batches exist at once (semaphore), so memory stays bounded and the
     *   reader never ends up doing DB work itself.
     * - If any batch fails, the reader stops, waits for the running batches, then fails the task.
     */
    private IngestionCounters ingest(UUID taskId, Path path, String threadName) throws Exception {
        IngestionCounters c = new IngestionCounters();
        int maxInFlight = Math.max(2, dbWorkerThreads * 2);
        Semaphore inFlight = new Semaphore(maxInFlight);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        try (CSVReader reader = new CSVReader(open(path))) {
            String[] header = reader.readNext();
            if (header == null) {
                throw new IllegalArgumentException("File is empty");
            }
            Map<String, Integer> idx = parser.mapHeader(header);

            List<String[]> batch = new ArrayList<>(batchSize);
            long rowsRead = 0;
            long batchStartRow = 1; // 1-based index of the first data row in this batch
            int batchCount = 0;
            String[] cols;

            while (failure.get() == null && (cols = reader.readNext()) != null) {
                batch.add(cols);
                rowsRead++;
                if (batch.size() >= batchSize) {
                    submit(taskId, batch, batchStartRow, idx, c, inFlight, failure);
                    batchCount++;
                    batchStartRow = rowsRead + 1;
                    batch = new ArrayList<>(batchSize);
                }
            }
            if (!batch.isEmpty() && failure.get() == null) {
                submit(taskId, batch, batchStartRow, idx, c, inFlight, failure);
                batchCount++;
            }
            log.info("[{}] Finished reading {} rows in {} batches, waiting for DB workers...",
                    threadName, rowsRead, batchCount);
        } finally {
            // Wait for every running batch (also when reading failed), so nothing writes after we return.
            inFlight.acquireUninterruptibly(maxInFlight);
            inFlight.release(maxInFlight);
        }

        Throwable t = failure.get();
        if (t != null) {
            throw (t instanceof Exception e) ? e : new RuntimeException(t);
        }
        return c;
    }

    private void submit(UUID taskId, List<String[]> batch, long startRow, Map<String, Integer> idx,
                        IngestionCounters c, Semaphore inFlight, AtomicReference<Throwable> failure)
            throws InterruptedException {
        inFlight.acquire(); // blocks the reader when the workers are saturated = backpressure
        try {
            batchProcessor.processBatch(taskId, batch, startRow, idx, c)
                    .whenComplete((ok, err) -> {
                        if (err != null) {
                            failure.compareAndSet(null, unwrap(err));
                        }
                        inFlight.release();
                    });
        } catch (RuntimeException e) { // e.g. executor rejected the task
            inFlight.release();
            throw e;
        }
    }

    private Throwable unwrap(Throwable t) {
        return (t instanceof CompletionException && t.getCause() != null) ? t.getCause() : t;
    }

    /** Counts '\n' bytes: no charset decoding, no String allocation. Data rows = lines - header. */
    private long countDataRows(Path path) throws IOException {
        long lines = 0;
        byte last = '\n';
        byte[] buf = new byte[1 << 20];
        try (InputStream in = Files.newInputStream(path)) {
            int n;
            while ((n = in.read(buf)) > 0) {
                for (int i = 0; i < n; i++) {
                    if (buf[i] == '\n') {
                        lines++;
                    }
                }
                last = buf[n - 1];
            }
        }
        if (last != '\n') {
            lines++; // final line without trailing newline
        }
        return Math.max(0, lines - 1);
    }

    private BufferedReader open(Path path) throws IOException {
        return new BufferedReader(new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8), 1 << 16);
    }

    private String truncate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}