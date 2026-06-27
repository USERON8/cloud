package com.cloud.payment.service.support;

import com.cloud.common.exception.SystemException;
import com.cloud.common.messaging.event.PaymentSuccessEvent;
import com.cloud.payment.messaging.PaymentMessageProducer;
import com.cloud.payment.module.entity.PaymentOrderEntity;
import com.cloud.payment.service.provider.PaymentProviderGateway;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentFlowSupport {

  private final List<PaymentProviderGateway> providerGateways;
  private final PaymentMessageProducer paymentMessageProducer;

  public PaymentProviderGateway resolveGateway(String channel) {
    for (PaymentProviderGateway gateway : providerGateways) {
      if (gateway.supports(channel)) {
        return gateway;
      }
    }
    return null;
  }

  public void publishPaymentSuccessIfNeeded(PaymentOrderEntity order, String previousStatus) {
    if (PaymentOrderStateSupport.ORDER_STATUS_PAID.equals(previousStatus)) {
      return;
    }
    if (order == null || !PaymentOrderStateSupport.ORDER_STATUS_PAID.equals(order.getStatus())) {
      return;
    }
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
    if (!paymentMessageProducer.sendPaymentSuccessEvent(event)) {
      throw new SystemException("failed to enqueue payment success event");
    }
  }
}
