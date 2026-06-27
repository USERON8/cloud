package com.cloud.payment.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.cloud.common.messaging.event.PaymentSuccessEvent;
import com.cloud.common.messaging.event.RefundCompletedEvent;
import com.cloud.common.messaging.outbox.OutboxEventService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentMessageProducerTest {

  @Mock private OutboxEventService outboxEventService;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void sendPaymentSuccessEventEnqueuesPaymentOutboxEvent() throws Exception {
    PaymentMessageProducer producer = new PaymentMessageProducer(outboxEventService, objectMapper);
    PaymentSuccessEvent event =
        PaymentSuccessEvent.builder()
            .orderNo("MO-1001")
            .amount(new BigDecimal("19.90"))
            .eventId("event-1001")
            .build();

    boolean result = producer.sendPaymentSuccessEvent(event);

    assertThat(result).isTrue();
    ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
    verify(outboxEventService)
        .enqueue(
            eq("PAYMENT"),
            eq("MO-1001"),
            eq("PAYMENT_SUCCESS"),
            payloadCaptor.capture(),
            eq("event-1001"));
    JsonNode payload = objectMapper.readTree(payloadCaptor.getValue());
    assertThat(payload.get("orderNo").asText()).isEqualTo("MO-1001");
    assertThat(payload.get("eventType").asText()).isEqualTo("PAYMENT_SUCCESS");
    assertThat(payload.get("timestamp").asLong()).isPositive();
  }

  @Test
  void sendRefundCompletedEventEnqueuesRefundOutboxEvent() throws Exception {
    PaymentMessageProducer producer = new PaymentMessageProducer(outboxEventService, objectMapper);
    RefundCompletedEvent event =
        RefundCompletedEvent.builder()
            .refundNo("RF-1001")
            .paymentNo("PAY-1001")
            .eventId("event-2001")
            .build();

    boolean result = producer.sendRefundCompletedEvent(event);

    assertThat(result).isTrue();
    ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
    verify(outboxEventService)
        .enqueue(
            eq("REFUND"),
            eq("RF-1001"),
            eq("REFUND_COMPLETED"),
            payloadCaptor.capture(),
            eq("event-2001"));
    JsonNode payload = objectMapper.readTree(payloadCaptor.getValue());
    assertThat(payload.get("refundNo").asText()).isEqualTo("RF-1001");
    assertThat(payload.get("eventType").asText()).isEqualTo("REFUND_COMPLETED");
    assertThat(payload.get("timestamp").asLong()).isPositive();
  }

  @Test
  void sendNullEventDoesNotEnqueueOutboxEvent() {
    PaymentMessageProducer producer = new PaymentMessageProducer(outboxEventService, objectMapper);

    assertThat(producer.sendPaymentSuccessEvent(null)).isFalse();
    assertThat(producer.sendRefundCompletedEvent(null)).isFalse();

    verify(outboxEventService, never())
        .enqueue(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any());
  }
}
