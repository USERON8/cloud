package com.cloud.payment.service.support;

import com.cloud.common.exception.SystemException;
import com.cloud.common.messaging.event.PaymentSuccessEvent;
import com.cloud.common.metrics.TradeMetrics;
import com.cloud.payment.messaging.PaymentSuccessTxProducer;
import com.cloud.payment.module.entity.PaymentOrderEntity;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class PaymentOrderStateSupport {

  public static final String ORDER_STATUS_CREATED = "CREATED";
  public static final String ORDER_STATUS_PAID = "PAID";
  public static final String ORDER_STATUS_FAILED = "FAILED";

  private final ObjectProvider<PaymentSuccessTxProducer> paymentSuccessTxProducerProvider;
  private final TradeMetrics tradeMetrics;
  private final PaymentSecurityCacheService paymentSecurityCacheService;

  public void markPaid(PaymentOrderEntity order, String providerTxnNo, LocalDateTime paidAt) {
    order.setStatus(ORDER_STATUS_PAID);
    if (StringUtils.hasText(providerTxnNo)) {
      order.setProviderTxnNo(providerTxnNo);
    }
    if (order.getPaidAt() == null) {
      order.setPaidAt(paidAt != null ? paidAt : LocalDateTime.now());
    }
  }

  public void markFailed(PaymentOrderEntity order) {
    order.setStatus(ORDER_STATUS_FAILED);
  }

  public void handlePersistedState(PaymentOrderEntity order, String previousStatus) {
    if (isTerminalStatus(order.getStatus())) {
      paymentSecurityCacheService.evictStatus(order.getPaymentNo());
    }
    if (!ORDER_STATUS_PAID.equals(previousStatus) && ORDER_STATUS_PAID.equals(order.getStatus())) {
      publishPaymentSuccess(order);
      tradeMetrics.incrementPayment("success");
    } else if (!ORDER_STATUS_FAILED.equals(previousStatus)
        && ORDER_STATUS_FAILED.equals(order.getStatus())) {
      tradeMetrics.incrementPayment("failed");
    }
  }

  public boolean isTerminalStatus(String status) {
    return ORDER_STATUS_PAID.equals(status) || ORDER_STATUS_FAILED.equals(status);
  }

  private void publishPaymentSuccess(PaymentOrderEntity order) {
    PaymentSuccessEvent event =
        PaymentSuccessEvent.builder()
            .paymentId(order.getId())
            .orderNo(order.getMainOrderNo())
            .subOrderNo(order.getSubOrderNo())
            .userId(order.getUserId())
            .amount(order.getAmount())
            .paymentMethod(order.getChannel())
            .transactionNo(order.getProviderTxnNo())
            .build();
    PaymentSuccessTxProducer producer = paymentSuccessTxProducerProvider.getIfAvailable();
    if (producer == null) {
      throw new SystemException("rocketmq template is not configured for tx producer");
    }
    producer.send(event);
  }
}
