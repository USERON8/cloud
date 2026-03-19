package com.cloud.common.messaging.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.stream.function.StreamBridge;

class AbstractOutboxRelayTest {

  private final OutboxEventService outboxEventService = mock(OutboxEventService.class);
  private final StreamBridge streamBridge = mock(StreamBridge.class);
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void dispatchSkipsFetchWhenOutboxIsDisabled() {
    OutboxProperties properties = new OutboxProperties();
    properties.setEnabled(false);
    TestRelay relay = new TestRelay(properties, null, event -> true);

    relay.dispatch();

    verify(outboxEventService, never()).fetchDueEvents(anyInt());
  }

  @Test
  void dispatchMarksEventSentWhenRelaySucceeds() throws Exception {
    OutboxProperties properties = new OutboxProperties();
    OutboxEvent event = newEvent(1L, "ORDER_CREATED");
    when(outboxEventService.fetchDueEvents(properties.getBatchSize())).thenReturn(List.of(event));
    when(outboxEventService.markProcessing(event.getId())).thenReturn(true);
    TestRelay relay = new TestRelay(properties, null, candidate -> true);

    relay.dispatch();

    verify(outboxEventService).markSent(event.getId());
    verify(outboxEventService, never())
        .markFailed(event, properties.getMaxRetry(), properties.getRetryBackoffSeconds());
  }

  @Test
  void dispatchMarksEventFailedAndRecordsMetricWhenRelayThrows() throws Exception {
    OutboxProperties properties = new OutboxProperties();
    OutboxEvent event = newEvent(2L, "STOCK_ALERT");
    when(outboxEventService.fetchDueEvents(properties.getBatchSize())).thenReturn(List.of(event));
    when(outboxEventService.markProcessing(event.getId())).thenReturn(true);
    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    TestRelay relay =
        new TestRelay(
            properties,
            meterRegistry,
            candidate -> {
              throw new IllegalStateException("boom");
            });

    relay.dispatch();

    verify(outboxEventService)
        .markFailed(event, properties.getMaxRetry(), properties.getRetryBackoffSeconds());
    verify(outboxEventService, never()).markSent(event.getId());
    assertThat(
            meterRegistry
                .get("outbox.relay.failure")
                .tag("eventType", event.getEventType())
                .counter()
                .count())
        .isEqualTo(1.0d);
  }

  private OutboxEvent newEvent(Long id, String eventType) {
    OutboxEvent event = new OutboxEvent();
    event.setId(id);
    event.setEventId("evt-" + id);
    event.setEventType(eventType);
    event.setPayload("{}");
    event.setRetryCount(0);
    return event;
  }

  private final class TestRelay extends AbstractOutboxRelay {

    private final RelayBehavior relayBehavior;

    private TestRelay(
        OutboxProperties properties,
        SimpleMeterRegistry meterRegistry,
        RelayBehavior relayBehavior) {
      super(outboxEventService, properties, streamBridge, objectMapper, meterRegistry);
      this.relayBehavior = relayBehavior;
    }

    private void dispatch() {
      dispatchDueEvents();
    }

    @Override
    protected boolean relay(OutboxEvent event) throws Exception {
      return relayBehavior.relay(event);
    }
  }

  @FunctionalInterface
  private interface RelayBehavior {
    boolean relay(OutboxEvent event) throws Exception;
  }
}
