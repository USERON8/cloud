package com.cloud.common.cache.warmup;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 缓存预热管理器
 * 
 * 主要功能：
 * - 应用启动时自动执行缓存预热
 * - 支持异步预热，不阻塞启动过程
 * - 可扩展的预热策略接口
 * - 预热进度监控和日志记录
 *
 * @author CloudDevAgent
 * @version 1.0
 * @since 2025-09-27
 */
@Component
@Slf4j
public class CacheWarmupManager {
    
    private final CacheManager cacheManager;
    private final List<CacheWarmupStrategy> warmupStrategies;
    
    public CacheWarmupManager(CacheManager cacheManager, 
                             List<CacheWarmupStrategy> warmupStrategies) {
        this.cacheManager = cacheManager;
        this.warmupStrategies = warmupStrategies != null ? warmupStrategies : new ArrayList<>();
    }
    
    /**
     * 应用启动完成后执行缓存预热
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(100) // 确保在其他初始化完成后执行
    public void onApplicationReady() {
        if (warmupStrategies.isEmpty()) {
            log.info("未配置缓存预热策略，跳过预热");
            return;
        }
        
        log.info("应用启动完成，开始执行缓存预热...");
        executeWarmup();
    }
    
    /**
     * 执行缓存预热
     */
    @Async("warmupExecutor")
    public CompletableFuture<Void> executeWarmup() {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("开始缓存预热，共 {} 个策略", warmupStrategies.size());
            
            List<CompletableFuture<WarmupResult>> futures = new ArrayList<>();
            
            // 并行执行各预热策略
            for (CacheWarmupStrategy strategy : warmupStrategies) {
                CompletableFuture<WarmupResult> future = CompletableFuture
                        .supplyAsync(() -> executeStrategy(strategy))
                        .exceptionally(throwable -> {
                            log.error("预热策略 {} 执行失败: {}", 
                                    strategy.getClass().getSimpleName(), 
                                    throwable.getMessage(), throwable);
                            return new WarmupResult(strategy.getClass().getSimpleName(), 
                                                  false, 0, throwable.getMessage());
                        });
                futures.add(future);
            }
            
            // 等待所有预热策略完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenRun(() -> {
                        long duration = System.currentTimeMillis() - startTime;
                        logWarmupResults(futures, duration);
                    })
                    .get(); // 等待完成
            
        } catch (Exception e) {
            log.error("缓存预热过程出现异常: {}", e.getMessage(), e);
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * 执行单个预热策略
     */
    private WarmupResult executeStrategy(CacheWarmupStrategy strategy) {
        String strategyName = strategy.getClass().getSimpleName();
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("开始执行预热策略: {}", strategyName);
            
            int warmedUpCount = strategy.warmup(cacheManager);
            long duration = System.currentTimeMillis() - startTime;
            
            log.info("预热策略 {} 完成: 预热 {} 项数据，耗时 {}ms", 
                    strategyName, warmedUpCount, duration);
            
            return new WarmupResult(strategyName, true, warmedUpCount, null);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("预热策略 {} 执行失败，耗时 {}ms: {}", 
                    strategyName, duration, e.getMessage(), e);
            
            return new WarmupResult(strategyName, false, 0, e.getMessage());
        }
    }
    
    /**
     * 记录预热结果
     */
    private void logWarmupResults(List<CompletableFuture<WarmupResult>> futures, long totalDuration) {
        log.info("========================================");
        log.info("缓存预热完成，总耗时: {}ms", totalDuration);
        log.info("========================================");
        
        int successCount = 0;
        int failureCount = 0;
        int totalWarmedUp = 0;
        
        for (CompletableFuture<WarmupResult> future : futures) {
            try {
                WarmupResult result = future.get();
                if (result.isSuccess()) {
                    successCount++;
                    totalWarmedUp += result.getWarmedUpCount();
                    log.info("✅ {}: 成功预热 {} 项数据", 
                            result.getStrategyName(), result.getWarmedUpCount());
                } else {
                    failureCount++;
                    log.error("❌ {}: 预热失败 - {}", 
                            result.getStrategyName(), result.getErrorMessage());
                }
            } catch (Exception e) {
                failureCount++;
                log.error("❌ 获取预热结果失败: {}", e.getMessage());
            }
        }
        
        log.info("预热结果汇总:");
        log.info("  成功策略: {} 个", successCount);
        log.info("  失败策略: {} 个", failureCount);
        log.info("  总预热数据: {} 项", totalWarmedUp);
        log.info("  整体成功率: {:.1f}%", 
                successCount * 100.0 / (successCount + failureCount));
        log.info("========================================");
    }
    
    /**
     * 手动触发预热
     */
    public CompletableFuture<Void> triggerWarmup() {
        log.info("手动触发缓存预热");
        return executeWarmup();
    }
    
    /**
     * 预热结果内部类
     */
    private static class WarmupResult {
        private final String strategyName;
        private final boolean success;
        private final int warmedUpCount;
        private final String errorMessage;
        
        public WarmupResult(String strategyName, boolean success, 
                          int warmedUpCount, String errorMessage) {
            this.strategyName = strategyName;
            this.success = success;
            this.warmedUpCount = warmedUpCount;
            this.errorMessage = errorMessage;
        }
        
        public String getStrategyName() { return strategyName; }
        public boolean isSuccess() { return success; }
        public int getWarmedUpCount() { return warmedUpCount; }
        public String getErrorMessage() { return errorMessage; }
    }
}
