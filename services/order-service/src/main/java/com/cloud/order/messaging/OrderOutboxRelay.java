package com.cloud.order.messaging;

import com.cloud.common.messaging.event.OrderCreatedEvent;
import com.cloud.common.messaging.event.StockRestoreEvent;
import com.cloud.common.messaging.outbox.OutboxEvent;
import com.cloud.common.messaging.outbox.OutboxEventService;
import com.cloud.common.messaging.outbox.OutboxProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageConst;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderOutboxRelay {

  private final OutboxEventService outboxEventService;
  private final OutboxProperties outboxProperties;
  private final StreamBridge streamBridge;
  private final ObjectMapper objectMapper;

  @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:2000}")
  public void dispatch() {
    if (!outboxProperties.isEnabled()) {
      return;
    }
    List<OutboxEvent> events = outboxEventService.fetchDueEvents(outboxProperties.getBatchSize());
    if (events.isEmpty()) {
      return;
    }

    for (OutboxEvent event : events) {
      if (!outboxEventService.markProcessing(event.getId())) {
        continue;
      }
      boolean sent = false;
      try {
        sent = sendEvent(event);
      } catch (Exception ex) {
        log.warn(
            "Outbox dispatch failed: eventId={}, eventType={}",
            event.getEventId(),
            event.getEventType(),
            ex);
      }

      if (sent) {
        outboxEventService.markSent(event.getId());
      } else {
        outboxEventService.markFailed(
            event, outboxProperties.getMaxRetry(), outboxProperties.getRetryBackoffSeconds());
      }
    }
  }

  private boolean sendEvent(OutboxEvent event) throws Exception {
    String eventType = event.getEventType();
    if (eventType == null || eventType.isBlank()) {
      log.warn("Outbox event type missing: eventId={}", event.getEventId());
      return false;
    }

    return switch (eventType) {
      case "ORDER_CREATED" -> sendOrderCreated(event);
      case "ORDER_CANCELLED" -> sendOrderCancelled(event);
      case "STOCK_RESTORE" -> sendStockRestore(event);
      default -> {
        log.warn(
            "Unknown outbox event type: eventId={}, eventType={}", event.getEventId(), eventType);
        yield false;
      }
    };
  }

  private boolean sendOrderCreated(OutboxEvent event) throws Exception {
    OrderCreatedEvent payload = objectMapper.readValue(event.getPayload(), OrderCreatedEvent.class);
    Map<String, Object> headers = new HashMap<>();
    headers.put(MessageConst.PROPERTY_KEYS, payload.getOrderNo());
    headers.put(MessageConst.PROPERTY_TAGS, "ORDER_CREATED");
    headers.put("eventId", payload.getEventId());
    headers.put("eventType", payload.getEventType());

    Message<OrderCreatedEvent> message =
        MessageBuilder.withPayload(payload).copyHeaders(headers).build();
    return streamBridge.send("orderCreatedProducer-out-0", message);
  }

  private boolean sendOrderCancelled(OutboxEvent event) throws Exception {
    Map<String, Object> payload =
        objectMapper.readValue(event.getPayload(), new TypeReference<Map<String, Object>>() {});
    Map<String, Object> headers = new HashMap<>();
    headers.put(MessageConst.PROPERTY_KEYS, asText(payload.get("orderNo")));
    headers.put(MessageConst.PROPERTY_TAGS, "ORDER_CANCELLED");

    Message<Map<String, Object>> message =
        MessageBuilder.withPayload(payload).copyHeaders(headers).build();
    return streamBridge.send("orderCancelledProducer-out-0", message);
  }

  private boolean sendStockRestore(OutboxEvent event) throws Exception {
    StockRestoreEvent payload = objectMapper.readValue(event.getPayload(), StockRestoreEvent.class);
    Map<String, Object> headers = new HashMap<>();
    headers.put(MessageConst.PROPERTY_KEYS, payload.getRefundNo());
    headers.put(MessageConst.PROPERTY_TAGS, "STOCK_RESTORE");

    Message<StockRestoreEvent> message =
        MessageBuilder.withPayload(payload).copyHeaders(headers).build();
    return streamBridge.send("stockRestoreProducer-out-0", message);
  }

  private String asText(Object value) {
    return value == null ? null : String.valueOf(value);
  }
}
