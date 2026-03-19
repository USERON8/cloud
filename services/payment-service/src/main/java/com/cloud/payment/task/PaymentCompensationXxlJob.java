package com.cloud.payment.task;

import com.cloud.common.annotation.DistributedLock;
import com.cloud.common.task.XxlJobSupport;
import com.cloud.payment.service.PaymentCompensationService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCompensationXxlJob {

  private final PaymentCompensationService paymentCompensationService;

  @XxlJob("paymentOrderReconcileJob")
  @DistributedLock(
      key = "'xxl:payment:order-reconcile'",
      waitTime = 1,
      leaseTime = 300,
      failStrategy = DistributedLock.LockFailStrategy.RETURN_NULL)
  public void reconcilePendingOrders() {
    XxlJobSupport.logHandledCount(
        log, "paymentOrderReconcileJob", paymentCompensationService.reconcilePendingOrders());
  }

  @XxlJob("paymentRefundRetryJob")
  @DistributedLock(
      key = "'xxl:payment:refund-retry'",
      waitTime = 1,
      leaseTime = 300,
      failStrategy = DistributedLock.LockFailStrategy.RETURN_NULL)
  public void retryPendingRefunds() {
    XxlJobSupport.logHandledCount(
        log, "paymentRefundRetryJob", paymentCompensationService.retryPendingRefunds());
  }
}
