package com.cloud.order.messaging;

import com.cloud.common.messaging.consumer.AbstractJsonMqConsumer;
import com.cloud.common.messaging.event.OrderAutoReceiveEvent;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.enums.OrderAction;
import com.cloud.order.mapper.OrderSubMapper;
import com.cloud.order.service.OrderService;
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
public class OrderAutoReceiveConsumer extends AbstractJsonMqConsumer<OrderAutoReceiveEvent> {

  private static final String NS_ORDER_AUTO_RECEIVE = "order:auto-receive";
  private static final Set<String> ALLOW_STATUSES = Set.of("SHIPPED");

  private final OrderService orderService;
  private final OrderSubMapper orderSubMapper;

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
  protected Class<OrderAutoReceiveEvent> payloadClass() {
    return OrderAutoReceiveEvent.class;
  }

  @Override
  protected String payloadDescription() {
    return "OrderAutoReceiveEvent";
  }

  @Override
  protected String resolveIdempotentNamespace(
      String topic, MessageExt msgExt, OrderAutoReceiveEvent payload) {
    return NS_ORDER_AUTO_RECEIVE;
  }

  @Override
  protected String buildIdempotentKey(
      String topic, String msgId, OrderAutoReceiveEvent payload, MessageExt msgExt) {
    return resolveEventId(
        "ORDER_AUTO_RECEIVE",
        payload == null ? null : payload.getEventId(),
        payload == null ? null : payload.getSubOrderNo(),
        payload == null ? null : payload.getSubOrderId());
  }
}
