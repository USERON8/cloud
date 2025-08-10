package com.cloud.stock.controller;

import com.cloud.common.domain.PageResult;
import com.cloud.common.domain.Result;
import com.cloud.common.domain.dto.StockPageDTO;
import com.cloud.common.domain.vo.StockStatisticsVO;
import com.cloud.common.domain.vo.StockVO;
import com.cloud.stock.service.AsyncStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 异步库存管理控制器
 */
@RestController
@RequestMapping("/stock/async")
@RequiredArgsConstructor
@Slf4j
public class AsyncStockController {

    private final AsyncStockService asyncStockService;

    /**
     * 异步根据商品ID查询库存
     */
    @GetMapping("/product/{productId}")
    public CompletableFuture<Result<StockVO>> getByProductIdAsync(@PathVariable Long productId) {
        log.info("异步查询商品库存请求，productId: {}", productId);

        if (productId == null || productId <= 0) {
            log.warn("商品ID无效: {}", productId);
            return CompletableFuture.completedFuture(Result.error("商品ID不能为空"));
        }

        return asyncStockService.getByProductIdAsync(productId)
                .orTimeout(3, TimeUnit.SECONDS)
                .thenApply(stockVO -> {
                    // 返回空库存对象代替错误
                    return Result.success(Objects.requireNonNullElseGet(stockVO, StockVO::new));
                })
                .exceptionally(throwable -> {
                    log.error("异步查询商品库存异常, productId: {}", productId, throwable);
                    return Result.error("查询失败，请稍后重试");
                });
    }

    /**
     * 异步分页查询库存
     */
    @PostMapping("/page")
    public CompletableFuture<Result<PageResult<StockVO>>> pageQueryAsync(@RequestBody StockPageDTO pageDTO) {
        log.info("异步分页查询库存请求，查询条件: {}", pageDTO);

        return asyncStockService.pageQueryAsync(pageDTO)
                .thenApply(Result::success)
                .exceptionally(throwable -> {
                    log.error("异步分页查询库存异常", throwable);
                    return Result.error("查询失败: " + throwable.getMessage());
                });
    }

    /**
     * 异步批量查询库存
     */
    @PostMapping("/batch")
    public CompletableFuture<Result<List<StockVO>>> batchQueryAsync(@RequestBody List<Long> productIds) {
        log.info("异步批量查询库存请求，商品数量: {}", productIds.size());

        return asyncStockService.batchQueryAsync(productIds)
                .thenApply(Result::success)
                .exceptionally(throwable -> {
                    log.error("异步批量查询库存异常", throwable);
                    return Result.error("查询失败: " + throwable.getMessage());
                });
    }

    /**
     * 异步查询库存统计信息
     */
    @GetMapping("/statistics")
    public CompletableFuture<Result<StockStatisticsVO>> getStatisticsAsync() {
        log.info("异步查询库存统计请求");

        return asyncStockService.getStatisticsAsync()
                .thenApply(Result::success)
                .exceptionally(throwable -> {
                    log.error("异步查询库存统计异常", throwable);
                    return Result.error("查询失败: " + throwable.getMessage());
                });
    }

    /**
     * 并发查询多个商品库存（演示高并发场景）
     */
    @PostMapping("/concurrent")
    public CompletableFuture<Result<List<StockVO>>> concurrentQuery(@RequestBody List<Long> productIds) {
        log.info("并发查询多个商品库存，商品数量: {}", productIds.size());

        // 将商品ID列表拆分为多个异步任务
        List<CompletableFuture<StockVO>> futures = productIds.stream()
                .map(asyncStockService::getByProductIdAsync)
                .toList();

        // 等待所有任务完成
        CompletableFuture<Void> allOf = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        );

        return allOf.thenApply(v -> {
            List<StockVO> results = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .toList();

            log.info("并发查询完成，成功查询到 {} 个商品库存", results.size());
            return Result.success(results);
        }).exceptionally(throwable -> {
            log.error("并发查询异常", throwable);
            return Result.error("查询失败: " + throwable.getMessage());
        });
    }
}