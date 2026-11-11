package com.checkin.ingestion.stock;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/** Read-only analytics queries. Plain SQL that runs on both H2 (2.x) and PostgreSQL. */
public interface StockAnalyticsRepository extends Repository<StockData, Long> {

    interface SummaryRow {
        Number getTotalRows();
        Number getTickerCount();
        Number getAverageClose();
        Number getDollarVolume();
        Number getTotalVolume();
    }

    interface BoundsRow {
        String getMinDate();
        String getMaxDate();
    }

    interface SeriesRow {
        String getPeriod();
        Number getAvgClose();
        Number getVolume();
    }

    interface TickerVolumeRow {
        String getTicker();
        Number getDollarVolume();
        Number getVolume();
    }

    interface PerformerRow {
        String getTicker();
        Number getFirstClose();
        Number getLastClose();
        Number getChangePct();
    }

    interface AnomalyRow {
        String getDate();
        String getTicker();
        String getType();
        Number getReturnPct();
        Number getVolume();
        Number getTotal();
    }

    @Query(nativeQuery = true, value = """
            SELECT COUNT(*) AS "totalRows",
                   COUNT(DISTINCT ticker) AS "tickerCount",
                   AVG(close_price) AS "averageClose",
                   COALESCE(SUM(close_price * volume), 0) AS "dollarVolume",
                   COALESCE(SUM(volume), 0) AS "totalVolume"
            FROM stock_data
            WHERE (:ticker = '' OR ticker = :ticker)
              AND trade_date BETWEEN :start AND :end
            """)
    SummaryRow summary(@Param("ticker") String ticker,
                       @Param("start") LocalDate start,
                       @Param("end") LocalDate end);

    @Query(nativeQuery = true, value = """
            SELECT CAST(MIN(trade_date) AS VARCHAR(10)) AS "minDate",
                   CAST(MAX(trade_date) AS VARCHAR(10)) AS "maxDate"
            FROM stock_data
            WHERE (:ticker = '' OR ticker = :ticker)
              AND trade_date BETWEEN :start AND :end
            """)
    BoundsRow bounds(@Param("ticker") String ticker,
                     @Param("start") LocalDate start,
                     @Param("end") LocalDate end);

    /** One row per day. The service groups these into weeks/months, so no date_trunc is needed. */
    @Query(nativeQuery = true, value = """
            SELECT CAST(trade_date AS VARCHAR(10)) AS "period",
                   AVG(close_price) AS "avgClose",
                   COALESCE(SUM(volume), 0) AS "volume"
            FROM stock_data
            WHERE (:ticker = '' OR ticker = :ticker)
              AND trade_date BETWEEN :start AND :end
            GROUP BY trade_date
            ORDER BY trade_date
            """)
    List<SeriesRow> dailySeries(@Param("ticker") String ticker,
                                @Param("start") LocalDate start,
                                @Param("end") LocalDate end);

    @Query(nativeQuery = true, value = """
            SELECT ticker AS "ticker",
                   COALESCE(SUM(close_price * volume), 0) AS "dollarVolume",
                   COALESCE(SUM(volume), 0) AS "volume"
            FROM stock_data
            WHERE (:ticker = '' OR ticker = :ticker)
              AND trade_date BETWEEN :start AND :end
            GROUP BY ticker
            ORDER BY SUM(close_price * volume) DESC NULLS LAST
            LIMIT :limit
            """)
    List<TickerVolumeRow> topTickers(@Param("ticker") String ticker,
                                     @Param("start") LocalDate start,
                                     @Param("end") LocalDate end,
                                     @Param("limit") int limit);

    @Query(nativeQuery = true, value = """
            WITH rng AS (
                SELECT ticker, MIN(trade_date) AS d0, MAX(trade_date) AS d1
                FROM stock_data
                WHERE trade_date BETWEEN :start AND :end
                  AND adj_close_price IS NOT NULL
                GROUP BY ticker
                HAVING COUNT(*) > 1
            ), f AS (
                SELECT rng.ticker AS ticker,
                       AVG(CASE WHEN s.trade_date = rng.d0 THEN CAST(s.adj_close_price AS DOUBLE PRECISION) END) AS first_close,
                       AVG(CASE WHEN s.trade_date = rng.d1 THEN CAST(s.adj_close_price AS DOUBLE PRECISION) END) AS last_close
                FROM rng
                JOIN stock_data s ON s.ticker = rng.ticker
                                 AND s.trade_date IN (rng.d0, rng.d1)
                                 AND s.adj_close_price IS NOT NULL
                GROUP BY rng.ticker
            ), p AS (
                SELECT ticker, first_close, last_close,
                       (last_close / NULLIF(first_close, 0) - 1) * 100 AS change_pct
                FROM f
            )
            SELECT ticker AS "ticker", first_close AS "firstClose",
                   last_close AS "lastClose", change_pct AS "changePct"
            FROM p
            WHERE change_pct IS NOT NULL
            ORDER BY change_pct DESC
            LIMIT :limit
            """)
    List<PerformerRow> bestPerformers(@Param("start") LocalDate start,
                                      @Param("end") LocalDate end,
                                      @Param("limit") int limit);

    @Query(nativeQuery = true, value = """
            WITH rng AS (
                SELECT ticker, MIN(trade_date) AS d0, MAX(trade_date) AS d1
                FROM stock_data
                WHERE trade_date BETWEEN :start AND :end
                  AND adj_close_price IS NOT NULL
                GROUP BY ticker
                HAVING COUNT(*) > 1
            ), f AS (
                SELECT rng.ticker AS ticker,
                       AVG(CASE WHEN s.trade_date = rng.d0 THEN CAST(s.adj_close_price AS DOUBLE PRECISION) END) AS first_close,
                       AVG(CASE WHEN s.trade_date = rng.d1 THEN CAST(s.adj_close_price AS DOUBLE PRECISION) END) AS last_close
                FROM rng
                JOIN stock_data s ON s.ticker = rng.ticker
                                 AND s.trade_date IN (rng.d0, rng.d1)
                                 AND s.adj_close_price IS NOT NULL
                GROUP BY rng.ticker
            ), p AS (
                SELECT ticker, first_close, last_close,
                       (last_close / NULLIF(first_close, 0) - 1) * 100 AS change_pct
                FROM f
            )
            SELECT ticker AS "ticker", first_close AS "firstClose",
                   last_close AS "lastClose", change_pct AS "changePct"
            FROM p
            WHERE change_pct IS NOT NULL
            ORDER BY change_pct ASC
            LIMIT :limit
            """)
    List<PerformerRow> worstPerformers(@Param("start") LocalDate start,
                                       @Param("end") LocalDate end,
                                       @Param("limit") int limit);

    /**
     * Anomaly = daily adjusted return OR volume more than :z standard deviations from that
     * ticker's own mean (stats use the ticker's full history). "total" repeats on every row.
     */
    @Query(nativeQuery = true, value = """
            WITH r AS (
                SELECT ticker, trade_date, volume,
                       CAST(adj_close_price AS DOUBLE PRECISION)
                         / NULLIF(LAG(CAST(adj_close_price AS DOUBLE PRECISION))
                                  OVER (PARTITION BY ticker ORDER BY trade_date), 0) - 1 AS ret
                FROM stock_data
                WHERE (:ticker = '' OR ticker = :ticker)
            ), s AS (
                SELECT ticker,
                       AVG(ret) AS m, STDDEV_SAMP(ret) AS sd,
                       AVG(CAST(volume AS DOUBLE PRECISION)) AS vm,
                       STDDEV_SAMP(CAST(volume AS DOUBLE PRECISION)) AS vsd
                FROM r GROUP BY ticker
            ), a AS (
                SELECT r.ticker AS ticker, r.trade_date AS trade_date, r.ret AS ret, r.volume AS volume,
                       (r.ret - s.m) / NULLIF(s.sd, 0) AS ret_z,
                       (CAST(r.volume AS DOUBLE PRECISION) - s.vm) / NULLIF(s.vsd, 0) AS vol_z
                FROM r JOIN s ON s.ticker = r.ticker
                WHERE r.trade_date BETWEEN :start AND :end
            ), b AS (
                SELECT ticker, trade_date, ret, volume, ret_z, vol_z,
                       CASE WHEN COALESCE(ABS(ret_z), 0) > COALESCE(vol_z, 0)
                            THEN COALESCE(ABS(ret_z), 0) ELSE COALESCE(vol_z, 0) END AS score
                FROM a
            )
            SELECT CAST(trade_date AS VARCHAR(10)) AS "date",
                   ticker AS "ticker",
                   CASE WHEN ABS(ret_z) > :z AND vol_z > :z THEN 'PRICE_AND_VOLUME'
                        WHEN ABS(ret_z) > :z THEN 'PRICE_MOVE'
                        ELSE 'VOLUME_SPIKE' END AS "type",
                   ret * 100 AS "returnPct",
                   volume AS "volume",
                   COUNT(*) OVER () AS "total"
            FROM b
            WHERE ABS(ret_z) > :z OR vol_z > :z
            ORDER BY score DESC
            LIMIT :limit
            """)
    List<AnomalyRow> anomalies(@Param("ticker") String ticker,
                               @Param("start") LocalDate start,
                               @Param("end") LocalDate end,
                               @Param("z") double z,
                               @Param("limit") int limit);
}