package com.cloud.payment.task;

import com.cloud.common.annotation.DistributedLock;
import com.cloud.payment.service.PaymentCompensationService;
import com.xxl.job.core.context.XxlJobHelper;
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
    runJob("paymentOrderReconcileJob", paymentCompensationService.reconcilePendingOrders());
  }

  @XxlJob("paymentRefundRetryJob")
  @DistributedLock(
      key = "'xxl:payment:refund-retry'",
      waitTime = 1,
      leaseTime = 300,
      failStrategy = DistributedLock.LockFailStrategy.RETURN_NULL)
  public void retryPendingRefunds() {
    runJob("paymentRefundRetryJob", paymentCompensationService.retryPendingRefunds());
  }

  private void runJob(String jobName, int handledCount) {
    String message = jobName + " finished, handled records: " + handledCount;
    XxlJobHelper.log(message);
    log.info(message);
  }
}
