package com.checkin.ingestion.stock;

import com.checkin.ingestion.config.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

@Component
public class StockCacheEvictor {

    @CacheEvict(cacheNames = {CacheConfig.STOCK_DATA_CACHE, CacheConfig.DASHBOARD_CACHE}, allEntries = true)
    public void evictAll() {
        // eviction handled by the annotation
    }
}
