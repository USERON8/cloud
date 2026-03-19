package com.cloud.common.messaging.outbox;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageConst;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.StringUtils;

@Slf4j
public abstract class AbstractOutboxRelay {

  private final OutboxEventService outboxEventService;
  private final OutboxProperties outboxProperties;
  private final StreamBridge streamBridge;
  private final ObjectMapper objectMapper;
  @Nullable private final MeterRegistry meterRegistry;

  protected AbstractOutboxRelay(
      OutboxEventService outboxEventService,
      OutboxProperties outboxProperties,
      StreamBridge streamBridge,
      ObjectMapper objectMapper,
      @Nullable MeterRegistry meterRegistry) {
    this.outboxEventService = outboxEventService;
    this.outboxProperties = outboxProperties;
    this.streamBridge = streamBridge;
    this.objectMapper = objectMapper;
    this.meterRegistry = meterRegistry;
  }

  protected final void dispatchDueEvents() {
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
        sent = relay(event);
      } catch (Exception ex) {
        onDispatchFailure(event, ex);
      }

      if (sent) {
        outboxEventService.markSent(event.getId());
      } else {
        outboxEventService.markFailed(
            event, outboxProperties.getMaxRetry(), outboxProperties.getRetryBackoffSeconds());
      }
    }
  }

  protected abstract boolean relay(OutboxEvent event) throws Exception;

  protected final <T> T readPayload(OutboxEvent event, Class<T> type) throws Exception {
    return objectMapper.readValue(event.getPayload(), type);
  }

  protected final <T> T readPayload(OutboxEvent event, TypeReference<T> typeReference)
      throws Exception {
    return objectMapper.readValue(event.getPayload(), typeReference);
  }

  protected final boolean sendMessage(String bindingName, Object payload, String key, String tag) {
    return sendMessage(bindingName, payload, key, tag, null, null);
  }

  protected final boolean sendMessage(
      String bindingName,
      Object payload,
      String key,
      String tag,
      @Nullable String eventId,
      @Nullable String eventType) {
    Message<Object> message =
        MessageBuilder.withPayload(payload)
            .copyHeaders(buildHeaders(key, tag, eventId, eventType))
            .build();
    return streamBridge.send(bindingName, message);
  }

  protected final String asText(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  private Map<String, Object> buildHeaders(
      String key, String tag, @Nullable String eventId, @Nullable String eventType) {
    Map<String, Object> headers = new HashMap<>();
    putIfHasText(headers, MessageConst.PROPERTY_KEYS, key);
    putIfHasText(headers, MessageConst.PROPERTY_TAGS, tag);
    putIfHasText(headers, "eventId", eventId);
    putIfHasText(headers, "eventType", eventType);
    return headers;
  }

  private void putIfHasText(Map<String, Object> headers, String name, @Nullable String value) {
    if (StringUtils.hasText(value)) {
      headers.put(name, value);
    }
  }

  private void onDispatchFailure(OutboxEvent event, Exception ex) {
    log.error(
        "Outbox dispatch failed: eventId={}, eventType={}",
        event.getEventId(),
        event.getEventType(),
        ex);
    if (meterRegistry != null) {
      meterRegistry.counter("outbox.relay.failure", "eventType", event.getEventType()).increment();
    }
  }
}
