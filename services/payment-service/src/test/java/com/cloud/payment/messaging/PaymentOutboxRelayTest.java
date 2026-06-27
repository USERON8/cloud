package com.cloud.payment.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.common.messaging.event.PaymentSuccessEvent;
import com.cloud.common.messaging.event.RefundCompletedEvent;
import com.cloud.common.messaging.outbox.OutboxEvent;
import com.cloud.common.messaging.outbox.OutboxEventService;
import com.cloud.common.messaging.outbox.OutboxProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.common.message.MessageConst;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;

@ExtendWith(MockitoExtension.class)
class PaymentOutboxRelayTest {

  @Mock private OutboxEventService outboxEventService;
  @Mock private StreamBridge streamBridge;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void relaySendsPaymentSuccessToPaymentBinding() throws Exception {
    PaymentOutboxRelay relay = newRelay();
    PaymentSuccessEvent payload =
        PaymentSuccessEvent.builder()
            .orderNo("MO-1001")
            .eventId("event-1001")
            .eventType("PAYMENT_SUCCESS")
            .build();
    OutboxEvent event = outboxEvent("PAYMENT_SUCCESS", payload);
    when(streamBridge.send(eq("paymentSuccessProducer-out-0"), any(Message.class)))
        .thenReturn(true);

    boolean result = relay.relay(event);

    assertThat(result).isTrue();
    Message<?> message = captureMessage("paymentSuccessProducer-out-0");
    assertThat(message.getPayload()).isInstanceOf(PaymentSuccessEvent.class);
    assertThat(message.getHeaders().get(MessageConst.PROPERTY_KEYS)).isEqualTo("MO-1001");
    assertThat(message.getHeaders().get(MessageConst.PROPERTY_TAGS)).isEqualTo("PAYMENT_SUCCESS");
    assertThat(message.getHeaders().get("eventId")).isEqualTo("event-1001");
    assertThat(message.getHeaders().get("eventType")).isEqualTo("PAYMENT_SUCCESS");
  }

  @Test
  void relaySendsRefundCompletedToRefundBinding() throws Exception {
    PaymentOutboxRelay relay = newRelay();
    RefundCompletedEvent payload =
        RefundCompletedEvent.builder()
            .refundNo("RF-1001")
            .eventId("event-2001")
            .eventType("REFUND_COMPLETED")
            .build();
    OutboxEvent event = outboxEvent("REFUND_COMPLETED", payload);
    when(streamBridge.send(eq("refundCompletedProducer-out-0"), any(Message.class)))
        .thenReturn(true);

    boolean result = relay.relay(event);

    assertThat(result).isTrue();
    Message<?> message = captureMessage("refundCompletedProducer-out-0");
    assertThat(message.getPayload()).isInstanceOf(RefundCompletedEvent.class);
    assertThat(message.getHeaders().get(MessageConst.PROPERTY_KEYS)).isEqualTo("RF-1001");
    assertThat(message.getHeaders().get(MessageConst.PROPERTY_TAGS)).isEqualTo("REFUND_COMPLETED");
    assertThat(message.getHeaders().get("eventId")).isEqualTo("event-2001");
    assertThat(message.getHeaders().get("eventType")).isEqualTo("REFUND_COMPLETED");
  }

  @Test
  void relayRejectsUnknownEventTypeWithoutSendingMessage() throws Exception {
    PaymentOutboxRelay relay = newRelay();
    OutboxEvent event = new OutboxEvent();
    event.setEventId("event-3001");
    event.setEventType("UNKNOWN_EVENT");
    event.setPayload("{}");

    boolean result = relay.relay(event);

    assertThat(result).isFalse();
    verify(streamBridge, never()).send(any(String.class), any(Message.class));
  }

  private PaymentOutboxRelay newRelay() {
    return new PaymentOutboxRelay(
        outboxEventService, new OutboxProperties(), streamBridge, objectMapper, null);
  }

  private OutboxEvent outboxEvent(String eventType, Object payload) throws Exception {
    OutboxEvent event = new OutboxEvent();
    event.setEventId("outbox-" + eventType);
    event.setEventType(eventType);
    event.setPayload(objectMapper.writeValueAsString(payload));
    return event;
  }

  private Message<?> captureMessage(String bindingName) {
    ArgumentCaptor<Message<?>> messageCaptor = ArgumentCaptor.forClass(Message.class);
    verify(streamBridge).send(eq(bindingName), messageCaptor.capture());
    return messageCaptor.getValue();
  }
}
