package com.checkin.ingestion.stock;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockDataRepository extends JpaRepository<StockData, Long>, JpaSpecificationExecutor<StockData> {

    Optional<StockData> findByTickerAndTradeDate(String ticker, LocalDate tradeDate);

    boolean existsByTickerAndTradeDate(String ticker, LocalDate tradeDate);

    long countByUploadTaskId(UUID uploadTaskId);

    Page<StockData> findByUploadTaskId(UUID uploadTaskId, Pageable pageable);

    Page<StockData> findByTicker(String ticker, Pageable pageable);

    Page<StockData> findByTradeDate(LocalDate tradeDate, Pageable pageable);

    @Modifying
    @Transactional
    @Query("DELETE FROM StockData s WHERE s.uploadTaskId = :uploadTaskId")
    int deleteByUploadTaskIdBulk(@Param("uploadTaskId") UUID uploadTaskId);
}