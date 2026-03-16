package com.cloud.order.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cloud.common.messaging.MessageIdempotencyService;
import com.cloud.common.messaging.event.OrderAutoReceiveEvent;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.mapper.OrderSubMapper;
import com.cloud.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

@ExtendWith(MockitoExtension.class)
class OrderAutoReceiveConsumerTest {

  @Mock private MessageIdempotencyService messageIdempotencyService;

  @Mock private OrderService orderService;

  @Mock private OrderSubMapper orderSubMapper;

  private OrderAutoReceiveConsumer consumer;

  @BeforeEach
  void setUp() {
    consumer =
        new OrderAutoReceiveConsumer(messageIdempotencyService, orderService, orderSubMapper);
  }

  @Test
  void shouldAdvanceWhenShipped() {
    OrderAutoReceiveEvent event =
        OrderAutoReceiveEvent.builder().subOrderId(11L).subOrderNo("S2026000001").build();
    Message<OrderAutoReceiveEvent> message = MessageBuilder.withPayload(event).build();
    when(messageIdempotencyService.tryAcquire(
            eq("order:auto-receive"), eq("ORDER_AUTO_RECEIVE:S2026000001")))
        .thenReturn(true);

    OrderSub subOrder = new OrderSub();
    subOrder.setId(11L);
    subOrder.setOrderStatus("SHIPPED");
    subOrder.setDeleted(0);
    when(orderSubMapper.selectById(11L)).thenReturn(subOrder);

    consumer.orderAutoReceiveConsumer().accept(message);

    verify(orderService).advanceSubOrderStatus(11L, "DONE");
    verify(messageIdempotencyService)
        .markSuccess(eq("order:auto-receive"), eq("ORDER_AUTO_RECEIVE:S2026000001"));
  }

  @Test
  void shouldSkipWhenDuplicate() {
    OrderAutoReceiveEvent event =
        OrderAutoReceiveEvent.builder().subOrderId(12L).subOrderNo("S2026000002").build();
    Message<OrderAutoReceiveEvent> message = MessageBuilder.withPayload(event).build();
    when(messageIdempotencyService.tryAcquire(any(), any())).thenReturn(false);

    consumer.orderAutoReceiveConsumer().accept(message);

    verifyNoInteractions(orderSubMapper);
    verifyNoInteractions(orderService);
  }

  @Test
  void shouldSkipWhenStatusNotShipped() {
    OrderAutoReceiveEvent event =
        OrderAutoReceiveEvent.builder().subOrderId(13L).subOrderNo("S2026000003").build();
    Message<OrderAutoReceiveEvent> message = MessageBuilder.withPayload(event).build();
    when(messageIdempotencyService.tryAcquire(
            eq("order:auto-receive"), eq("ORDER_AUTO_RECEIVE:S2026000003")))
        .thenReturn(true);

    OrderSub subOrder = new OrderSub();
    subOrder.setId(13L);
    subOrder.setOrderStatus("PAID");
    subOrder.setDeleted(0);
    when(orderSubMapper.selectById(13L)).thenReturn(subOrder);

    consumer.orderAutoReceiveConsumer().accept(message);

    verify(orderService, never()).advanceSubOrderStatus(13L, "DONE");
    verify(messageIdempotencyService)
        .markSuccess(eq("order:auto-receive"), eq("ORDER_AUTO_RECEIVE:S2026000003"));
  }
}
