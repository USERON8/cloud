package com.cloud.stock.controller;

import com.cloud.common.domain.dto.stock.StockDTO;
import com.cloud.common.lock.RedissonLockManager;
import com.cloud.common.result.Result;
import com.cloud.stock.service.impl.StockAnnotationServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 库存分布式锁测试控制器
 * 用于测试和演示分布式锁注解的功能
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/stock/lock-test")
@RequiredArgsConstructor
@Tag(name = "库存分布式锁测试", description = "分布式锁功能测试和演示接口")
public class StockLockTestController {

    private final StockAnnotationServiceImpl stockAnnotationService;
    private final RedissonLockManager redissonLockManager;

    /**
     * 测试注解式分布式锁 - 库存出库
     */
    @PostMapping("/stock-out/{productId}")
    @Operation(summary = "测试库存出库分布式锁", description = "使用@DistributedLock注解测试库存出库操作")
    public Result<Boolean> testStockOut(
            @Parameter(description = "商品ID") @PathVariable Long productId,
            @Parameter(description = "出库数量") @RequestParam Integer quantity) {

        log.info("🧪 测试库存出库分布式锁 - 商品ID: {}, 数量: {}", productId, quantity);

        try {
            boolean result = stockAnnotationService.stockOutWithAnnotation(productId, quantity);
            return Result.success("库存出库操作完成", result);
        } catch (Exception e) {
            log.error("❌ 库存出库测试失败", e);
            return Result.error("库存出库操作失败: " + e.getMessage());
        }
    }

    /**
     * 测试注解式分布式锁 - 库存预留（公平锁）
     */
    @PostMapping("/reserve/{productId}")
    @Operation(summary = "测试库存预留公平锁", description = "使用公平锁测试库存预留操作")
    public Result<Boolean> testReserveStock(
            @Parameter(description = "商品ID") @PathVariable Long productId,
            @Parameter(description = "预留数量") @RequestParam Integer quantity) {

        log.info("🧪 测试库存预留公平锁 - 商品ID: {}, 数量: {}", productId, quantity);

        try {
            boolean result = stockAnnotationService.reserveStockWithAnnotation(productId, quantity);
            return Result.success("库存预留操作完成", result);
        } catch (Exception e) {
            log.error("❌ 库存预留测试失败", e);
            return Result.error("库存预留操作失败: " + e.getMessage());
        }
    }

    /**
     * 测试注解式分布式锁 - 库存查询（读锁）
     */
    @GetMapping("/query/{productId}")
    @Operation(summary = "测试库存查询读锁", description = "使用读锁测试库存查询操作")
    public Result<StockDTO> testGetStock(
            @Parameter(description = "商品ID") @PathVariable Long productId) {

        log.info("🧪 测试库存查询读锁 - 商品ID: {}", productId);

        try {
            StockDTO result = stockAnnotationService.getStockWithAnnotation(productId);
            return Result.success("库存查询操作完成", result);
        } catch (Exception e) {
            log.error("❌ 库存查询测试失败", e);
            return Result.error("库存查询操作失败: " + e.getMessage());
        }
    }

    /**
     * 测试注解式分布式锁 - 库存更新（写锁）
     */
    @PutMapping("/update/{productId}")
    @Operation(summary = "测试库存更新写锁", description = "使用写锁测试库存更新操作")
    public Result<Boolean> testUpdateStock(
            @Parameter(description = "商品ID") @PathVariable Long productId,
            @Parameter(description = "新库存数量") @RequestParam Integer newQuantity) {

        log.info("🧪 测试库存更新写锁 - 商品ID: {}, 新数量: {}", productId, newQuantity);

        try {
            boolean result = stockAnnotationService.updateStockWithAnnotation(productId, newQuantity);
            return Result.success("库存更新操作完成", result);
        } catch (Exception e) {
            log.error("❌ 库存更新测试失败", e);
            return Result.error("库存更新操作失败: " + e.getMessage());
        }
    }

    /**
     * 测试注解式分布式锁 - 批量操作
     */
    @PostMapping("/batch")
    @Operation(summary = "测试批量库存操作", description = "使用复杂SpEL表达式测试批量库存操作")
    public Result<Integer> testBatchOperation(
            @Parameter(description = "商品ID列表") @RequestBody List<Long> productIds,
            @Parameter(description = "操作类型") @RequestParam String operation) {

        log.info("🧪 测试批量库存操作 - 操作类型: {}, 商品数量: {}", operation, productIds.size());

        try {
            int result = stockAnnotationService.batchStockOperationWithAnnotation(productIds, operation);
            return Result.success("批量库存操作完成", result);
        } catch (Exception e) {
            log.error("❌ 批量库存操作测试失败", e);
            return Result.error("批量库存操作失败: " + e.getMessage());
        }
    }

    /**
     * 测试快速查询（锁获取失败返回null）
     */
    @GetMapping("/quick/{productId}")
    @Operation(summary = "测试快速查询", description = "测试锁获取失败时返回null的情况")
    public Result<StockDTO> testQuickGet(
            @Parameter(description = "商品ID") @PathVariable Long productId) {

        log.info("🧪 测试快速查询 - 商品ID: {}", productId);

        try {
            StockDTO result = stockAnnotationService.quickGetStockWithAnnotation(productId);
            return Result.success("快速查询完成", result);
        } catch (Exception e) {
            log.error("❌ 快速查询测试失败", e);
            return Result.error("快速查询失败: " + e.getMessage());
        }
    }

    /**
     * 测试编程式分布式锁
     */
    @PostMapping("/programmatic/{productId}")
    @Operation(summary = "测试编程式分布式锁", description = "使用RedissonLockManager测试编程式分布式锁")
    public Result<String> testProgrammaticLock(
            @Parameter(description = "商品ID") @PathVariable Long productId) {

        log.info("🧪 测试编程式分布式锁 - 商品ID: {}", productId);

        String lockKey = "test:programmatic:" + productId;

        try {
            String result = redissonLockManager.executeWithLock(lockKey, 3, 10, TimeUnit.SECONDS, () -> {
                log.info("🔄 执行编程式锁保护的业务逻辑 - 商品ID: {}", productId);

                // 模拟业务处理
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                return "编程式锁执行成功 - 商品ID: " + productId;
            });

            return Result.success("编程式分布式锁测试完成", result);
        } catch (Exception e) {
            log.error("❌ 编程式分布式锁测试失败", e);
            return Result.error("编程式分布式锁测试失败: " + e.getMessage());
        }
    }

    /**
     * 并发测试 - 模拟多个线程同时访问
     */
    @PostMapping("/concurrent/{productId}")
    @Operation(summary = "并发测试", description = "模拟多个线程同时访问测试分布式锁效果")
    public Result<String> testConcurrent(
            @Parameter(description = "商品ID") @PathVariable Long productId,
            @Parameter(description = "并发线程数") @RequestParam(defaultValue = "5") Integer threadCount) {

        log.info("🧪 开始并发测试 - 商品ID: {}, 线程数: {}", productId, threadCount);

        try {
            CompletableFuture<?>[] futures = new CompletableFuture[threadCount];

            for (int i = 0; i < threadCount; i++) {
                final int threadIndex = i;
                futures[i] = CompletableFuture.runAsync(() -> {
                    try {
                        log.info("🚀 线程 {} 开始执行", threadIndex);
                        boolean result = stockAnnotationService.stockOutWithAnnotation(productId, 1);
                        log.info("✅ 线程 {} 执行完成，结果: {}", threadIndex, result);
                    } catch (Exception e) {
                        log.error("❌ 线程 {} 执行异常", threadIndex, e);
                    }
                });
            }

            // 等待所有线程完成
            CompletableFuture.allOf(futures).join();

            return Result.success("并发测试完成", "所有 " + threadCount + " 个线程执行完毕");
        } catch (Exception e) {
            log.error("❌ 并发测试失败", e);
            return Result.error("并发测试失败: " + e.getMessage());
        }
    }

    /**
     * 锁状态检查
     */
    @GetMapping("/lock-status/{productId}")
    @Operation(summary = "检查锁状态", description = "检查指定商品的锁状态")
    public Result<String> checkLockStatus(
            @Parameter(description = "商品ID") @PathVariable Long productId) {

        String lockKey = "stock:product:" + productId;

        boolean isLocked = redissonLockManager.isLocked(lockKey);
        boolean isHeldByCurrentThread = redissonLockManager.isHeldByCurrentThread(lockKey);
        long remainTime = redissonLockManager.remainTimeToLive(lockKey);

        String status = String.format(
                "锁键: %s, 是否被锁定: %s, 是否被当前线程持有: %s, 剩余时间: %dms",
                lockKey, isLocked, isHeldByCurrentThread, remainTime
        );

        log.info("🔍 锁状态检查 - {}", status);

        return Result.success("锁状态检查完成", status);
    }
}
