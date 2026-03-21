package com.cloud.order.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.common.messaging.event.OrderTimeoutEvent;
import com.cloud.common.messaging.outbox.OutboxEvent;
import com.cloud.common.messaging.outbox.OutboxEventService;
import com.cloud.common.messaging.outbox.OutboxProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import org.apache.rocketmq.common.message.MessageConst;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;

@ExtendWith(MockitoExtension.class)
class OrderOutboxRelayTest {

  @Mock private OutboxEventService outboxEventService;
  @Mock private StreamBridge streamBridge;

  @Test
  void relayShouldSendOrderTimeoutEventsWithDelayHeader() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    OutboxProperties outboxProperties = new OutboxProperties();
    OrderOutboxRelay relay =
        new OrderOutboxRelay(
            outboxEventService, outboxProperties, streamBridge, objectMapper, null, 18);

    OrderTimeoutEvent payload =
        OrderTimeoutEvent.builder()
            .eventId("evt-timeout")
            .eventType("ORDER_TIMEOUT")
            .subOrderNo("S-200")
            .subOrderId(200L)
            .build();
    OutboxEvent event = new OutboxEvent();
    event.setEventId("evt-timeout");
    event.setEventType("ORDER_TIMEOUT");
    event.setPayload(objectMapper.writeValueAsString(payload));
    when(streamBridge.send(org.mockito.Mockito.eq("orderTimeoutProducer-out-0"), anyMessage()))
        .thenReturn(true);

    Method relayMethod = OrderOutboxRelay.class.getDeclaredMethod("relay", OutboxEvent.class);
    relayMethod.setAccessible(true);
    boolean sent = (boolean) relayMethod.invoke(relay, event);

    assertThat(sent).isTrue();
    ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
    verify(streamBridge)
        .send(org.mockito.Mockito.eq("orderTimeoutProducer-out-0"), captor.capture());
    Message<?> message = captor.getValue();
    assertThat(message.getHeaders().get(MessageConst.PROPERTY_TAGS)).isEqualTo("ORDER_TIMEOUT");
    assertThat(message.getHeaders().get(MessageConst.PROPERTY_KEYS)).isEqualTo("S-200");
    assertThat(message.getHeaders().get(MessageConst.PROPERTY_DELAY_TIME_LEVEL)).isEqualTo("18");
  }

  @SuppressWarnings("unchecked")
  private Message<Object> anyMessage() {
    return org.mockito.ArgumentMatchers.any(Message.class);
  }
}
