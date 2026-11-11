package com.checkin.ingestion.stock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockDataResponseDto {

    private Long id;
    private long rowNum;
    private String ticker;
    private LocalDate tradeDate;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private Long volume;
    private BigDecimal adjClose;
    private BigDecimal dividends;
    private BigDecimal stockSplits;
    private UUID uploadTaskId;

    public static StockDataResponseDto fromEntity(StockData entity) {
        if (entity == null) {
            return null;
        }

        return StockDataResponseDto.builder()
                .id(entity.getId())
                .rowNum(entity.getRowNum())
                .ticker(entity.getTicker())
                .tradeDate(entity.getTradeDate())
                .open(entity.getOpen())
                .high(entity.getHigh())
                .low(entity.getLow())
                .close(entity.getClose())
                .volume(entity.getVolume())
                .uploadTaskId(entity.getUploadTaskId())
                .adjClose(entity.getAdjClose())
                .stockSplits(entity.getStockSplits())
                .dividends(entity.getDividends())
                .build();
    }
}