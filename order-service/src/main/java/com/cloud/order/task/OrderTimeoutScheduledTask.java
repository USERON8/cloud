package com.cloud.order.task;

import com.cloud.common.annotation.DistributedLock;
import com.cloud.order.service.OrderTimeoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled tasks for timeout orders.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutScheduledTask {

    private final OrderTimeoutService orderTimeoutService;

    /**
     * Check timeout unpaid orders every 5 minutes.
     */
    @Scheduled(cron = "0 */5 * * * ?")
    @DistributedLock(
            key = "'schedule:order:timeout-check'",
            waitTime = 1,
            leaseTime = 300,
            failStrategy = DistributedLock.LockFailStrategy.RETURN_NULL
    )
    public void checkTimeoutOrders() {
        log.info("Start timeout-order check task");
        try {
            int cancelCount = orderTimeoutService.checkAndHandleTimeoutOrders();
            log.info("Finish timeout-order check task: cancelCount={}", cancelCount);
        } catch (Exception e) {
            log.error("Timeout-order check task failed", e);
        }
    }

    /**
     * Generate daily timeout order report at 01:00.
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @DistributedLock(
            key = "'schedule:order:timeout-report'",
            waitTime = 1,
            leaseTime = 600,
            failStrategy = DistributedLock.LockFailStrategy.RETURN_NULL
    )
    public void generateTimeoutOrderReport() {
        log.info("Start timeout-order report task");
        try {
            // Report generation extension point.
            log.info("Finish timeout-order report task");
        } catch (Exception e) {
            log.error("Timeout-order report task failed", e);
        }
    }
}
