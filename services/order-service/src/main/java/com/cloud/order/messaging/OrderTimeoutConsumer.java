package com.cloud.order.messaging;

import com.cloud.common.messaging.consumer.AbstractJsonMqConsumer;
import com.cloud.common.messaging.event.OrderTimeoutEvent;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.mapper.OrderSubMapper;
import com.cloud.order.service.OrderTimeoutService;
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
    topic = "order-timeout",
    consumerGroup = "order-timeout-consumer-group",
    selectorExpression = "ORDER_TIMEOUT")
public class OrderTimeoutConsumer extends AbstractJsonMqConsumer<OrderTimeoutEvent> {

  private static final String NS_ORDER_TIMEOUT = "order:timeout:cancel";
  private static final Set<String> CANCELLABLE_STATUSES = Set.of("CREATED", "STOCK_RESERVED");

  private final OrderTimeoutService orderTimeoutService;
  private final OrderSubMapper orderSubMapper;

  @Override
  protected void doConsume(OrderTimeoutEvent event, MessageExt msgExt) {
    if (event == null || event.getSubOrderId() == null) {
      return;
    }

    OrderSub subOrder = orderSubMapper.selectById(event.getSubOrderId());
    if (subOrder == null || Integer.valueOf(1).equals(subOrder.getDeleted())) {
      return;
    }

    if (!CANCELLABLE_STATUSES.contains(subOrder.getOrderStatus())) {
      return;
    }

    orderTimeoutService.cancelTimeoutOrder(subOrder.getId());
  }

  @Override
  protected Class<OrderTimeoutEvent> payloadClass() {
    return OrderTimeoutEvent.class;
  }

  @Override
  protected String payloadDescription() {
    return "OrderTimeoutEvent";
  }

  @Override
  protected String resolveIdempotentNamespace(
      String topic, MessageExt msgExt, OrderTimeoutEvent payload) {
    return NS_ORDER_TIMEOUT;
  }

  @Override
  protected String buildIdempotentKey(
      String topic, String msgId, OrderTimeoutEvent payload, MessageExt msgExt) {
    return resolveEventId(
        "ORDER_TIMEOUT",
        payload == null ? null : payload.getEventId(),
        payload == null ? null : payload.getSubOrderNo(),
        payload == null ? null : payload.getSubOrderId());
  }
}
