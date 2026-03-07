package com.cloud.order.task;

import com.cloud.common.annotation.DistributedLock;
import com.cloud.order.service.OrderTimeoutService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutXxlJob {

    private final OrderTimeoutService orderTimeoutService;

    @XxlJob("orderTimeoutCheckJob")
    @DistributedLock(
            key = "'xxl:order:timeout-check'",
            waitTime = 1,
            leaseTime = 300,
            failStrategy = DistributedLock.LockFailStrategy.RETURN_NULL
    )
    public void checkTimeoutOrders() {
        try {
            int cancelCount = orderTimeoutService.checkAndHandleTimeoutOrders();
            String message = "orderTimeoutCheckJob finished, cancelled timeout orders: " + cancelCount;
            XxlJobHelper.log(message);
            log.info(message);
        } catch (Exception ex) {
            String message = "orderTimeoutCheckJob failed: " + ex.getMessage();
            XxlJobHelper.log(message);
            log.error("orderTimeoutCheckJob failed", ex);
            throw ex;
        }
    }
}
