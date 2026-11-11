package com.checkin.ingestion.stock;

import com.checkin.ingestion.config.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StockAnalyticsService {

    private static final LocalDate MIN_DATE = LocalDate.of(1900, 1, 1);
    private static final LocalDate MAX_DATE = LocalDate.of(2999, 12, 31);
    private static final int TOP_TICKERS = 10;
    private static final int PERFORMERS = 5;
    private static final int ANOMALIES = 10;
    private static final double Z_THRESHOLD = 3.0;

    private final StockAnalyticsRepository repo;

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.DASHBOARD_CACHE,
               key = "(#ticker == null ? '' : #ticker.trim().toUpperCase()) + '|' + #startDate + '|' + #endDate")
    public DashboardDto getDashboard(String ticker, LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must not be after endDate");
        }
        String t = ticker == null ? "" : ticker.trim().toUpperCase();
        LocalDate from = startDate != null ? startDate : MIN_DATE;
        LocalDate to = endDate != null ? endDate : MAX_DATE;

        var s = repo.summary(t, from, to);
        if (lng(s.getTotalRows()) == 0) {
            return DashboardDto.empty();
        }

        // Top tickers by dollar volume
        List<DashboardDto.TickerVolume> top = repo.topTickers(t, from, to, TOP_TICKERS).stream()
                .map(r -> new DashboardDto.TickerVolume(r.getTicker(), dbl(r.getDollarVolume()), lng(r.getVolume())))
                .toList();

        // Price chart: the requested ticker, or the most traded one when none is selected
        String priceTicker = !t.isEmpty() ? t : (top.isEmpty() ? "" : top.get(0).ticker());
        var bounds = repo.bounds(priceTicker, from, to);
        String unit = granularity(bounds.getMinDate(), bounds.getMaxDate());
        List<DashboardDto.PricePoint> series = withMovingAverages(bucketize(repo.dailySeries(priceTicker, from, to), unit));

        // Gainers / losers only make sense across several tickers
        List<DashboardDto.Performer> gainers = List.of();
        List<DashboardDto.Performer> losers = List.of();
        if (t.isEmpty()) {
            gainers = performers(from, to, 1);
            losers = performers(from, to, -1);
        }

        // Anomalies
        var rows = repo.anomalies(t, from, to, Z_THRESHOLD, ANOMALIES);
        long anomalyCount = rows.isEmpty() ? 0 : lng(rows.get(0).getTotal());
        List<DashboardDto.Anomaly> anomalies = rows.stream()
                .map(r -> new DashboardDto.Anomaly(r.getDate(), r.getTicker(), r.getType(),
                        r.getReturnPct() == null ? null : round(dbl(r.getReturnPct())), lng(r.getVolume())))
                .toList();

        var summary = new DashboardDto.Summary(
                lng(s.getTotalRows()), lng(s.getTickerCount()), round(dbl(s.getAverageClose())),
                dbl(s.getDollarVolume()), lng(s.getTotalVolume()), anomalyCount);

        return new DashboardDto(summary, priceTicker, unit, series, top, gainers, losers, anomalies);
    }

    private List<DashboardDto.Performer> performers(LocalDate from, LocalDate to, int sign) {
        var rows = sign > 0 ? repo.bestPerformers(from, to, PERFORMERS)
                            : repo.worstPerformers(from, to, PERFORMERS);
        return rows.stream()
                .map(r -> new DashboardDto.Performer(r.getTicker(), round(dbl(r.getFirstClose())),
                        round(dbl(r.getLastClose())), round(dbl(r.getChangePct()))))
                .toList();
    }

    /** Keeps the chart under ~400 points however long the range is. */
    private String granularity(String min, String max) {
        if (min == null || max == null) return "day";
        long days = ChronoUnit.DAYS.between(LocalDate.parse(min), LocalDate.parse(max));
        return days <= 400 ? "day" : days <= 2500 ? "week" : "month";
    }

    private record Bucket(String period, double close, long volume) {}

    /** Groups daily rows into day / week (Monday start) / month buckets. */
    private List<Bucket> bucketize(List<StockAnalyticsRepository.SeriesRow> days, String unit) {
        Map<String, double[]> acc = new LinkedHashMap<>();   // [sumClose, count, volume]
        for (var r : days) {
            if (r.getAvgClose() == null) continue;
            LocalDate d = LocalDate.parse(r.getPeriod());
            LocalDate key = switch (unit) {
                case "week" -> d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                case "month" -> d.withDayOfMonth(1);
                default -> d;
            };
            double[] a = acc.computeIfAbsent(key.toString(), k -> new double[3]);
            a[0] += dbl(r.getAvgClose());
            a[1] += 1;
            a[2] += lng(r.getVolume());
        }
        return acc.entrySet().stream()
                .map(e -> new Bucket(e.getKey(), e.getValue()[0] / e.getValue()[1], (long) e.getValue()[2]))
                .toList();
    }

    private List<DashboardDto.PricePoint> withMovingAverages(List<Bucket> buckets) {
        List<Double> closes = new ArrayList<>();
        List<DashboardDto.PricePoint> out = new ArrayList<>();
        for (var b : buckets) {
            closes.add(b.close());
            int i = closes.size() - 1;
            out.add(new DashboardDto.PricePoint(b.period(), round(b.close()),
                    movingAverage(closes, i, 20), movingAverage(closes, i, 50), b.volume()));
        }
        return out;
    }

    private Double movingAverage(List<Double> v, int i, int window) {
        if (i + 1 < window) return null;
        double sum = 0;
        for (int k = i - window + 1; k <= i; k++) sum += v.get(k);
        return round(sum / window);
    }

    private static double dbl(Number n) { return n == null ? 0 : n.doubleValue(); }
    private static long lng(Number n) { return n == null ? 0 : n.longValue(); }
    private static double round(double v) { return Math.round(v * 10000.0) / 10000.0; }
}
