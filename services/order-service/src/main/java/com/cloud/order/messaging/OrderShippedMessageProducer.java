package com.cloud.order.messaging;

import com.cloud.common.messaging.event.OrderShippedEvent;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageConst;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderShippedMessageProducer {

  private final StreamBridge streamBridge;

  public void sendAfterCommit(OrderShippedEvent event) {
    if (event == null) {
      return;
    }
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

  private void dispatch(OrderShippedEvent event) {
    try {
      if (event.getEventId() == null || event.getEventId().isBlank()) {
        event.setEventId(UUID.randomUUID().toString());
      }
      if (event.getEventType() == null || event.getEventType().isBlank()) {
        event.setEventType("ORDER_SHIPPED");
      }
      if (event.getTimestamp() == null) {
        event.setTimestamp(System.currentTimeMillis());
      }

      Message<OrderShippedEvent> message =
          MessageBuilder.withPayload(event)
              .setHeader(MessageConst.PROPERTY_KEYS, event.getSubOrderNo())
              .setHeader(MessageConst.PROPERTY_TAGS, event.getEventType())
              .setHeader("eventId", event.getEventId())
              .setHeader("eventType", event.getEventType())
              .build();
      streamBridge.send("orderShippedProducer-out-0", message);
    } catch (Exception ex) {
      log.error("Send order shipped event failed: subOrderNo={}", event.getSubOrderNo(), ex);
    }
  }
}
