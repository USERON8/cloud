package com.cloud.order.messaging;

import com.cloud.common.messaging.consumer.AbstractMqConsumer;
import com.cloud.common.messaging.event.OrderAutoReceiveEvent;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.enums.OrderAction;
import com.cloud.order.mapper.OrderSubMapper;
import com.cloud.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
    topic = "order-auto-receive",
    consumerGroup = "order-auto-receive-consumer-group",
    selectorExpression = "ORDER_AUTO_RECEIVE")
public class OrderAutoReceiveConsumer extends AbstractMqConsumer<OrderAutoReceiveEvent> {

  private static final String NS_ORDER_AUTO_RECEIVE = "order:auto-receive";
  private static final Set<String> ALLOW_STATUSES = Set.of("SHIPPED");

  private final OrderService orderService;
  private final OrderSubMapper orderSubMapper;
  private final ObjectMapper objectMapper;

  @Override
  protected void doConsume(OrderAutoReceiveEvent event, MessageExt msgExt) {
    if (event == null || event.getSubOrderId() == null) {
      return;
    }

    OrderSub subOrder = orderSubMapper.selectById(event.getSubOrderId());
    if (subOrder == null || Integer.valueOf(1).equals(subOrder.getDeleted())) {
      return;
    }

    if (!ALLOW_STATUSES.contains(subOrder.getOrderStatus())) {
      return;
    }

    orderService.advanceSubOrderStatus(subOrder.getId(), OrderAction.DONE);
  }

  @Override
  protected OrderAutoReceiveEvent deserialize(byte[] body) {
    try {
      return body == null ? null : objectMapper.readValue(body, OrderAutoReceiveEvent.class);
    } catch (Exception ex) {
      throw new IllegalArgumentException("Failed to deserialize OrderAutoReceiveEvent", ex);
    }
  }

  @Override
  protected String resolveIdempotentNamespace(
      String topic, MessageExt msgExt, OrderAutoReceiveEvent payload) {
    return NS_ORDER_AUTO_RECEIVE;
  }

  @Override
  protected String buildIdempotentKey(
      String topic, String msgId, OrderAutoReceiveEvent payload, MessageExt msgExt) {
    return resolveEventId(payload);
  }

  private String resolveEventId(OrderAutoReceiveEvent event) {
    if (event != null && event.getEventId() != null && !event.getEventId().isBlank()) {
      return event.getEventId();
    }
    if (event != null && event.getSubOrderNo() != null && !event.getSubOrderNo().isBlank()) {
      return "ORDER_AUTO_RECEIVE:" + event.getSubOrderNo();
    }
    if (event != null && event.getSubOrderId() != null) {
      return "ORDER_AUTO_RECEIVE:" + event.getSubOrderId();
    }
    return "ORDER_AUTO_RECEIVE:" + System.currentTimeMillis();
  }
}
