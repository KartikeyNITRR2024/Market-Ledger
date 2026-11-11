package com.checkin.ingestion.worker;

import com.checkin.ingestion.stock.IngestionError;
import com.checkin.ingestion.stock.StockData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsvBatchProcessor {

    private static final int MAX_LOCK_RETRIES = 3;

    private final BatchWriter batchWriter;
    private final CsvRowParser parser;

    @Value("${ingestion.max-stored-errors:500}")
    private int maxStoredErrors;

    /**
     * Runs on the dbWorkerExecutor pool: parse -> write -> bump progress.
     * An exception thrown here completes the returned future exceptionally; the producer notices
     * it and stops reading the file.
     */
    @Async("dbWorkerExecutor")
    public CompletableFuture<Void> processBatch(
            UUID taskId,
            List<String[]> rawBatch,
            long startRow,
            Map<String, Integer> idx,
            IngestionCounters c) {

        List<StockData> rows = new ArrayList<>(rawBatch.size());
        List<IngestionError> errors = new ArrayList<>();
        long errorsInBatch = 0;

        // 1) parse + validate (CPU only, no DB)
        for (int i = 0; i < rawBatch.size(); i++) {
            String[] cols = rawBatch.get(i);
            if (cols.length == 1 && cols[0].isBlank()) {
                continue;
            }
            long rowNum = startRow + i + 1; // +1 because row 1 is the header
            try {
                rows.add(parser.parse(rowNum, cols, idx, taskId));
            } catch (IllegalArgumentException e) {
                errorsInBatch++;
                addError(c, errors, error(taskId, rowNum, raw(cols), e.getMessage()));
            }
        }

        // 2) write the whole batch in one transaction (fast path)
        try {
            withLockRetry(() -> batchWriter.writeBatch(taskId, rows, errors));
        } catch (DataAccessException ex) {
            log.warn("[{}] batch of {} rows rejected ({}), isolating bad rows",
                    Thread.currentThread().getName(), rows.size(), ex.getMostSpecificCause().getMessage());
            // The failed transaction rolled back everything, including the parse errors, so save them now.
            errorsInBatch += recoverFromBatchFailure(taskId, rows, ex, errors, c);
            batchWriter.writeErrors(taskId, errors);
        }

        // 3) progress: counters in memory + delta in the DB (data is already committed)
        long processedInBatch = rawBatch.size();
        c.processed.addAndGet(processedInBatch);
        c.errors.addAndGet(errorsInBatch);
        try {
            batchWriter.addProgress(taskId, processedInBatch, errorsInBatch);
        } catch (DataAccessException e) {
            log.warn("Task {}: could not update progress ({}); data for this batch is committed",
                    taskId, e.getMostSpecificCause().getMessage());
        }

        return CompletableFuture.completedFuture(null);
    }

    // ------------------------------------------------------------------ failure handling

    /**
     * Binary-split instead of row-by-row: k bad rows in n cost about k*log2(n) transactions
     * instead of n. Returns how many rows the DB rejected.
     */
    private int recoverFromBatchFailure(UUID taskId, List<StockData> rows, DataAccessException ex,
                                        List<IngestionError> errors, IngestionCounters c) {
        // Database down / no connection: this is not a "bad row", fail the task instead of
        // recording every row as rejected.
        if (ex instanceof DataAccessResourceFailureException || rows.isEmpty()) {
            throw ex;
        }
        if (rows.size() == 1) {
            StockData r = rows.get(0);
            addError(c, errors, error(taskId, r.getRowNum(),
                    r.getTicker() + "," + r.getTradeDate(),
                    "Database rejected row: " + truncate(ex.getMostSpecificCause().getMessage(), 300)));
            return 1;
        }
        int mid = rows.size() / 2;
        return writeIsolating(taskId, rows.subList(0, mid), errors, c)
                + writeIsolating(taskId, rows.subList(mid, rows.size()), errors, c);
    }

    private int writeIsolating(UUID taskId, List<StockData> rows, List<IngestionError> errors, IngestionCounters c) {
        try {
            withLockRetry(() -> batchWriter.writeRows(taskId, rows));
            return 0;
        } catch (DataAccessException ex) {
            return recoverFromBatchFailure(taskId, rows, ex, errors, c);
        }
    }

    /** Deadlock / lock timeout / serialization failure: roll back already happened, just try again. */
    private void withLockRetry(Runnable write) {
        for (int attempt = 1; ; attempt++) {
            try {
                write.run();
                return;
            } catch (ConcurrencyFailureException ex) {
                if (attempt >= MAX_LOCK_RETRIES || !backoff(attempt)) {
                    throw ex;
                }
                log.debug("Lock conflict (attempt {}), retrying: {}", attempt, ex.getMessage());
            }
        }
    }

    private boolean backoff(int attempt) {
        try {
            Thread.sleep(20L * attempt + ThreadLocalRandom.current().nextInt(20));
            return true;
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    // ------------------------------------------------------------------ helpers

    private void addError(IngestionCounters c, List<IngestionError> errors, IngestionError error) {
        // incrementAndGet is atomic, so the cap holds even with 6 workers racing
        if (c.stored.incrementAndGet() <= maxStoredErrors) {
            errors.add(error);
        }
    }

    private IngestionError error(UUID taskId, long rowNum, String rawRow, String reason) {
        return IngestionError.builder()
                .taskId(taskId)
                .rowNum(rowNum)
                .rawRow(rawRow)
                .reason(truncate(Objects.toString(reason, "invalid row"), 500))
                .build();
    }

    private String raw(String[] cols) {
        return truncate(String.join(",", cols), 1000);
    }

    private String truncate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}