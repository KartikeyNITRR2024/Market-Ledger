package com.checkin.ingestion.stock;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StockDataService {
    Page<StockDataResponseDto> getStockData(StockSearchCriteria criteria, Pageable pageable);
}