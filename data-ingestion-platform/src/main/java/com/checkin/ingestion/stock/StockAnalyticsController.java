package com.checkin.ingestion.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/stocks/analytics")
@RequiredArgsConstructor
public class StockAnalyticsController {

    private final StockAnalyticsService analyticsService;

    @PreAuthorize("hasAnyRole('ADMIN','VIEWER')")
    @GetMapping
    public ResponseEntity<DashboardDto> getDashboard(
            @RequestParam(required = false) String ticker,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(analyticsService.getDashboard(ticker, startDate, endDate));
    }
}