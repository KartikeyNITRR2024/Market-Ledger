package com.checkin.ingestion.worker;

import com.checkin.ingestion.stock.IngestionError;
import com.checkin.ingestion.stock.StockData;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Separate bean so @Transactional works (calls from CsvBatchProcessor go through the proxy).
 *
 * Key differences from the old version:
 *  - Progress is NOT updated inside the data transaction any more. Every worker used to lock the
 *    same upload_tasks row until commit, which serialised the workers and could deadlock against
 *    stock_data row locks. Progress is now a tiny auto-commit "+= delta" after the batch commits.
 *  - Rows are sorted by (ticker, date) before writing, so concurrent batches take row locks in the
 *    same order (far fewer deadlocks). The same TreeMap also de-duplicates inside the batch.
 */
@Service
@RequiredArgsConstructor
public class BatchWriter {

    // ---- H2 (what you run now) ------------------------------------------------------------
//    private static final String UPSERT_SQL = """
//            MERGE INTO stock_data
//                (row_num, ticker, trade_date, open_price, high_price, low_price, close_price, volume, upload_task_id, adj_close_price, dividends, stock_splits)
//            KEY (ticker, trade_date)
//            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
//            """;

    // ---- PostgreSQL version --------------------------------------------------------------
    // The WHERE guard makes "last row in the file wins" hold even when the same (ticker, date)
    // lands in two concurrent batches of the same upload:
    //
	private static final String UPSERT_SQL = """
     INSERT INTO stock_data
         (row_num, ticker, trade_date, open_price, high_price, low_price, close_price,
          volume, upload_task_id, adj_close_price, dividends, stock_splits)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
     ON CONFLICT (ticker, trade_date) DO UPDATE SET
         row_num = EXCLUDED.row_num, open_price = EXCLUDED.open_price, high_price = EXCLUDED.high_price,
         low_price = EXCLUDED.low_price, close_price = EXCLUDED.close_price, volume = EXCLUDED.volume,
         upload_task_id = EXCLUDED.upload_task_id, adj_close_price = EXCLUDED.adj_close_price,
         dividends = EXCLUDED.dividends, stock_splits = EXCLUDED.stock_splits
     WHERE stock_data.upload_task_id IS DISTINCT FROM EXCLUDED.upload_task_id
        OR stock_data.row_num < EXCLUDED.row_num
         """;
    //
    // Also add  ?reWriteBatchedInserts=true  to the Postgres JDBC URL.

    private final JdbcTemplate jdbc;

    /** Normal path: good rows + this batch's error rows commit together or not at all. */
    @Transactional
    public void writeBatch(UUID taskId, List<StockData> rows, List<IngestionError> errors) {
        upsert(taskId, rows);
        insertErrors(taskId, errors);
    }

    /** Used when isolating bad rows after a whole batch was rejected. */
    @Transactional
    public void writeRows(UUID taskId, List<StockData> rows) {
        upsert(taskId, rows);
    }

    @Transactional
    public void writeErrors(UUID taskId, List<IngestionError> errors) {
        insertErrors(taskId, errors);
    }

    /**
     * Auto-commit, one tiny statement, adds deltas. Order-independent, so it can never go backwards
     * when workers finish out of order. Deliberately NOT @Transactional (and called outside any tx).
     */
    public void addProgress(UUID taskId, long processedDelta, long errorDelta) {
        jdbc.update("UPDATE upload_tasks "
                        + "SET processed_rows = COALESCE(processed_rows, 0) + ?, "
                        + "    error_count    = COALESCE(error_count, 0) + ? "
                        + "WHERE id = ?",
                processedDelta, errorDelta, taskId);
    }

    private record Key(String ticker, LocalDate date) implements Comparable<Key> {
        @Override
        public int compareTo(Key o) {
            int c = ticker.compareTo(o.ticker);
            return c != 0 ? c : date.compareTo(o.date);
        }
    }

    private void upsert(UUID taskId, List<StockData> rows) {
        if (rows.isEmpty()) {
            return;
        }

        // Sorted + de-duplicated (last occurrence wins) in one pass.
        TreeMap<Key, StockData> sorted = new TreeMap<>();
        for (StockData r : rows) {
            sorted.put(new Key(r.getTicker(), r.getTradeDate()), r);
        }
        List<StockData> unique = new ArrayList<>(sorted.values());

        jdbc.batchUpdate(UPSERT_SQL, unique, unique.size(), (ps, r) -> {
            ps.setLong(1, r.getRowNum());
            ps.setString(2, r.getTicker());
            ps.setDate(3, Date.valueOf(r.getTradeDate()));
            ps.setBigDecimal(4, r.getOpen());
            ps.setBigDecimal(5, r.getHigh());
            ps.setBigDecimal(6, r.getLow());
            ps.setBigDecimal(7, r.getClose());
            if (r.getVolume() == null) {
                ps.setNull(8, Types.BIGINT);
            } else {
                ps.setLong(8, r.getVolume());
            }
            ps.setObject(9, taskId);
            ps.setBigDecimal(10, r.getAdjClose());
            ps.setBigDecimal(11, r.getDividends());
            ps.setBigDecimal(12, r.getStockSplits());
        });
    }

    private void insertErrors(UUID taskId, List<IngestionError> errors) {
        if (errors.isEmpty()) {
            return;
        }
        jdbc.batchUpdate(
                "INSERT INTO ingestion_errors (task_id, row_num, raw_row, reason) VALUES (?, ?, ?, ?)",
                errors, errors.size(), (ps, e) -> {
                    ps.setObject(1, taskId);
                    ps.setLong(2, e.getRowNum());
                    ps.setString(3, e.getRawRow());
                    ps.setString(4, e.getReason());
                });
    }
}