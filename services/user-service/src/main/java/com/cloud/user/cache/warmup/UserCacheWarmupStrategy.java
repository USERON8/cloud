package com.cloud.user.cache.warmup;

import com.cloud.user.service.UserAsyncService;
import com.cloud.user.service.UserStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserCacheWarmupStrategy {

    private final UserAsyncService userAsyncService;
    private final UserStatisticsService userStatisticsService;

    @EventListener(ApplicationReadyEvent.class)
    @Async("userCommonAsyncExecutor")
    public void onApplicationReady() {
        long startTime = System.currentTimeMillis();
        try {
            warmupPopularUsers();
            warmupStatistics();
            log.debug("User cache warmup completed in {} ms", System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            log.error("User cache warmup failed", e);
        }
    }

    public void warmup() {
        try {
            warmupPopularUsers();
            warmupStatistics();
        } catch (Exception e) {
            log.error("User cache warmup failed", e);
        }
    }

    private void warmupPopularUsers() {
        try {
            Integer preloadCount = userAsyncService.preloadPopularUsersAsync(100)
                    .exceptionally(e -> {
                        log.warn("Preload popular users failed: {}", e.getMessage());
                        return 0;
                    })
                    .join();
            log.debug("Preloaded {} popular users", preloadCount);
        } catch (Exception e) {
            log.error("Warmup popular users failed", e);
        }
    }

    private void warmupStatistics() {
        try {
            userStatisticsService.getUserStatisticsOverviewAsync()
                    .exceptionally(e -> {
                        log.warn("Warmup statistics overview failed: {}", e.getMessage());
                        return null;
                    })
                    .join();

            userStatisticsService.getUserTypeDistributionAsync()
                    .exceptionally(e -> {
                        log.warn("Warmup user type distribution failed: {}", e.getMessage());
                        return null;
                    })
                    .join();

            userStatisticsService.getUserStatusDistributionAsync()
                    .exceptionally(e -> {
                        log.warn("Warmup user status distribution failed: {}", e.getMessage());
                        return null;
                    })
                    .join();
        } catch (Exception e) {
            log.error("Warmup statistics failed", e);
        }
    }

    public void manualWarmup() {
        warmup();
    }

    public void refreshWarmup() {
        try {
            warmup();
        } catch (Exception e) {
            log.error("Refresh warmup failed", e);
        }
    }
}
