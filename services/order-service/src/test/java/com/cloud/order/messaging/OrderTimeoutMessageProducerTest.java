package com.cloud.order.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.common.messaging.event.OrderTimeoutEvent;
import com.cloud.common.messaging.outbox.OutboxEventService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderTimeoutMessageProducerTest {

  @Mock private OutboxEventService outboxEventService;
  @Mock private ObjectMapper objectMapper;

  @Test
  void sendAfterCommitShouldEnqueueOrderTimeoutEvent() throws Exception {
    OrderTimeoutMessageProducer producer =
        new OrderTimeoutMessageProducer(outboxEventService, objectMapper);
    OrderTimeoutEvent event = OrderTimeoutEvent.builder().subOrderNo("S-1").build();
    when(objectMapper.writeValueAsString(event)).thenReturn("{\"subOrderNo\":\"S-1\"}");

    producer.sendAfterCommit(event);

    verify(outboxEventService)
        .enqueue(
            eq("ORDER"), eq("S-1"), eq("ORDER_TIMEOUT"), eq("{\"subOrderNo\":\"S-1\"}"), any());
    assertThat(event.getEventId()).isNotBlank();
    assertThat(event.getEventType()).isEqualTo("ORDER_TIMEOUT");
    assertThat(event.getTimestamp()).isNotNull();
  }

  @Test
  void sendAfterCommitShouldIgnoreNullEvent() {
    OrderTimeoutMessageProducer producer =
        new OrderTimeoutMessageProducer(outboxEventService, objectMapper);

    producer.sendAfterCommit(null);

    verify(outboxEventService, never()).enqueue(any(), any(), any(), any(), any());
  }

  @Test
  void sendAfterCommitShouldPropagateSerializationFailure() throws Exception {
    OrderTimeoutMessageProducer producer =
        new OrderTimeoutMessageProducer(outboxEventService, objectMapper);
    OrderTimeoutEvent event = OrderTimeoutEvent.builder().subOrderNo("S-2").build();
    when(objectMapper.writeValueAsString(event))
        .thenThrow(new JsonProcessingException("bad json") {});

    assertThatThrownBy(() -> producer.sendAfterCommit(event))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("S-2");
  }
}
