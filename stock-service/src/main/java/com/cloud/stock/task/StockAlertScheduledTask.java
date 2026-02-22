package com.cloud.stock.task;

import com.cloud.common.annotation.DistributedLock;
import com.cloud.stock.service.StockAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled tasks for stock alert.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockAlertScheduledTask {

    private final StockAlertService stockAlertService;

    /**
     * Check low stock alerts every hour.
     */
    @Scheduled(cron = "0 5 * * * ?")
    @DistributedLock(
            key = "'schedule:stock:low-alert-check'",
            waitTime = 1,
            leaseTime = 300,
            failStrategy = DistributedLock.LockFailStrategy.RETURN_NULL
    )
    public void checkLowStockAlerts() {
        log.info("Start low-stock alert task");
        try {
            int alertCount = stockAlertService.checkAndSendLowStockAlerts();
            log.info("Finish low-stock alert task: alertCount={}", alertCount);
        } catch (Exception e) {
            log.error("Low-stock alert task failed", e);
        }
    }

    /**
     * Generate daily low stock report at 02:00.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @DistributedLock(
            key = "'schedule:stock:low-alert-report'",
            waitTime = 1,
            leaseTime = 600,
            failStrategy = DistributedLock.LockFailStrategy.RETURN_NULL
    )
    public void generateDailyLowStockReport() {
        log.info("Start low-stock daily report task");
        try {
            int alertCount = stockAlertService.checkAndSendLowStockAlerts();
            log.info("Finish low-stock daily report task: alertCount={}", alertCount);
        } catch (Exception e) {
            log.error("Low-stock daily report task failed", e);
        }
    }
}
