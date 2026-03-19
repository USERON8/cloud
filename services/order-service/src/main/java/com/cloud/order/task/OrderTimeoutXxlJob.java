package com.cloud.order.task;

import com.cloud.common.annotation.DistributedLock;
import com.cloud.common.task.XxlJobSupport;
import com.cloud.order.service.OrderTimeoutService;
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
      failStrategy = DistributedLock.LockFailStrategy.RETURN_NULL)
  public void cancelTimeoutOrders() {
    int handledCount = orderTimeoutService.checkAndHandleTimeoutOrders();
    XxlJobSupport.logHandledCount(log, "orderTimeoutCheckJob", handledCount);
  }
}
