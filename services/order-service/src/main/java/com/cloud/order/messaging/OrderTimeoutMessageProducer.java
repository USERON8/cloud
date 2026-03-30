package com.cloud.order.messaging;

import com.cloud.common.messaging.event.OrderTimeoutEvent;
import com.cloud.common.messaging.outbox.OutboxEventService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderTimeoutMessageProducer {

  private final OutboxEventService outboxEventService;
  private final ObjectMapper objectMapper;
  private final OrderOutboxDispatcher orderOutboxDispatcher;

  public void sendAfterCommit(OrderTimeoutEvent event) {
    if (event == null) {
      return;
    }
    dispatch(event);
  }

  private void dispatch(OrderTimeoutEvent event) {
    if (event.getEventId() == null || event.getEventId().isBlank()) {
      event.setEventId(UUID.randomUUID().toString());
    }
    if (event.getEventType() == null || event.getEventType().isBlank()) {
      event.setEventType("ORDER_TIMEOUT");
    }
    if (event.getTimestamp() == null) {
      event.setTimestamp(System.currentTimeMillis());
    }
    try {
      String payload = objectMapper.writeValueAsString(event);
      outboxEventService.enqueue(
          "ORDER", event.getSubOrderNo(), event.getEventType(), payload, event.getEventId());
      orderOutboxDispatcher.dispatchAfterCommit();
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException(
          "Failed to serialize order timeout event for subOrderNo=" + event.getSubOrderNo(), ex);
    }
  }
}
