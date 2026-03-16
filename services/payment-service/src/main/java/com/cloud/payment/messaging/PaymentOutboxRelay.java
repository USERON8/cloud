package com.cloud.payment.messaging;

import com.cloud.common.messaging.event.RefundCompletedEvent;
import com.cloud.common.messaging.outbox.OutboxEvent;
import com.cloud.common.messaging.outbox.OutboxEventService;
import com.cloud.common.messaging.outbox.OutboxProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageConst;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentOutboxRelay {

  private final OutboxEventService outboxEventService;
  private final OutboxProperties outboxProperties;
  private final StreamBridge streamBridge;
  private final ObjectMapper objectMapper;

  @Autowired(required = false)
  private MeterRegistry meterRegistry;

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
        log.error(
            "Outbox dispatch failed: eventId={}, eventType={}",
            event.getEventId(),
            event.getEventType(),
            ex);
        if (meterRegistry != null) {
          meterRegistry.counter("outbox.relay.failure", "eventType", event.getEventType()).increment();
        }
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
      case "REFUND_COMPLETED" -> sendRefundCompleted(event);
      default -> {
        log.warn(
            "Unknown outbox event type: eventId={}, eventType={}", event.getEventId(), eventType);
        yield false;
      }
    };
  }

  private boolean sendRefundCompleted(OutboxEvent event) throws Exception {
    RefundCompletedEvent payload =
        objectMapper.readValue(event.getPayload(), RefundCompletedEvent.class);
    Map<String, Object> headers = new HashMap<>();
    headers.put(MessageConst.PROPERTY_KEYS, payload.getRefundNo());
    headers.put(MessageConst.PROPERTY_TAGS, "REFUND_COMPLETED");
    headers.put("eventId", payload.getEventId());
    headers.put("eventType", payload.getEventType());

    Message<RefundCompletedEvent> message =
        MessageBuilder.withPayload(payload).copyHeaders(headers).build();
    return streamBridge.send("refundCompletedProducer-out-0", message);
  }
}
