package com.cloud.payment.messaging;

import com.cloud.common.messaging.event.PaymentSuccessEvent;
import com.cloud.common.messaging.event.RefundCompletedEvent;
import com.cloud.common.messaging.outbox.AbstractOutboxRelay;
import com.cloud.common.messaging.outbox.OutboxEvent;
import com.cloud.common.messaging.outbox.OutboxEventService;
import com.cloud.common.messaging.outbox.OutboxProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentOutboxRelay extends AbstractOutboxRelay {

  public PaymentOutboxRelay(
      OutboxEventService outboxEventService,
      OutboxProperties outboxProperties,
      StreamBridge streamBridge,
      ObjectMapper objectMapper,
      @Nullable MeterRegistry meterRegistry) {
    super(outboxEventService, outboxProperties, streamBridge, objectMapper, meterRegistry);
  }

  public void dispatch() {
    dispatchDueEvents();
  }

  @Override
  protected boolean relay(OutboxEvent event) throws Exception {
    String eventType = event.getEventType();
    if (eventType == null || eventType.isBlank()) {
      log.warn("Outbox event type missing: eventId={}", event.getEventId());
      return false;
    }

    return switch (eventType) {
      case "REFUND_COMPLETED" -> sendRefundCompleted(event);
      case "PAYMENT_SUCCESS" -> sendPaymentSuccess(event);
      default -> {
        log.warn(
            "Unknown outbox event type: eventId={}, eventType={}", event.getEventId(), eventType);
        yield false;
      }
    };
  }

  private boolean sendRefundCompleted(OutboxEvent event) throws Exception {
    RefundCompletedEvent payload = readPayload(event, RefundCompletedEvent.class);
    return sendMessage(
        "refundCompletedProducer-out-0",
        payload,
        payload.getRefundNo(),
        "REFUND_COMPLETED",
        payload.getEventId(),
        payload.getEventType());
  }

  private boolean sendPaymentSuccess(OutboxEvent event) throws Exception {
    PaymentSuccessEvent payload = readPayload(event, PaymentSuccessEvent.class);
    return sendMessage(
        "paymentSuccessProducer-out-0",
        payload,
        payload.getOrderNo(),
        "PAYMENT_SUCCESS",
        payload.getEventId(),
        payload.getEventType());
  }
}
