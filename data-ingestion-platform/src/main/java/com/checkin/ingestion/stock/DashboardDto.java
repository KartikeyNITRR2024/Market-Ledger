package com.checkin.ingestion.stock;

import java.util.List;

public record DashboardDto(
        Summary summary,
        String priceTicker,          // ticker the price chart is drawn for
        String granularity,          // day | week | month
        List<PricePoint> priceSeries,
        List<TickerVolume> topTickers,
        List<Performer> gainers,
        List<Performer> losers,
        List<Anomaly> anomalies) {

    public record Summary(long totalRows, long tickerCount, double averageClose,
                          double totalDollarVolume, long totalVolume, long anomalyCount) {}

    public record PricePoint(String date, double close, Double ma20, Double ma50, long volume) {}

    public record TickerVolume(String ticker, double dollarVolume, long volume) {}

    public record Performer(String ticker, double firstClose, double lastClose, double changePct) {}

    public record Anomaly(String date, String ticker, String type, Double returnPct, long volume) {}

    public static DashboardDto empty() {
        return new DashboardDto(new Summary(0, 0, 0, 0, 0, 0), "", "day",
                List.of(), List.of(), List.of(), List.of(), List.of());
    }
}