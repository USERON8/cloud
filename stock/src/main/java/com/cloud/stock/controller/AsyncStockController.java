package com.cloud.stock.controller;

import com.cloud.stock.module.dto.StockPageDTO;
import com.cloud.stock.service.AsyncStockService;
import domain.PageResult;
import domain.Result;
import domain.vo.StockStatisticsVO;
import domain.vo.StockVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

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

        return asyncStockService.getByProductIdAsync(productId)
                .thenApply(stockVO -> {
                    if (stockVO == null) {
                        return Result.error("商品库存不存在");
                    }
                    return Result.success(stockVO);
                })
                .exceptionally(throwable -> {
                    log.error("异步查询商品库存异常", throwable);
                    return Result.error("查询失败: " + throwable.getMessage());
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
                    .filter(stockVO -> stockVO != null)
                    .toList();

            log.info("并发查询完成，成功查询到 {} 个商品库存", results.size());
            return Result.success(results);
        }).exceptionally(throwable -> {
            log.error("并发查询异常", throwable);
            return Result.error("查询失败: " + throwable.getMessage());
        });
    }
}
