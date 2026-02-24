package com.cloud.stock.task;

import com.cloud.common.annotation.DistributedLock;
import com.cloud.stock.service.StockAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockAlertScheduledTask {

    private final StockAlertService stockAlertService;

    @Scheduled(cron = "0 5 * * * ?")
    @DistributedLock(
            key = "'schedule:stock:low-alert-check'",
            waitTime = 1,
            leaseTime = 300,
            failStrategy = DistributedLock.LockFailStrategy.RETURN_NULL
    )
    public void checkLowStockAlerts() {
        try {
            int alertCount = stockAlertService.checkAndSendLowStockAlerts();
            log.info("Low-stock alert task completed, alertCount={}", alertCount);
        } catch (Exception e) {
            log.error("Low-stock alert task failed", e);
        }
    }

    @Scheduled(cron = "0 0 2 * * ?")
    @DistributedLock(
            key = "'schedule:stock:low-alert-report'",
            waitTime = 1,
            leaseTime = 600,
            failStrategy = DistributedLock.LockFailStrategy.RETURN_NULL
    )
    public void generateDailyLowStockReport() {
        try {
            int alertCount = stockAlertService.checkAndSendLowStockAlerts();
            log.info("Daily low-stock report generated, alertCount={}", alertCount);
        } catch (Exception e) {
            log.error("Low-stock daily report task failed", e);
        }
    }
}
