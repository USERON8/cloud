package com.cloud.stock.messaging;

import com.cloud.common.messaging.event.ProductSyncEvent;
import com.cloud.common.messaging.event.StockAlertEvent;
import com.cloud.common.messaging.event.StockFreezeFailedEvent;
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
public class StockOutboxRelay extends AbstractOutboxRelay {

  public StockOutboxRelay(
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
      case "STOCK_FREEZE_FAILED" -> sendStockFreezeFailed(event);
      case "STOCK_ALERT" -> sendStockAlert(event);
      case "PRODUCT_UPSERT" -> sendProductSync(event);
      default -> {
        log.warn(
            "Unknown outbox event type: eventId={}, eventType={}", event.getEventId(), eventType);
        yield false;
      }
    };
  }

  private boolean sendStockFreezeFailed(OutboxEvent event) throws Exception {
    StockFreezeFailedEvent payload = readPayload(event, StockFreezeFailedEvent.class);
    return sendMessage(
        "stockFreezeFailedProducer-out-0",
        payload,
        payload.getOrderNo(),
        "STOCK_FREEZE_FAILED",
        payload.getEventId(),
        payload.getEventType());
  }

  private boolean sendStockAlert(OutboxEvent event) throws Exception {
    StockAlertEvent payload = readPayload(event, StockAlertEvent.class);
    return sendMessage(
        "stockAlertProducer-out-0",
        payload,
        payload.getEventId(),
        payload.getEventType(),
        payload.getEventId(),
        payload.getEventType());
  }

  private boolean sendProductSync(OutboxEvent event) throws Exception {
    ProductSyncEvent payload = readPayload(event, ProductSyncEvent.class);
    return sendMessage(
        "productSyncProducer-out-0",
        payload,
        String.valueOf(payload.getSpuId()),
        "PRODUCT_UPSERT",
        payload.getEventId(),
        payload.getEventType());
  }
}
