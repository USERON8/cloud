package com.cloud.order.task;

import com.cloud.common.annotation.DistributedLock;
import com.cloud.common.task.XxlJobSupport;
import com.cloud.order.service.OrderAutomationService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAutomationXxlJob {

  private final OrderAutomationService orderAutomationService;

  @XxlJob("orderAutoConfirmReceiptJob")
  @DistributedLock(
      key = "'xxl:order:auto-confirm-receipt'",
      waitTime = 1,
      leaseTime = 300,
      failStrategy = DistributedLock.LockFailStrategy.RETURN_NULL)
  public void autoConfirmReceipt() {
    XxlJobSupport.logHandledCount(
        log, "orderAutoConfirmReceiptJob", orderAutomationService.autoConfirmShippedOrders());
  }

  @XxlJob("afterSaleAutoApproveJob")
  @DistributedLock(
      key = "'xxl:order:after-sale-auto-approve'",
      waitTime = 1,
      leaseTime = 300,
      failStrategy = DistributedLock.LockFailStrategy.RETURN_NULL)
  public void autoApproveAfterSales() {
    XxlJobSupport.logHandledCount(
        log, "afterSaleAutoApproveJob", orderAutomationService.autoApproveTimedOutAfterSales());
  }
}
