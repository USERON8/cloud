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
import org.springframework.cache.CacheManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;







@Slf4j
@Service
@RequiredArgsConstructor
public class StockAsyncServiceImpl implements StockAsyncService {

    


    private static final int BATCH_SIZE = 50;
    private final StockService stockService;
    private final StockMapper stockMapper;
    private final StockConverter stockConverter;
    private final CacheManager cacheManager;
    @Resource
    @Qualifier("stockQueryExecutor")
    private Executor stockQueryExecutor;
    @Resource
    @Qualifier("stockOperationExecutor")
    private Executor stockOperationExecutor;
    @Resource
    @Qualifier("stockCommonExecutor")
    private Executor stockCommonExecutor;

    @Override
    @Async("stockQueryExecutor")
    public CompletableFuture<List<StockDTO>> getStocksByProductIdsAsync(Collection<Long> productIds) {
        log.debug("寮傛鎵归噺鏌ヨ搴撳瓨锛屽晢鍝佹暟閲? {}", productIds.size());
        long startTime = System.currentTimeMillis();

        try {
            if (productIds == null || productIds.isEmpty()) {
                return CompletableFuture.completedFuture(Collections.emptyList());
            }

            
            if (productIds.size() <= BATCH_SIZE) {
                List<StockDTO> result = stockService.getStocksByProductIds(productIds);
                log.debug("寮傛鎵归噺鏌ヨ搴撳瓨瀹屾垚锛岃€楁椂: {}ms", System.currentTimeMillis() - startTime);
                return CompletableFuture.completedFuture(result);
            }

            
            List<Long> productIdList = new ArrayList<>(productIds);
            List<CompletableFuture<List<StockDTO>>> futures = new ArrayList<>();

            for (int i = 0; i < productIdList.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, productIdList.size());
                List<Long> batch = productIdList.subList(i, end);

                CompletableFuture<List<StockDTO>> future = CompletableFuture.supplyAsync(
                        () -> stockService.getStocksByProductIds(batch),
                        stockQueryExecutor
                );
                futures.add(future);
            }

            
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> {
                        List<StockDTO> result = futures.stream()
                                .map(CompletableFuture::join)
                                .flatMap(List::stream)
                                .collect(Collectors.toList());

                        

                        return result;
                    });

        } catch (Exception e) {
            log.error("寮傛鎵归噺鏌ヨ搴撳瓨澶辫触", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    @Async("stockQueryExecutor")
    public CompletableFuture<Map<Long, Boolean>> checkStocksSufficientAsync(Map<Long, Integer> productQuantityMap) {
        log.debug("寮傛鎵归噺妫€鏌ュ簱瀛橈紝鍟嗗搧鏁伴噺: {}", productQuantityMap.size());

        return CompletableFuture.supplyAsync(() -> {
            try {
                
                List<CompletableFuture<Map.Entry<Long, Boolean>>> futures =
                        productQuantityMap.entrySet().stream()
                                .map(entry -> CompletableFuture.supplyAsync(() -> {
                                    Long productId = entry.getKey();
                                    Integer quantity = entry.getValue();
                                    boolean sufficient = stockService.checkStockSufficient(productId, quantity);
                                    return Map.entry(productId, sufficient);
                                }, stockQueryExecutor))
                                .collect(Collectors.toList());

                
                Map<Long, Boolean> result = futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                
                return result;

            } catch (Exception e) {
                log.error("寮傛鎵归噺妫€鏌ュ簱瀛樺け璐?, e);
                throw new RuntimeException("鎵归噺妫€鏌ュ簱瀛樺け璐?, e);
            }
        }, stockQueryExecutor);
    }

    @Override
    @Async("stockOperationExecutor")
    public CompletableFuture<StockOperationResult> batchReserveStockAsync(Map<Long, Integer> productQuantityMap) {
        
        long startTime = System.currentTimeMillis();

        return CompletableFuture.supplyAsync(() -> {
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);
            List<String> errors = new ArrayList<>();

            try {
                
                productQuantityMap.forEach((productId, quantity) -> {
                    try {
                        boolean success = stockService.reserveStock(productId, quantity);
                        if (success) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                            errors.add("鍟嗗搧" + productId + "棰勭暀澶辫触");
                        }
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                        errors.add("鍟嗗搧" + productId + "棰勭暀寮傚父: " + e.getMessage());
                        log.warn("棰勭暀搴撳瓨澶辫触: productId={}", productId, e);
                    }
                });

                


                return new StockOperationResult(successCount.get(), failureCount.get(), errors);

            } catch (Exception e) {
                log.error("寮傛鎵归噺棰勭暀搴撳瓨澶辫触", e);
                errors.add("鎵归噺鎿嶄綔澶辫触: " + e.getMessage());
                return new StockOperationResult(successCount.get(), failureCount.get(), errors);
            }
        }, stockOperationExecutor);
    }

    @Override
    @Async("stockOperationExecutor")
    public CompletableFuture<StockOperationResult> batchReleaseStockAsync(Map<Long, Integer> productQuantityMap) {
        
        long startTime = System.currentTimeMillis();

        return CompletableFuture.supplyAsync(() -> {
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);
            List<String> errors = new ArrayList<>();

            try {
                productQuantityMap.forEach((productId, quantity) -> {
                    try {
                        boolean success = stockService.releaseReservedStock(productId, quantity);
                        if (success) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                            errors.add("鍟嗗搧" + productId + "閲婃斁澶辫触");
                        }
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                        errors.add("鍟嗗搧" + productId + "閲婃斁寮傚父: " + e.getMessage());
                        log.warn("閲婃斁搴撳瓨澶辫触: productId={}", productId, e);
                    }
                });

                


                return new StockOperationResult(successCount.get(), failureCount.get(), errors);

            } catch (Exception e) {
                log.error("寮傛鎵归噺閲婃斁搴撳瓨澶辫触", e);
                errors.add("鎵归噺鎿嶄綔澶辫触: " + e.getMessage());
                return new StockOperationResult(successCount.get(), failureCount.get(), errors);
            }
        }, stockOperationExecutor);
    }

    @Override
    @Async("stockOperationExecutor")
    public CompletableFuture<StockOperationResult> batchStockInAsync(List<StockInRequest> stockInList) {
        
        long startTime = System.currentTimeMillis();

        return CompletableFuture.supplyAsync(() -> {
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);
            List<String> errors = new ArrayList<>();

            try {
                stockInList.forEach(request -> {
                    try {
                        boolean success = stockService.stockIn(
                                request.getProductId(),
                                request.getQuantity(),
                                request.getRemark()
                        );
                        if (success) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                            errors.add("鍟嗗搧" + request.getProductId() + "鍏ュ簱澶辫触");
                        }
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                        errors.add("鍟嗗搧" + request.getProductId() + "鍏ュ簱寮傚父: " + e.getMessage());
                        log.warn("鍏ュ簱澶辫触: productId={}", request.getProductId(), e);
                    }
                });

                


                return new StockOperationResult(successCount.get(), failureCount.get(), errors);

            } catch (Exception e) {
                log.error("寮傛鎵归噺鍏ュ簱澶辫触", e);
                errors.add("鎵归噺鎿嶄綔澶辫触: " + e.getMessage());
                return new StockOperationResult(successCount.get(), failureCount.get(), errors);
            }
        }, stockOperationExecutor);
    }

    @Override
    @Async("stockOperationExecutor")
    public CompletableFuture<StockOperationResult> batchStockOutAsync(List<StockOutRequest> stockOutList) {
        
        long startTime = System.currentTimeMillis();

        return CompletableFuture.supplyAsync(() -> {
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);
            List<String> errors = new ArrayList<>();

            try {
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
                            errors.add("鍟嗗搧" + request.getProductId() + "鍑哄簱澶辫触");
                        }
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                        errors.add("鍟嗗搧" + request.getProductId() + "鍑哄簱寮傚父: " + e.getMessage());
                        log.warn("鍑哄簱澶辫触: productId={}", request.getProductId(), e);
                    }
                });

                


                return new StockOperationResult(successCount.get(), failureCount.get(), errors);

            } catch (Exception e) {
                log.error("寮傛鎵归噺鍑哄簱澶辫触", e);
                errors.add("鎵归噺鎿嶄綔澶辫触: " + e.getMessage());
                return new StockOperationResult(successCount.get(), failureCount.get(), errors);
            }
        }, stockOperationExecutor);
    }

    @Override
    @Async("stockCommonExecutor")
    public CompletableFuture<Void> refreshStockCacheAsync(Long productId) {
        log.debug("寮傛鍒锋柊搴撳瓨缂撳瓨: productId={}", productId);

        return CompletableFuture.runAsync(() -> {
            try {
                if (cacheManager != null) {
                    
                    Objects.requireNonNull(cacheManager.getCache("stock")).evict(productId);

                    
                    stockService.getStockByProductId(productId);

                    log.debug("鍒锋柊搴撳瓨缂撳瓨鎴愬姛: productId={}", productId);
                }
            } catch (Exception e) {
                log.error("鍒锋柊搴撳瓨缂撳瓨澶辫触: productId={}", productId, e);
            }
        }, stockCommonExecutor);
    }

    @Override
    @Async("stockCommonExecutor")
    public CompletableFuture<Void> batchRefreshStockCacheAsync(Collection<Long> productIds) {
        

        return CompletableFuture.runAsync(() -> {
            try {
                
                List<CompletableFuture<Void>> futures = productIds.stream()
                        .map(this::refreshStockCacheAsync)
                        .collect(Collectors.toList());

                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

                

            } catch (Exception e) {
                log.error("鎵归噺鍒锋柊搴撳瓨缂撳瓨澶辫触", e);
            }
        }, stockCommonExecutor);
    }

    @Override
    @Async("stockCommonExecutor")
    public CompletableFuture<Integer> preloadPopularStocksAsync(Integer limit) {
        
        long startTime = System.currentTimeMillis();

        return CompletableFuture.supplyAsync(() -> {
            try {
                
                LambdaQueryWrapper<Stock> wrapper = new LambdaQueryWrapper<>();
                wrapper.gt(Stock::getAvailableQuantity, 0)
                        .orderByDesc(Stock::getAvailableQuantity)
                        .last("LIMIT " + limit);

                List<Stock> stocks = stockMapper.selectList(wrapper);

                
                stocks.forEach(stock -> {
                    try {
                        stockService.getStockByProductId(stock.getProductId());
                    } catch (Exception e) {
                        log.warn("棰勫姞杞藉簱瀛樼紦瀛樺け璐? productId={}", stock.getProductId(), e);
                    }
                });

                

                return stocks.size();

            } catch (Exception e) {
                log.error("棰勫姞杞界儹闂ㄥ晢鍝佸簱瀛樺け璐?, e);
                return 0;
            }
        }, stockCommonExecutor);
    }

    @Override
    @Async("stockQueryExecutor")
    public CompletableFuture<List<StockDTO>> getStockAlertListAsync(Integer threshold) {
        

        return CompletableFuture.supplyAsync(() -> {
            try {
                LambdaQueryWrapper<Stock> wrapper = new LambdaQueryWrapper<>();
                wrapper.le(Stock::getAvailableQuantity, threshold)
                        .gt(Stock::getAvailableQuantity, 0)
                        .orderByAsc(Stock::getAvailableQuantity);

                List<Stock> stocks = stockMapper.selectList(wrapper);

                
                return stockConverter.toDTOList(stocks);

            } catch (Exception e) {
                log.error("缁熻搴撳瓨棰勮澶辫触", e);
                return Collections.emptyList();
            }
        }, stockQueryExecutor);
    }

    @Override
    @Async("stockQueryExecutor")
    public CompletableFuture<Map<String, Object>> calculateStockValueAsync() {
        

                return Collections.emptyMap();
            }
        }, stockQueryExecutor);
    }
}
