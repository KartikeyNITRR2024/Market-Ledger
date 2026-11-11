package com.checkin.ingestion.worker;

import org.springframework.stereotype.Component;

import com.checkin.ingestion.stock.StockData;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Maps CSV headers to fields (case-insensitive, with aliases) and validates one row at a time.
 * Any IllegalArgumentException thrown here means "bad row": the worker records it and moves on.
 */
@Component
public class CsvRowParser {

    private static final Map<String, List<String>> ALIASES = Map.of(
            "ticker", List.of("ticker", "symbol", "stock", "name"),
            "date", List.of("date", "trade_date", "timestamp", "datetime"),
            "open", List.of("open"),
            "high", List.of("high"),
            "low", List.of("low"),
            "close", List.of("close", "close_price", "price"),
            "volume", List.of("volume", "vol"),
            "adj_close", List.of("adj_close", "adjclose", "adjusted_close", "adj_close_price"),
            "dividends", List.of("dividends", "dividend", "div"),
            "stock_splits", List.of("stock_splits", "stocksplits", "split", "splits")
    );

    private static final Set<String> REQUIRED = Set.of("ticker", "date", "close");
    private static final DateTimeFormatter US_DATE = DateTimeFormatter.ofPattern("M/d/yyyy");

    public Map<String, Integer> mapHeader(String[] header) {
        Map<String, Integer> normalized = new HashMap<>();
        for (int i = 0; i < header.length; i++) {
            normalized.putIfAbsent(normalize(header[i]), i);
        }

        Map<String, Integer> idx = new HashMap<>();
        ALIASES.forEach((field, names) -> {
            for (String n : names) {
                Integer i = normalized.get(n);
                if (i != null) {
                    idx.put(field, i);
                    break;
                }
            }
        });

        for (String required : REQUIRED) {
            if (!idx.containsKey(required)) {
                throw new IllegalArgumentException(
                        "Missing required column '" + required + "'. Found: " + Arrays.toString(header));
            }
        }
        return idx;
    }

    public StockData parse(long rowNum, String[] cols, Map<String, Integer> idx, UUID taskId) {
        String ticker = text(cols, idx.get("ticker"));
        if (ticker == null) {
            throw new IllegalArgumentException("ticker is empty");
        }
        if (ticker.length() > 20) {
            throw new IllegalArgumentException("ticker too long: " + ticker);
        }

        LocalDate date = parseDate(text(cols, idx.get("date")));
        BigDecimal open = decimal(cols, idx.get("open"), "open");
        BigDecimal high = decimal(cols, idx.get("high"), "high");
        BigDecimal low = decimal(cols, idx.get("low"), "low");
        BigDecimal close = decimal(cols, idx.get("close"), "close");

        if (close == null) {
            throw new IllegalArgumentException("close is empty");
        }
        if (high != null && low != null && high.compareTo(low) < 0) {
            throw new IllegalArgumentException("high is less than low");
        }

        Long volume = volume(cols, idx.get("volume"));
        BigDecimal adjClose = decimal(cols, idx.get("adj_close"), "adj_close");
        BigDecimal dividends = decimal(cols, idx.get("dividends"), "dividends");
        BigDecimal stockSplits = decimal(cols, idx.get("stock_splits"), "stock_splits");

        return StockData.builder()
                .rowNum(rowNum)
                .ticker(ticker.toUpperCase(Locale.ROOT))
                .tradeDate(date)
                .open(open)
                .high(high)
                .low(low)
                .close(close)
                .volume(volume)
                .adjClose(adjClose)
                .dividends(dividends)
                .stockSplits(stockSplits)
                .uploadTaskId(taskId)
                .build();
    }

    private String normalize(String h) {
        return h == null ? "" : h.replace("\ufeff", "").trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    private String text(String[] cols, Integer i) {
        if (i == null || i >= cols.length || cols[i] == null) {
            return null;
        }
        String v = cols[i].trim();
        return v.isEmpty() ? null : v;
    }

    private BigDecimal decimal(String[] cols, Integer i, String field) {
        String v = text(cols, i);
        if (v == null) {
            return null;
        }
        try {
            BigDecimal d = new BigDecimal(v.replace(",", ""));
            if (d.signum() < 0) {
                throw new IllegalArgumentException(field + " is negative: " + v);
            }
            return d;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(field + " is not a number: " + v);
        }
    }

    private Long volume(String[] cols, Integer i) {
        String v = text(cols, i);
        if (v == null) {
            return null;
        }
        try {
            BigDecimal d = new BigDecimal(v.replace(",", ""));
            if (d.signum() < 0) {
                throw new IllegalArgumentException("volume is negative: " + v);
            }
            return d.setScale(0, RoundingMode.HALF_UP).longValueExact();
        } catch (NumberFormatException | ArithmeticException e) {
            throw new IllegalArgumentException("volume is not a valid number: " + v);
        }
    }

    private LocalDate parseDate(String v) {
        if (v == null) {
            throw new IllegalArgumentException("date is empty");
        }
        try {
            if (v.length() >= 10 && v.charAt(4) == '-') {
                return LocalDate.parse(v.substring(0, 10)); // handles "yyyy-MM-dd" and "yyyy-MM-dd HH:mm:ss"
            }
            return LocalDate.parse(v, US_DATE);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("invalid date: " + v);
        }
    }
}