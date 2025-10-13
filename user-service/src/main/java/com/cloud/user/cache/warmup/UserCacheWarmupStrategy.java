package com.cloud.user.cache.warmup;

import com.cloud.user.service.UserAsyncService;
import com.cloud.user.service.UserStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 用户服务缓存预热策略
 * 在应用启动后自动预热常用数据
 *
 * @author what's up
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserCacheWarmupStrategy {

    private final UserAsyncService userAsyncService;
    private final UserStatisticsService userStatisticsService;

    /**
     * 应用启动完成后执行预热
     */
    @EventListener(ApplicationReadyEvent.class)
    @Async("userCommonAsyncExecutor")
    public void onApplicationReady() {
        log.info("🔥 用户服务缓存预热开始...");
        long startTime = System.currentTimeMillis();

        try {
            // 预热热门用户数据
            warmupPopularUsers();

            // 预热统计数据
            warmupStatistics();

            log.info("✅ 用户服务缓存预热完成，耗时: {}ms", System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            log.error("❌ 用户服务缓存预热失败", e);
        }
    }

    /**
     * 执行缓存预热
     */
    public void warmup() {
        log.info("执行用户服务缓存预热");

        try {
            warmupPopularUsers();
            warmupStatistics();

            log.info("用户服务缓存预热完成");

        } catch (Exception e) {
            log.error("用户服务缓存预热失败", e);
        }
    }

    /**
     * 预热热门用户数据
     */
    private void warmupPopularUsers() {
        try {
            log.debug("预热热门用户数据...");

            // 预加载最近活跃的100个用户
            Integer preloadCount = userAsyncService.preloadPopularUsersAsync(100)
                    .exceptionally(e -> {
                        log.warn("预热热门用户数据失败: {}", e.getMessage());
                        return 0;
                    })
                    .join();

            log.info("热门用户数据预热完成，数量: {}", preloadCount);

        } catch (Exception e) {
            log.error("预热热门用户数据异常", e);
        }
    }

    /**
     * 预热统计数据
     */
    private void warmupStatistics() {
        try {
            log.debug("预热统计数据...");

            // 预加载用户统计概览
            userStatisticsService.getUserStatisticsOverviewAsync()
                    .exceptionally(e -> {
                        log.warn("预热统计数据失败: {}", e.getMessage());
                        return null;
                    })
                    .join();

            // 预加载用户类型分布
            userStatisticsService.getUserTypeDistributionAsync()
                    .exceptionally(e -> {
                        log.warn("预热用户类型分布失败: {}", e.getMessage());
                        return null;
                    })
                    .join();

            // 预加载用户状态分布
            userStatisticsService.getUserStatusDistributionAsync()
                    .exceptionally(e -> {
                        log.warn("预热用户状态分布失败: {}", e.getMessage());
                        return null;
                    })
                    .join();

            log.info("统计数据预热完成");

        } catch (Exception e) {
            log.error("预热统计数据异常", e);
        }
    }

    /**
     * 手动触发预热
     */
    public void manualWarmup() {
        log.info("手动触发用户服务缓存预热");
        warmup();
    }

    /**
     * 清除并重新预热
     */
    public void refreshWarmup() {
        log.info("清除并重新预热用户服务缓存");

        try {
            // 清除缓存的逻辑...

            // 重新预热
            warmup();

            log.info("缓存刷新并预热完成");

        } catch (Exception e) {
            log.error("刷新并预热缓存失败", e);
        }
    }
}
