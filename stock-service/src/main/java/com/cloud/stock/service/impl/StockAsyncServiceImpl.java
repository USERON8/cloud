package com.cloud.stock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.common.domain.dto.stock.StockDTO;
import com.cloud.stock.converter.StockConverter;
import com.cloud.stock.mapper.StockMapper;
import com.cloud.stock.module.dto.StockOperationResult;
import com.cloud.stock.module.entity.Stock;
import com.cloud.stock.service.StockAsyncService;
import com.cloud.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockAsyncServiceImpl implements StockAsyncService {

    private static final int BATCH_SIZE = 50;
    private final StockService stockService;
    private final StockMapper stockMapper;
    private final StockConverter stockConverter;
    private final CacheManager cacheManager;

    @Override
    @Async("stockQueryExecutor")
    public CompletableFuture<List<StockDTO>> getStocksByProductIdsAsync(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        if (productIds.size() <= BATCH_SIZE) {
            return CompletableFuture.completedFuture(stockService.getStocksByProductIds(productIds));
        }

        List<Long> idList = new ArrayList<>(productIds);
        List<StockDTO> result = new ArrayList<>();
        for (int i = 0; i < idList.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, idList.size());
            List<Long> batch = idList.subList(i, end);
            List<StockDTO> batchResult = stockService.getStocksByProductIds(batch);
            if (batchResult != null && !batchResult.isEmpty()) {
                result.addAll(batchResult);
            }
        }
        return CompletableFuture.completedFuture(result);
    }

    @Override
    @Async("stockQueryExecutor")
    public CompletableFuture<Map<Long, Boolean>> checkStocksSufficientAsync(Map<Long, Integer> productQuantityMap) {
        if (productQuantityMap == null || productQuantityMap.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyMap());
        }

        Map<Long, Boolean> result = new HashMap<>();
        productQuantityMap.forEach((productId, quantity) ->
                result.put(productId, stockService.checkStockSufficient(productId, quantity)));
        return CompletableFuture.completedFuture(result);
    }

    @Override
    @Async("stockOperationExecutor")
    public CompletableFuture<StockOperationResult> batchReserveStockAsync(Map<Long, Integer> productQuantityMap) {
        return CompletableFuture.completedFuture(
                executeBatchMapOperation(productQuantityMap, (productId, qty) -> stockService.reserveStock(productId, qty))
        );
    }

    @Override
    @Async("stockOperationExecutor")
    public CompletableFuture<StockOperationResult> batchReleaseStockAsync(Map<Long, Integer> productQuantityMap) {
        return CompletableFuture.completedFuture(
                executeBatchMapOperation(productQuantityMap, (productId, qty) -> stockService.releaseReservedStock(productId, qty))
        );
    }

    @Override
    @Async("stockOperationExecutor")
    public CompletableFuture<StockOperationResult> batchStockInAsync(List<StockInRequest> stockInList) {
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<String> errors = new ArrayList<>();

        if (stockInList != null) {
            stockInList.forEach(request -> {
                try {
                    boolean success = stockService.stockIn(request.getProductId(), request.getQuantity(), request.getRemark());
                    if (success) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                        errors.add("stock in failed for productId=" + request.getProductId());
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    errors.add("stock in exception for productId=" + request.getProductId() + ", " + e.getMessage());
                }
            });
        }

        return CompletableFuture.completedFuture(new StockOperationResult(successCount.get(), failureCount.get(), errors));
    }

    @Override
    @Async("stockOperationExecutor")
    public CompletableFuture<StockOperationResult> batchStockOutAsync(List<StockOutRequest> stockOutList) {
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<String> errors = new ArrayList<>();

        if (stockOutList != null) {
            stockOutList.forEach(request -> {
                try {
                    boolean success = stockService.stockOut(
                            request.getProductId(),
                            request.getQuantity(),
                            request.getOrderId(),
                            request.getOrderNo(),
                            request.getRemark()
                    );
                    if (success) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                        errors.add("stock out failed for productId=" + request.getProductId());
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    errors.add("stock out exception for productId=" + request.getProductId() + ", " + e.getMessage());
                }
            });
        }

        return CompletableFuture.completedFuture(new StockOperationResult(successCount.get(), failureCount.get(), errors));
    }

    @Override
    @Async("stockCommonExecutor")
    public CompletableFuture<Void> refreshStockCacheAsync(Long productId) {
        refreshStockCacheDirect(productId);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    @Async("stockCommonExecutor")
    public CompletableFuture<Void> batchRefreshStockCacheAsync(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        productIds.stream()
                .filter(Objects::nonNull)
                .forEach(this::refreshStockCacheDirect);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    @Async("stockCommonExecutor")
    public CompletableFuture<Integer> preloadPopularStocksAsync(Integer limit) {
        int size = (limit == null || limit <= 0) ? 100 : limit;
        LambdaQueryWrapper<Stock> wrapper = new LambdaQueryWrapper<>();
        wrapper.gt(Stock::getStockQuantity, 0)
                .orderByDesc(Stock::getStockQuantity)
                .last("LIMIT " + size);
        List<Stock> stocks = stockMapper.selectList(wrapper);
        stocks.forEach(stock -> stockService.getStockByProductId(stock.getProductId()));
        return CompletableFuture.completedFuture(stocks.size());
    }

    @Override
    @Async("stockQueryExecutor")
    public CompletableFuture<List<StockDTO>> getStockAlertListAsync(Integer threshold) {
        int alertThreshold = threshold == null ? 10 : threshold;
        LambdaQueryWrapper<Stock> wrapper = new LambdaQueryWrapper<>();
        wrapper.le(Stock::getStockQuantity, alertThreshold)
                .orderByAsc(Stock::getStockQuantity);
        List<Stock> stocks = stockMapper.selectList(wrapper);
        return CompletableFuture.completedFuture(stockConverter.toDTOList(stocks));
    }

    @Override
    @Async("stockQueryExecutor")
    public CompletableFuture<Map<String, Object>> calculateStockValueAsync() {
        List<Stock> stocks = stockMapper.selectList(new LambdaQueryWrapper<>());
        int totalProducts = stocks.size();
        int totalStockQuantity = stocks.stream().map(Stock::getStockQuantity).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
        int totalFrozenQuantity = stocks.stream().map(Stock::getFrozenQuantity).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
        int totalAvailableQuantity = stocks.stream()
                .map(Stock::getAvailableQuantity)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        Map<String, Object> result = new HashMap<>();
        result.put("totalProducts", totalProducts);
        result.put("totalStockQuantity", totalStockQuantity);
        result.put("totalFrozenQuantity", totalFrozenQuantity);
        result.put("totalAvailableQuantity", totalAvailableQuantity);
        return CompletableFuture.completedFuture(result);
    }

    private StockOperationResult executeBatchMapOperation(Map<Long, Integer> productQuantityMap,
                                                          BatchOperation operation) {
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<String> errors = new ArrayList<>();

        if (productQuantityMap != null) {
            productQuantityMap.forEach((productId, quantity) -> {
                try {
                    boolean success = operation.execute(productId, quantity);
                    if (success) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                        errors.add("operation failed for productId=" + productId);
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    errors.add("operation exception for productId=" + productId + ", " + e.getMessage());
                }
            });
        }

        return new StockOperationResult(successCount.get(), failureCount.get(), errors);
    }

    private void evictCache(String cacheName, Object key) {
        if (cacheManager == null) {
            return;
        }
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }

    private void refreshStockCacheDirect(Long productId) {
        try {
            evictCache("stockCache", productId);
            evictCache("stockCache", "product:" + productId);
            stockService.getStockByProductId(productId);
        } catch (Exception e) {
            log.error("Failed to refresh stock cache asynchronously, productId={}", productId, e);
        }
    }

    @FunctionalInterface
    private interface BatchOperation {
        boolean execute(Long productId, Integer quantity);
    }
}
