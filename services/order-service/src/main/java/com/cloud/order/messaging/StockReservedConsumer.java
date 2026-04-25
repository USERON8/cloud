package com.cloud.order.messaging;

import com.cloud.common.messaging.consumer.AbstractJsonMqConsumer;
import com.cloud.common.messaging.event.StockReservedEvent;
import com.cloud.common.metrics.TradeMetrics;
import com.cloud.order.entity.OrderMain;
import com.cloud.order.mapper.OrderMainMapper;
import com.cloud.order.service.support.OrderInventoryEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
    topic = "stock-reserved",
    consumerGroup = "order-stock-reserved-consumer-group",
    selectorExpression = "STOCK_RESERVED")
public class StockReservedConsumer extends AbstractJsonMqConsumer<StockReservedEvent> {

  private static final String NS_STOCK_RESERVED = "order:stock:reserved";

  private final OrderMainMapper orderMainMapper;
  private final OrderInventoryEventService orderInventoryEventService;
  private final TradeMetrics tradeMetrics;

  @Override
  protected void doConsume(StockReservedEvent event, MessageExt msgExt) {
    if (event == null || event.getOrderNo() == null || event.getOrderNo().isBlank()) {
      tradeMetrics.incrementMessageConsume("stock_reserved", "failed");
      return;
    }
    OrderMain mainOrder = orderMainMapper.selectActiveByOrderNo(event.getOrderNo());
    if (mainOrder == null) {
      tradeMetrics.incrementMessageConsume("stock_reserved", "failed");
      return;
    }
    orderInventoryEventService.handleStockReserved(event.getOrderNo());
    tradeMetrics.incrementMessageConsume("stock_reserved", "success");
  }

  @Override
  protected Class<StockReservedEvent> payloadClass() {
    return StockReservedEvent.class;
  }

  @Override
  protected String payloadDescription() {
    return "StockReservedEvent";
  }

  @Override
  protected String resolveIdempotentNamespace(
      String topic, MessageExt msgExt, StockReservedEvent payload) {
    return NS_STOCK_RESERVED;
  }

  @Override
  protected String buildIdempotentKey(
      String topic, String msgId, StockReservedEvent payload, MessageExt msgExt) {
    return resolveEventId(
        "STOCK_RESERVED",
        payload == null ? null : payload.getEventId(),
        payload == null ? null : payload.getOrderNo());
  }
}
