package com.cloud.stock.task;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.annotation.DistributedLock;
import com.cloud.stock.module.entity.StockLedger;
import com.cloud.stock.service.StockLedgerQueryService;
import com.cloud.stock.service.support.StockRedisCacheService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockCacheXxlJob {

    private static final int DEFAULT_PAGE_SIZE = 500;

    private final StockLedgerQueryService stockLedgerQueryService;
    private final StockRedisCacheService stockRedisCacheService;

    @XxlJob("stockCacheWarmUpJob")
    @DistributedLock(
            key = "'xxl:stock:cache-warmup'",
            waitTime = 1,
            leaseTime = 300,
            failStrategy = DistributedLock.LockFailStrategy.RETURN_NULL
    )
    public void warmUpStockCache() {
        int total = 0;
        long pageIndex = 1;
        int pageSize = DEFAULT_PAGE_SIZE;
        while (true) {
            Page<StockLedger> page = stockLedgerQueryService.pageActiveLedgers(pageIndex, pageSize);
            List<StockLedger> records = page.getRecords();
            if (records == null || records.isEmpty()) {
                break;
            }
            for (StockLedger ledger : records) {
                stockRedisCacheService.cacheLedger(ledger);
                total++;
            }
            if (records.size() < pageSize) {
                break;
            }
            pageIndex++;
        }
        String message = "stockCacheWarmUpJob finished, total=" + total;
        XxlJobHelper.log(message);
        log.info(message);
    }

    @XxlJob("stockCacheVerifyJob")
    @DistributedLock(
            key = "'xxl:stock:cache-verify'",
            waitTime = 1,
            leaseTime = 300,
            failStrategy = DistributedLock.LockFailStrategy.RETURN_NULL
    )
    public void verifyStockCache() {
        int fixed = 0;
        long pageIndex = 1;
        int pageSize = DEFAULT_PAGE_SIZE;
        while (true) {
            Page<StockLedger> page = stockLedgerQueryService.pageActiveLedgers(pageIndex, pageSize);
            List<StockLedger> records = page.getRecords();
            if (records == null || records.isEmpty()) {
                break;
            }
            for (StockLedger ledger : records) {
                if (ledger.getSkuId() == null) {
                    continue;
                }
                if (stockRedisCacheService.getLedgerFromCache(ledger.getSkuId()) == null) {
                    stockRedisCacheService.cacheLedger(ledger);
                    fixed++;
                }
                if (ledger.getSalableQty() != null && ledger.getSalableQty() < 0) {
                    log.warn("Stock ledger salable negative: skuId={}, salable={}", ledger.getSkuId(), ledger.getSalableQty());
                }
            }
            if (records.size() < pageSize) {
                break;
            }
            pageIndex++;
        }
        String message = "stockCacheVerifyJob finished, fixed=" + fixed;
        XxlJobHelper.log(message);
        log.info(message);
    }
}
