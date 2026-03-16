package com.cloud.payment.messaging;

import com.cloud.common.messaging.event.RefundCompletedEvent;
import com.cloud.common.messaging.outbox.OutboxEventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentMessageProducer {

  private final OutboxEventService outboxEventService;
  private final ObjectMapper objectMapper;

  public boolean sendRefundCompletedEvent(RefundCompletedEvent event) {
    if (event == null) {
      return false;
    }
    try {
      if (event.getEventId() == null) {
        event.setEventId(UUID.randomUUID().toString());
      }
      if (event.getTimestamp() == null) {
        event.setTimestamp(System.currentTimeMillis());
      }
      if (event.getEventType() == null || event.getEventType().isBlank()) {
        event.setEventType("REFUND_COMPLETED");
      }

      String payload = objectMapper.writeValueAsString(event);
      outboxEventService.enqueue(
          "REFUND", event.getRefundNo(), event.getEventType(), payload, event.getEventId());
      return true;
    } catch (Exception ex) {
      log.error("Send refund completed event failed: refundNo={}", event.getRefundNo(), ex);
      return false;
    }
  }
}
