package com.checkin.ingestion.stock;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.checkin.ingestion.common.util.RestPage;
import com.checkin.ingestion.config.CacheConfig;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;

@Service
@RequiredArgsConstructor
public class StockDataServiceImpl implements StockDataService {

    private final StockDataRepository stockDataRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.STOCK_DATA_CACHE,
               key = "T(com.checkin.ingestion.stock.StockCacheKeys).of(#criteria, #pageable)")
    public Page<StockDataResponseDto> getStockData(StockSearchCriteria criteria, Pageable pageable) {
        Page<StockDataResponseDto> page = stockDataRepository
                .findAll(createSpecification(criteria), pageable)
                .map(StockDataResponseDto::fromEntity);
        return new RestPage<>(page);
    }

    private Specification<StockData> createSpecification(StockSearchCriteria criteria) {
        return (root, query, cb) -> {
            if (criteria == null) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            if (criteria.getTicker() != null && !criteria.getTicker().isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("ticker")), criteria.getTicker().trim().toUpperCase()));
            }

            if (criteria.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("tradeDate"), criteria.getStartDate()));
            }

            if (criteria.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("tradeDate"), criteria.getEndDate()));
            }

            if (criteria.getMinClosePrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("close"), criteria.getMinClosePrice()));
            }

            if (criteria.getMaxClosePrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("close"), criteria.getMaxClosePrice()));
            }

            if (criteria.getMinAdjClosePrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("adjClose"), criteria.getMinAdjClosePrice()));
            }

            if (criteria.getMaxAdjClosePrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("adjClose"), criteria.getMaxAdjClosePrice()));
            }

            if (criteria.getMinVolume() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("volume"), criteria.getMinVolume()));
            }

            if (criteria.getMaxVolume() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("volume"), criteria.getMaxVolume()));
            }

            if (criteria.getMinDividends() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dividends"), criteria.getMinDividends()));
            }

            if (Boolean.TRUE.equals(criteria.getHasDividends())) {
                predicates.add(cb.greaterThan(root.get("dividends"), BigDecimal.ZERO));
            }

            if (criteria.getMinStockSplits() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("stockSplits"), criteria.getMinStockSplits()));
            }

            if (Boolean.TRUE.equals(criteria.getHasStockSplits())) {
                predicates.add(cb.greaterThan(root.get("stockSplits"), BigDecimal.ZERO));
            }

            if (criteria.getUploadTaskId() != null) {
                predicates.add(cb.equal(root.get("uploadTaskId"), criteria.getUploadTaskId()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}