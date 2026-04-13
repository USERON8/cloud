package com.cloud.payment.service.support;

import com.cloud.common.metrics.TradeMetrics;
import com.cloud.payment.module.entity.PaymentOrderEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentOrderStateSupport {

  public static final String ORDER_STATUS_CREATED = "CREATED";
  public static final String ORDER_STATUS_PAID = "PAID";
  public static final String ORDER_STATUS_FAILED = "FAILED";

  private final TradeMetrics tradeMetrics;
  private final PaymentSecurityCacheService paymentSecurityCacheService;

  public void handlePersistedState(PaymentOrderEntity order, String previousStatus) {
    if (isTerminalStatus(order.getStatus())) {
      paymentSecurityCacheService.evictStatus(order.getPaymentNo());
    }
    if (!ORDER_STATUS_PAID.equals(previousStatus) && ORDER_STATUS_PAID.equals(order.getStatus())) {
      tradeMetrics.incrementPayment("success");
    } else if (!ORDER_STATUS_FAILED.equals(previousStatus)
        && ORDER_STATUS_FAILED.equals(order.getStatus())) {
      tradeMetrics.incrementPayment("failed");
    }
  }

  public boolean isTerminalStatus(String status) {
    return ORDER_STATUS_PAID.equals(status) || ORDER_STATUS_FAILED.equals(status);
  }
}
