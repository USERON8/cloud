package com.cloud.order.messaging;

import com.cloud.common.messaging.event.OrderCreatedEvent;
import com.cloud.common.messaging.event.OrderTimeoutEvent;
import com.cloud.common.messaging.event.StockConfirmRequestEvent;
import com.cloud.common.messaging.event.StockReleaseRequestEvent;
import com.cloud.common.messaging.event.StockReserveRequestEvent;
import com.cloud.common.messaging.event.StockRestoreEvent;
import com.cloud.common.messaging.outbox.AbstractOutboxRelay;
import com.cloud.common.messaging.outbox.OutboxEvent;
import com.cloud.common.messaging.outbox.OutboxEventService;
import com.cloud.common.messaging.outbox.OutboxProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageConst;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderOutboxRelay extends AbstractOutboxRelay {

  private final int orderTimeoutDelayLevel;

  public OrderOutboxRelay(
      OutboxEventService outboxEventService,
      OutboxProperties outboxProperties,
      StreamBridge streamBridge,
      ObjectMapper objectMapper,
      @Nullable MeterRegistry meterRegistry,
      @Value("${order.timeout.delay-level:16}") int orderTimeoutDelayLevel) {
    super(outboxEventService, outboxProperties, streamBridge, objectMapper, meterRegistry);
    this.orderTimeoutDelayLevel = Math.max(1, orderTimeoutDelayLevel);
  }

  @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:2000}")
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
      case "ORDER_CREATED" -> sendOrderCreated(event);
      case "ORDER_CANCELLED" -> sendOrderCancelled(event);
      case "ORDER_TIMEOUT" -> sendOrderTimeout(event);
      case "STOCK_RESERVE_REQUEST" -> sendStockReserveRequest(event);
      case "STOCK_CONFIRM_REQUEST" -> sendStockConfirmRequest(event);
      case "STOCK_RELEASE_REQUEST" -> sendStockReleaseRequest(event);
      case "STOCK_RESTORE" -> sendStockRestore(event);
      default -> {
        log.warn(
            "Unknown outbox event type: eventId={}, eventType={}", event.getEventId(), eventType);
        yield false;
      }
    };
  }

  private boolean sendOrderCreated(OutboxEvent event) throws Exception {
    OrderCreatedEvent payload = readPayload(event, OrderCreatedEvent.class);
    return sendMessage(
        "orderCreatedProducer-out-0",
        payload,
        payload.getOrderNo(),
        "ORDER_CREATED",
        payload.getEventId(),
        payload.getEventType());
  }

  private boolean sendOrderCancelled(OutboxEvent event) throws Exception {
    Map<String, Object> payload = readPayload(event, new TypeReference<Map<String, Object>>() {});
    return sendMessage(
        "orderCancelledProducer-out-0", payload, asText(payload.get("orderNo")), "ORDER_CANCELLED");
  }

  private boolean sendOrderTimeout(OutboxEvent event) throws Exception {
    OrderTimeoutEvent payload = readPayload(event, OrderTimeoutEvent.class);
    return sendMessage(
        "orderTimeoutProducer-out-0",
        payload,
        payload.getSubOrderNo(),
        "ORDER_TIMEOUT",
        payload.getEventId(),
        payload.getEventType(),
        Map.of(MessageConst.PROPERTY_DELAY_TIME_LEVEL, String.valueOf(orderTimeoutDelayLevel)));
  }

  private boolean sendStockRestore(OutboxEvent event) throws Exception {
    StockRestoreEvent payload = readPayload(event, StockRestoreEvent.class);
    return sendMessage(
        "stockRestoreProducer-out-0", payload, payload.getRefundNo(), "STOCK_RESTORE");
  }

  private boolean sendStockReserveRequest(OutboxEvent event) throws Exception {
    StockReserveRequestEvent payload = readPayload(event, StockReserveRequestEvent.class);
    return sendMessage(
        "stockReserveRequestProducer-out-0",
        payload,
        payload.getOrderNo(),
        "STOCK_RESERVE_REQUEST",
        payload.getEventId(),
        payload.getEventType());
  }

  private boolean sendStockConfirmRequest(OutboxEvent event) throws Exception {
    StockConfirmRequestEvent payload = readPayload(event, StockConfirmRequestEvent.class);
    return sendMessage(
        "stockConfirmRequestProducer-out-0",
        payload,
        payload.getSubOrderNo(),
        "STOCK_CONFIRM_REQUEST",
        payload.getEventId(),
        payload.getEventType());
  }

  private boolean sendStockReleaseRequest(OutboxEvent event) throws Exception {
    StockReleaseRequestEvent payload = readPayload(event, StockReleaseRequestEvent.class);
    return sendMessage(
        "stockReleaseRequestProducer-out-0",
        payload,
        payload.getSubOrderNo(),
        "STOCK_RELEASE_REQUEST",
        payload.getEventId(),
        payload.getEventType());
  }
}
