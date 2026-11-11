package com.checkin.ingestion.stock;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
            name = "stock_data", 
    		indexes = {
    			    @Index(name = "uq_stock_ticker_date", columnList = "ticker, trade_date", unique = true),
    			    @Index(name = "idx_stock_trade_date", columnList = "trade_date"),
    			    @Index(name = "idx_stock_upload_task", columnList = "upload_task_id"),
    			    @Index(name = "idx_stock_close_price", columnList = "close_price")
    			}
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class StockData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "row_num", nullable = false)
    private long rowNum;

    @Column(name = "ticker", nullable = false, length = 20)
    private String ticker;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "open_price", precision = 19, scale = 4)
    private BigDecimal open;

    @Column(name = "high_price", precision = 19, scale = 4)
    private BigDecimal high;

    @Column(name = "low_price", precision = 19, scale = 4)
    private BigDecimal low;

    @Column(name = "close_price", precision = 19, scale = 4)
    private BigDecimal close;

    @Column(name = "volume")
    private Long volume;
    
    @Column(name = "adj_close_price", precision = 19, scale = 4)
    private BigDecimal adjClose;
    
    @Column(name = "dividends", precision = 19, scale = 4)
    private BigDecimal dividends;
    
    @Column(name = "stock_splits", precision = 19, scale = 4)
    private BigDecimal stockSplits;
    
    @Column(name = "upload_task_id", nullable = false)
    private UUID uploadTaskId; 
}