package com.cloud.order.messaging;

import com.cloud.common.messaging.event.OrderCreatedEvent;
import com.cloud.common.messaging.event.StockConfirmRequestEvent;
import com.cloud.common.messaging.event.StockReleaseRequestEvent;
import com.cloud.common.messaging.event.StockReserveRequestEvent;
import com.cloud.common.messaging.event.StockRestoreEvent;
import com.cloud.common.messaging.outbox.OutboxEventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMessageProducer {

  private final OutboxEventService outboxEventService;
  private final ObjectMapper objectMapper;
  private final OrderOutboxDispatcher orderOutboxDispatcher;

  public boolean sendOrderCreatedEvent(OrderCreatedEvent event) {
    try {

      if (event.getEventId() == null) {
        event.setEventId(UUID.randomUUID().toString());
      }
      if (event.getTimestamp() == null) {
        event.setTimestamp(System.currentTimeMillis());
      }
      if (event.getEventType() == null || event.getEventType().isBlank()) {
        event.setEventType("ORDER_CREATED");
      }

      String payload = objectMapper.writeValueAsString(event);
      outboxEventService.enqueue(
          "ORDER", event.getOrderNo(), event.getEventType(), payload, event.getEventId());
      orderOutboxDispatcher.dispatchAfterCommit();
      return true;

    } catch (Exception e) {
      log.error("Failed to enqueue order created event: orderNo={}", event.getOrderNo(), e);
      return false;
    }
  }

  public boolean sendOrderCancelledEvent(Long orderId, String orderNo, String reason) {
    try {
      Map<String, Object> payload = new HashMap<>();
      payload.put("orderId", orderId);
      payload.put("orderNo", orderNo);
      payload.put("reason", reason);
      payload.put("timestamp", System.currentTimeMillis());
      String eventId = UUID.randomUUID().toString();
      payload.put("eventId", eventId);
      payload.put("eventType", "ORDER_CANCELLED");
      String payloadJson = objectMapper.writeValueAsString(payload);

      outboxEventService.enqueue("ORDER", orderNo, "ORDER_CANCELLED", payloadJson, eventId);
      orderOutboxDispatcher.dispatchAfterCommit();
      return true;

    } catch (Exception e) {
      log.error(
          "Failed to enqueue order cancelled event: orderId={}, orderNo={}", orderId, orderNo, e);
      return false;
    }
  }

  public boolean sendStockRestoreEvent(StockRestoreEvent event) {
    try {
      if (event.getEventId() == null) {
        event.setEventId(UUID.randomUUID().toString());
      }
      if (event.getTimestamp() == null) {
        event.setTimestamp(System.currentTimeMillis());
      }
      if (event.getEventType() == null || event.getEventType().isBlank()) {
        event.setEventType("STOCK_RESTORE");
      }
      String payload = objectMapper.writeValueAsString(event);
      outboxEventService.enqueue(
          "REFUND", event.getRefundNo(), event.getEventType(), payload, event.getEventId());
      orderOutboxDispatcher.dispatchAfterCommit();
      return true;

    } catch (Exception e) {
      log.error(
          "Failed to enqueue stock restore event: refundNo={}",
          event == null ? null : event.getRefundNo(),
          e);
      return false;
    }
  }

  public boolean sendStockReserveRequestEvent(StockReserveRequestEvent event) {
    try {
      StockReserveRequestEvent payloadEvent =
          initEvent(event, "STOCK_RESERVE_REQUEST", StockReserveRequestEvent.class);
      String payload = objectMapper.writeValueAsString(payloadEvent);
      outboxEventService.enqueue(
          "ORDER",
          payloadEvent.getOrderNo(),
          payloadEvent.getEventType(),
          payload,
          payloadEvent.getEventId());
      orderOutboxDispatcher.dispatchAfterCommit();
      return true;

    } catch (Exception e) {
      log.error("Failed to enqueue stock reserve request: orderNo={}", event.getOrderNo(), e);
      return false;
    }
  }

  public boolean sendStockConfirmRequestEvent(StockConfirmRequestEvent event) {
    try {
      StockConfirmRequestEvent payloadEvent =
          initEvent(event, "STOCK_CONFIRM_REQUEST", StockConfirmRequestEvent.class);
      String payload = objectMapper.writeValueAsString(payloadEvent);
      outboxEventService.enqueue(
          "ORDER",
          payloadEvent.getSubOrderNo(),
          payloadEvent.getEventType(),
          payload,
          payloadEvent.getEventId());
      orderOutboxDispatcher.dispatchAfterCommit();
      return true;

    } catch (Exception e) {
      log.error("Failed to enqueue stock confirm request: subOrderNo={}", event.getSubOrderNo(), e);
      return false;
    }
  }

  public boolean sendStockReleaseRequestEvent(StockReleaseRequestEvent event) {
    try {
      StockReleaseRequestEvent payloadEvent =
          initEvent(event, "STOCK_RELEASE_REQUEST", StockReleaseRequestEvent.class);
      String payload = objectMapper.writeValueAsString(payloadEvent);
      outboxEventService.enqueue(
          "ORDER",
          payloadEvent.getSubOrderNo(),
          payloadEvent.getEventType(),
          payload,
          payloadEvent.getEventId());
      orderOutboxDispatcher.dispatchAfterCommit();
      return true;

    } catch (Exception e) {
      log.error("Failed to enqueue stock release request: subOrderNo={}", event.getSubOrderNo(), e);
      return false;
    }
  }

  private <T> T initEvent(T event, String eventType, Class<T> type) throws Exception {
    if (event == null) {
      throw new IllegalArgumentException("event is required");
    }
    Map<String, Object> payload = objectMapper.convertValue(event, Map.class);
    payload.putIfAbsent("eventId", UUID.randomUUID().toString());
    payload.putIfAbsent("eventType", eventType);
    payload.putIfAbsent("timestamp", System.currentTimeMillis());
    return objectMapper.convertValue(payload, type);
  }
}
