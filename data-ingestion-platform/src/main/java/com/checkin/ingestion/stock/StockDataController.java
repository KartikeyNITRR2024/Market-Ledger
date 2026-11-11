package com.checkin.ingestion.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class StockDataController {

    private final StockDataService stockDataService;

    @PreAuthorize("hasAnyRole('ADMIN','VIEWER')")
    @GetMapping
    public ResponseEntity<Page<StockDataResponseDto>> getStockData(
            StockSearchCriteria criteria,
            @PageableDefault(page = 0, size = 20) Pageable pageable) {

        Page<StockDataResponseDto> result = stockDataService.getStockData(criteria, pageable);
        return ResponseEntity.ok(result);
    }
}