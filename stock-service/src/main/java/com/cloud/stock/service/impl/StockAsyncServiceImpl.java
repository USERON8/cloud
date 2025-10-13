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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 库存异步服务实现
 * 使用CompletableFuture实现高并发库存操作
 *
 * @author what's up
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockAsyncServiceImpl implements StockAsyncService {

    private final StockService stockService;
    private final StockMapper stockMapper;
    private final StockConverter stockConverter;
    private final CacheManager cacheManager;

    /**
     * 批处理大小
     */
    private static final int BATCH_SIZE = 50;

    @Override
    @Async("stockQueryExecutor")
    public CompletableFuture<List<StockDTO>> getStocksByProductIdsAsync(Collection<Long> productIds) {
        log.debug("异步批量查询库存，商品数量: {}", productIds.size());
        long startTime = System.currentTimeMillis();

        try {
            if (productIds == null || productIds.isEmpty()) {
                return CompletableFuture.completedFuture(Collections.emptyList());
            }

            // 小批量直接查询
            if (productIds.size() <= BATCH_SIZE) {
                List<StockDTO> result = stockService.getStocksByProductIds(productIds);
                log.debug("异步批量查询库存完成，耗时: {}ms", System.currentTimeMillis() - startTime);
                return CompletableFuture.completedFuture(result);
            }

            // 大批量：分批并发查询
            List<Long> productIdList = new ArrayList<>(productIds);
            List<CompletableFuture<List<StockDTO>>> futures = new ArrayList<>();

            for (int i = 0; i < productIdList.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, productIdList.size());
                List<Long> batch = productIdList.subList(i, end);

                CompletableFuture<List<StockDTO>> future = CompletableFuture.supplyAsync(
                        () -> stockService.getStocksByProductIds(batch)
                );
                futures.add(future);
            }

            // 等待所有批次完成并合并
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> {
                        List<StockDTO> result = futures.stream()
                                .map(CompletableFuture::join)
                                .flatMap(List::stream)
                                .collect(Collectors.toList());

                        log.info("异步批量查询库存完成，总数: {}, 耗时: {}ms",
                                result.size(), System.currentTimeMillis() - startTime);
                        return result;
                    });

        } catch (Exception e) {
            log.error("异步批量查询库存失败", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    @Async("stockQueryExecutor")
    public CompletableFuture<Map<Long, Boolean>> checkStocksSufficientAsync(Map<Long, Integer> productQuantityMap) {
        log.debug("异步批量检查库存，商品数量: {}", productQuantityMap.size());

        return CompletableFuture.supplyAsync(() -> {
            try {
                // 并发检查每个商品的库存
                List<CompletableFuture<Map.Entry<Long, Boolean>>> futures =
                        productQuantityMap.entrySet().stream()
                                .map(entry -> CompletableFuture.supplyAsync(() -> {
                                    Long productId = entry.getKey();
                                    Integer quantity = entry.getValue();
                                    boolean sufficient = stockService.checkStockSufficient(productId, quantity);
                                    return Map.entry(productId, sufficient);
                                }))
                                .collect(Collectors.toList());

                // 等待所有检查完成
                Map<Long, Boolean> result = futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                log.info("异步批量检查库存完成，商品数: {}", result.size());
                return result;

            } catch (Exception e) {
                log.error("异步批量检查库存失败", e);
                throw new RuntimeException("批量检查库存失败", e);
            }
        });
    }

    @Override
    @Async("stockOperationExecutor")
    public CompletableFuture<StockOperationResult> batchReserveStockAsync(Map<Long, Integer> productQuantityMap) {
        log.info("异步批量预留库存，商品数量: {}", productQuantityMap.size());
        long startTime = System.currentTimeMillis();

        return CompletableFuture.supplyAsync(() -> {
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);
            List<String> errors = Collections.synchronizedList(new ArrayList<>());

            try {
                // 并发预留库存
                productQuantityMap.forEach((productId, quantity) -> {
                    try {
                        boolean success = stockService.reserveStock(productId, quantity);
                        if (success) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                            errors.add("商品" + productId + "预留失败");
                        }
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                        errors.add("商品" + productId + "预留异常: " + e.getMessage());
                        log.warn("预留库存失败: productId={}", productId, e);
                    }
                });

                log.info("异步批量预留库存完成，成功: {}, 失败: {}, 耗时: {}ms",
                        successCount.get(), failureCount.get(), System.currentTimeMillis() - startTime);

                return new StockOperationResult(successCount.get(), failureCount.get(), errors);

            } catch (Exception e) {
                log.error("异步批量预留库存失败", e);
                errors.add("批量操作失败: " + e.getMessage());
                return new StockOperationResult(successCount.get(), failureCount.get(), errors);
            }
        });
    }

    @Override
    @Async("stockOperationExecutor")
    public CompletableFuture<StockOperationResult> batchReleaseStockAsync(Map<Long, Integer> productQuantityMap) {
        log.info("异步批量释放库存，商品数量: {}", productQuantityMap.size());
        long startTime = System.currentTimeMillis();

        return CompletableFuture.supplyAsync(() -> {
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);
            List<String> errors = Collections.synchronizedList(new ArrayList<>());

            try {
                productQuantityMap.forEach((productId, quantity) -> {
                    try {
                        boolean success = stockService.releaseReservedStock(productId, quantity);
                        if (success) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                            errors.add("商品" + productId + "释放失败");
                        }
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                        errors.add("商品" + productId + "释放异常: " + e.getMessage());
                        log.warn("释放库存失败: productId={}", productId, e);
                    }
                });

                log.info("异步批量释放库存完成，成功: {}, 失败: {}, 耗时: {}ms",
                        successCount.get(), failureCount.get(), System.currentTimeMillis() - startTime);

                return new StockOperationResult(successCount.get(), failureCount.get(), errors);

            } catch (Exception e) {
                log.error("异步批量释放库存失败", e);
                errors.add("批量操作失败: " + e.getMessage());
                return new StockOperationResult(successCount.get(), failureCount.get(), errors);
            }
        });
    }

    @Override
    @Async("stockOperationExecutor")
    public CompletableFuture<StockOperationResult> batchStockInAsync(List<StockInRequest> stockInList) {
        log.info("异步批量入库，数量: {}", stockInList.size());
        long startTime = System.currentTimeMillis();

        return CompletableFuture.supplyAsync(() -> {
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);
            List<String> errors = Collections.synchronizedList(new ArrayList<>());

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
                            errors.add("商品" + request.getProductId() + "入库失败");
                        }
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                        errors.add("商品" + request.getProductId() + "入库异常: " + e.getMessage());
                        log.warn("入库失败: productId={}", request.getProductId(), e);
                    }
                });

                log.info("异步批量入库完成，成功: {}, 失败: {}, 耗时: {}ms",
                        successCount.get(), failureCount.get(), System.currentTimeMillis() - startTime);

                return new StockOperationResult(successCount.get(), failureCount.get(), errors);

            } catch (Exception e) {
                log.error("异步批量入库失败", e);
                errors.add("批量操作失败: " + e.getMessage());
                return new StockOperationResult(successCount.get(), failureCount.get(), errors);
            }
        });
    }

    @Override
    @Async("stockOperationExecutor")
    public CompletableFuture<StockOperationResult> batchStockOutAsync(List<StockOutRequest> stockOutList) {
        log.info("异步批量出库，数量: {}", stockOutList.size());
        long startTime = System.currentTimeMillis();

        return CompletableFuture.supplyAsync(() -> {
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);
            List<String> errors = Collections.synchronizedList(new ArrayList<>());

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
                            errors.add("商品" + request.getProductId() + "出库失败");
                        }
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                        errors.add("商品" + request.getProductId() + "出库异常: " + e.getMessage());
                        log.warn("出库失败: productId={}", request.getProductId(), e);
                    }
                });

                log.info("异步批量出库完成，成功: {}, 失败: {}, 耗时: {}ms",
                        successCount.get(), failureCount.get(), System.currentTimeMillis() - startTime);

                return new StockOperationResult(successCount.get(), failureCount.get(), errors);

            } catch (Exception e) {
                log.error("异步批量出库失败", e);
                errors.add("批量操作失败: " + e.getMessage());
                return new StockOperationResult(successCount.get(), failureCount.get(), errors);
            }
        });
    }

    @Override
    @Async("stockCommonExecutor")
    public CompletableFuture<Void> refreshStockCacheAsync(Long productId) {
        log.debug("异步刷新库存缓存: productId={}", productId);

        return CompletableFuture.runAsync(() -> {
            try {
                if (cacheManager != null) {
                    // 清除缓存
                    Objects.requireNonNull(cacheManager.getCache("stock")).evict(productId);

                    // 预加载新数据
                    stockService.getStockByProductId(productId);

                    log.debug("刷新库存缓存成功: productId={}", productId);
                }
            } catch (Exception e) {
                log.error("刷新库存缓存失败: productId={}", productId, e);
            }
        });
    }

    @Override
    @Async("stockCommonExecutor")
    public CompletableFuture<Void> batchRefreshStockCacheAsync(Collection<Long> productIds) {
        log.info("异步批量刷新库存缓存，数量: {}", productIds.size());

        return CompletableFuture.runAsync(() -> {
            try {
                // 并发刷新
                List<CompletableFuture<Void>> futures = productIds.stream()
                        .map(this::refreshStockCacheAsync)
                        .collect(Collectors.toList());

                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

                log.info("批量刷新库存缓存完成，数量: {}", productIds.size());

            } catch (Exception e) {
                log.error("批量刷新库存缓存失败", e);
            }
        });
    }

    @Override
    @Async("stockCommonExecutor")
    public CompletableFuture<Integer> preloadPopularStocksAsync(Integer limit) {
        log.info("异步预加载热门商品库存，数量: {}", limit);
        long startTime = System.currentTimeMillis();

        return CompletableFuture.supplyAsync(() -> {
            try {
                // 查询热门商品（可用库存量大的）
                LambdaQueryWrapper<Stock> wrapper = new LambdaQueryWrapper<>();
                wrapper.gt(Stock::getAvailableQuantity, 0)
                        .orderByDesc(Stock::getAvailableQuantity)
                        .last("LIMIT " + limit);

                List<Stock> stocks = stockMapper.selectList(wrapper);

                // 预加载到缓存
                stocks.forEach(stock -> {
                    try {
                        stockService.getStockByProductId(stock.getProductId());
                    } catch (Exception e) {
                        log.warn("预加载库存缓存失败: productId={}", stock.getProductId(), e);
                    }
                });

                log.info("预加载热门商品库存完成，数量: {}, 耗时: {}ms",
                        stocks.size(), System.currentTimeMillis() - startTime);
                return stocks.size();

            } catch (Exception e) {
                log.error("预加载热门商品库存失败", e);
                return 0;
            }
        });
    }

    @Override
    @Async("stockQueryExecutor")
    public CompletableFuture<List<StockDTO>> getStockAlertListAsync(Integer threshold) {
        log.info("异步统计库存预警，阈值: {}", threshold);

        return CompletableFuture.supplyAsync(() -> {
            try {
                LambdaQueryWrapper<Stock> wrapper = new LambdaQueryWrapper<>();
                wrapper.le(Stock::getAvailableQuantity, threshold)
                        .gt(Stock::getAvailableQuantity, 0)
                        .orderByAsc(Stock::getAvailableQuantity);

                List<Stock> stocks = stockMapper.selectList(wrapper);

                log.info("库存预警统计完成，预警商品数: {}", stocks.size());
                return stockConverter.toDTOList(stocks);

            } catch (Exception e) {
                log.error("统计库存预警失败", e);
                return Collections.emptyList();
            }
        });
    }

    @Override
    @Async("stockQueryExecutor")
    public CompletableFuture<Map<String, Object>> calculateStockValueAsync() {
        log.info("异步计算库存总值");

        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Stock> stocks = stockMapper.selectList(null);

                long totalQuantity = stocks.stream()
                        .mapToLong(Stock::getAvailableQuantity)
                        .sum();

                long reservedQuantity = stocks.stream()
                        .mapToLong(Stock::getReservedQuantity)
                        .sum();

                long totalProducts = stocks.size();

                Map<String, Object> result = new HashMap<>();
                result.put("totalQuantity", totalQuantity);
                result.put("reservedQuantity", reservedQuantity);
                result.put("availableQuantity", totalQuantity - reservedQuantity);
                result.put("totalProducts", totalProducts);

                log.info("库存总值计算完成: {}", result);
                return result;

            } catch (Exception e) {
                log.error("计算库存总值失败", e);
                return Collections.emptyMap();
            }
        });
    }
}
