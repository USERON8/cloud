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

@Component
@Slf4j
public class CacheWarmupManager {

    private final CacheManager cacheManager;
    private final List<CacheWarmupStrategy> warmupStrategies;

    public CacheWarmupManager(CacheManager cacheManager, List<CacheWarmupStrategy> warmupStrategies) {
        this.cacheManager = cacheManager;
        this.warmupStrategies = warmupStrategies != null ? warmupStrategies : new ArrayList<>();
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(100)
    public void onApplicationReady() {
        if (warmupStrategies.isEmpty()) {
            return;
        }
        executeWarmup();
    }

    @Async("warmupExecutor")
    public CompletableFuture<Void> executeWarmup() {
        long start = System.currentTimeMillis();
        int successCount = 0;
        int failureCount = 0;
        int warmedUpCount = 0;

        for (CacheWarmupStrategy strategy : warmupStrategies) {
            try {
                int count = strategy.warmup(cacheManager);
                warmedUpCount += Math.max(count, 0);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                log.error("Cache warmup strategy failed: {}", strategy.getStrategyName(), e);
            }
        }

        long cost = System.currentTimeMillis() - start;
        if (failureCount > 0) {
            log.warn("Cache warmup finished with failures: success={}, failure={}, warmedUp={}, costMs={}",
                    successCount, failureCount, warmedUpCount, cost);
        } else {
            log.debug("Cache warmup finished: success={}, warmedUp={}, costMs={}",
                    successCount, warmedUpCount, cost);
        }

        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> triggerWarmup() {
        return executeWarmup();
    }
}
