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
import com.cloud.order.enums.OrderAction;
import com.cloud.order.mapper.OrderSubMapper;
import com.cloud.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.common.message.MessageExt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrderAutoReceiveConsumerTest {

  @Mock private MessageIdempotencyService messageIdempotencyService;

  @Mock private OrderService orderService;

  @Mock private OrderSubMapper orderSubMapper;

  private OrderAutoReceiveConsumer consumer;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    consumer = new OrderAutoReceiveConsumer(orderService, orderSubMapper);
    ReflectionTestUtils.setField(consumer, "messageIdempotencyService", messageIdempotencyService);
  }

  @Test
  void shouldAdvanceWhenShipped() {
    OrderAutoReceiveEvent event =
        OrderAutoReceiveEvent.builder().subOrderId(11L).subOrderNo("S2026000001").build();
    MessageExt message = buildMessage(event, "msg-1");
    when(messageIdempotencyService.tryAcquire(
            eq("order:auto-receive"), eq("ORDER_AUTO_RECEIVE:S2026000001")))
        .thenReturn(true);

    OrderSub subOrder = new OrderSub();
    subOrder.setId(11L);
    subOrder.setOrderStatus("SHIPPED");
    subOrder.setDeleted(0);
    when(orderSubMapper.selectById(11L)).thenReturn(subOrder);

    consumer.onMessage(message);

    verify(orderService).advanceSubOrderStatus(11L, OrderAction.DONE);
    verify(messageIdempotencyService)
        .markSuccess(eq("order:auto-receive"), eq("ORDER_AUTO_RECEIVE:S2026000001"));
  }

  @Test
  void shouldSkipWhenDuplicate() {
    OrderAutoReceiveEvent event =
        OrderAutoReceiveEvent.builder().subOrderId(12L).subOrderNo("S2026000002").build();
    MessageExt message = buildMessage(event, "msg-2");
    when(messageIdempotencyService.tryAcquire(any(), any())).thenReturn(false);

    consumer.onMessage(message);

    verifyNoInteractions(orderSubMapper);
    verifyNoInteractions(orderService);
  }

  @Test
  void shouldSkipWhenStatusNotShipped() {
    OrderAutoReceiveEvent event =
        OrderAutoReceiveEvent.builder().subOrderId(13L).subOrderNo("S2026000003").build();
    MessageExt message = buildMessage(event, "msg-3");
    when(messageIdempotencyService.tryAcquire(
            eq("order:auto-receive"), eq("ORDER_AUTO_RECEIVE:S2026000003")))
        .thenReturn(true);

    OrderSub subOrder = new OrderSub();
    subOrder.setId(13L);
    subOrder.setOrderStatus("PAID");
    subOrder.setDeleted(0);
    when(orderSubMapper.selectById(13L)).thenReturn(subOrder);

    consumer.onMessage(message);

    verify(orderService, never()).advanceSubOrderStatus(13L, OrderAction.DONE);
    verify(messageIdempotencyService)
        .markSuccess(eq("order:auto-receive"), eq("ORDER_AUTO_RECEIVE:S2026000003"));
  }

  private MessageExt buildMessage(OrderAutoReceiveEvent event, String msgId) {
    MessageExt message = new MessageExt();
    message.setTopic("order-auto-receive");
    message.setMsgId(msgId);
    try {
      message.setBody(objectMapper.writeValueAsBytes(event));
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
    return message;
  }
}
