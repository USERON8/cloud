package com.cloud.order.messaging;

import com.cloud.common.messaging.event.OrderAutoReceiveEvent;
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
public class OrderAutoReceiveMessageProducer {

  private final StreamBridge streamBridge;

  @Value("${order.auto-receive.delay-level:${order.timeout.delay-level:16}}")
  private int delayLevel;

  public void sendAfterCommit(OrderAutoReceiveEvent event) {
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

  private void dispatch(OrderAutoReceiveEvent event) {
    try {
      if (event.getEventId() == null || event.getEventId().isBlank()) {
        event.setEventId(UUID.randomUUID().toString());
      }
      if (event.getEventType() == null || event.getEventType().isBlank()) {
        event.setEventType("ORDER_AUTO_RECEIVE");
      }
      if (event.getTimestamp() == null) {
        event.setTimestamp(System.currentTimeMillis());
      }

      Message<OrderAutoReceiveEvent> message =
          MessageBuilder.withPayload(event)
              .setHeader(MessageConst.PROPERTY_KEYS, event.getSubOrderNo())
              .setHeader(MessageConst.PROPERTY_TAGS, event.getEventType())
              .setHeader(
                  MessageConst.PROPERTY_DELAY_TIME_LEVEL, String.valueOf(Math.max(1, delayLevel)))
              .setHeader("eventId", event.getEventId())
              .setHeader("eventType", event.getEventType())
              .build();
      streamBridge.send("orderAutoReceiveProducer-out-0", message);
    } catch (Exception ex) {
      log.error("Send order auto receive event failed: subOrderNo={}", event.getSubOrderNo(), ex);
    }
  }
}
