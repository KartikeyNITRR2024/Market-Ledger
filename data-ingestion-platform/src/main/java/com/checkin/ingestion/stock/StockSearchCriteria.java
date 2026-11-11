package com.checkin.ingestion.stock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockSearchCriteria {

    private String ticker;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    private BigDecimal minClosePrice;
    private BigDecimal maxClosePrice;

    private BigDecimal minAdjClosePrice;
    private BigDecimal maxAdjClosePrice;

    private Long minVolume;
    private Long maxVolume;

    private BigDecimal minDividends;
    private Boolean hasDividends; 

    private BigDecimal minStockSplits;
    private Boolean hasStockSplits;

    private UUID uploadTaskId;
}