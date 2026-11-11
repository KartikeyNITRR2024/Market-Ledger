package com.checkin.ingestion.stock;

import org.springframework.data.domain.Pageable;

public final class StockCacheKeys {

    private StockCacheKeys() {}

    public static String of(StockSearchCriteria c, Pageable p) {
        String criteriaPart = (c == null) ? "all" : String.join("|",
                c.getTicker() == null ? "" : c.getTicker().trim().toUpperCase(),
                String.valueOf(c.getStartDate()),
                String.valueOf(c.getEndDate()),
                String.valueOf(c.getMinClosePrice()),
                String.valueOf(c.getMaxClosePrice()),
                String.valueOf(c.getMinAdjClosePrice()),
                String.valueOf(c.getMaxAdjClosePrice()),
                String.valueOf(c.getMinVolume()),
                String.valueOf(c.getMaxVolume()),
                String.valueOf(c.getMinDividends()),
                String.valueOf(c.getHasDividends()),
                String.valueOf(c.getMinStockSplits()),
                String.valueOf(c.getHasStockSplits()),
                String.valueOf(c.getUploadTaskId()));

        String pagePart = p.isUnpaged()
                ? "unpaged"
                : p.getPageNumber() + ":" + p.getPageSize() + ":" + p.getSort();

        return criteriaPart + "#" + pagePart;
    }
}