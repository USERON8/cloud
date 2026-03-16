package com.cloud.product.messaging;

import com.cloud.common.messaging.event.ProductSyncEvent;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageConst;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductSyncMessageProducer {

  private final StreamBridge streamBridge;

  @Value("${product.search.sync.enabled:true}")
  private boolean enabled;

  public void sendUpsert(Long spuId) {
    sendEvent("PRODUCT_UPSERT", spuId);
  }

  public void sendDelete(Long spuId) {
    sendEvent("PRODUCT_DELETE", spuId);
  }

  private void sendEvent(String eventType, Long spuId) {
    if (!enabled) {
      return;
    }
    if (spuId == null) {
      return;
    }

    ProductSyncEvent event =
        ProductSyncEvent.builder()
            .spuId(spuId)
            .eventType(eventType)
            .eventId(UUID.randomUUID().toString())
            .timestamp(System.currentTimeMillis())
            .build();

    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              dispatch(event);
            }
          });
      return;
    }
    dispatch(event);
  }

  private void dispatch(ProductSyncEvent event) {
    try {
      Message<ProductSyncEvent> message =
          MessageBuilder.withPayload(event)
              .setHeader(MessageConst.PROPERTY_KEYS, String.valueOf(event.getSpuId()))
              .setHeader(MessageConst.PROPERTY_TAGS, event.getEventType())
              .setHeader("eventId", event.getEventId())
              .setHeader("eventType", event.getEventType())
              .build();
      streamBridge.send("productSyncProducer-out-0", message);
    } catch (Exception ex) {
      log.error(
          "Send product sync event failed: spuId={}, eventType={}",
          event.getSpuId(),
          event.getEventType(),
          ex);
    }
  }
}
