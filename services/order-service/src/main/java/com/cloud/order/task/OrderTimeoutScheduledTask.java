package com.cloud.order.task;

import com.cloud.common.annotation.DistributedLock;
import com.cloud.order.service.OrderTimeoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;




@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutScheduledTask {

    private final OrderTimeoutService orderTimeoutService;

    


    @Scheduled(cron = "0 */5 * * * ?")
    @DistributedLock(
            key = "'schedule:order:timeout-check'",
            waitTime = 1,
            leaseTime = 300,
            failStrategy = DistributedLock.LockFailStrategy.RETURN_NULL
    )
    public void checkTimeoutOrders() {
        
        try {
            int cancelCount = orderTimeoutService.checkAndHandleTimeoutOrders();
            
        } catch (Exception e) {
            log.error("Timeout-order check task failed", e);
        }
    }

    


    @Scheduled(cron = "0 0 1 * * ?")
    @DistributedLock(
            key = "'schedule:order:timeout-report'",
            waitTime = 1,
            leaseTime = 600,
            failStrategy = DistributedLock.LockFailStrategy.RETURN_NULL
    )
    public void generateTimeoutOrderReport() {
        
        try {
            
            
        } catch (Exception e) {
            log.error("Timeout-order report task failed", e);
        }
    }
}
