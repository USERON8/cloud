package com.cloud.stock.messaging;

import com.cloud.common.messaging.consumer.AbstractJsonMqConsumer;
import com.cloud.common.messaging.event.StockReserveRequestEvent;
import com.cloud.common.metrics.TradeMetrics;
import com.cloud.stock.service.support.StockInventoryCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
    topic = "stock-reserve-request",
    consumerGroup = "stock-reserve-request-consumer-group",
    selectorExpression = "STOCK_RESERVE_REQUEST")
public class StockReserveRequestConsumer extends AbstractJsonMqConsumer<StockReserveRequestEvent> {

  private static final String NS_STOCK_RESERVE_REQUEST = "stock:reserve:request";

  private final StockInventoryCommandService stockInventoryCommandService;
  private final TradeMetrics tradeMetrics;

  @Override
  protected void doConsume(StockReserveRequestEvent event, MessageExt msgExt) {
    if (event == null || event.getOrderNo() == null || event.getOrderNo().isBlank()) {
      tradeMetrics.incrementMessageConsume("stock_reserve_request", "failed");
      return;
    }
    stockInventoryCommandService.handleReserveRequest(event);
    tradeMetrics.incrementMessageConsume("stock_reserve_request", "success");
  }

  @Override
  protected Class<StockReserveRequestEvent> payloadClass() {
    return StockReserveRequestEvent.class;
  }

  @Override
  protected String payloadDescription() {
    return "StockReserveRequestEvent";
  }

  @Override
  protected String resolveIdempotentNamespace(
      String topic, MessageExt msgExt, StockReserveRequestEvent payload) {
    return NS_STOCK_RESERVE_REQUEST;
  }

  @Override
  protected String buildIdempotentKey(
      String topic, String msgId, StockReserveRequestEvent payload, MessageExt msgExt) {
    return resolveEventId(
        "STOCK_RESERVE_REQUEST",
        payload == null ? null : payload.getEventId(),
        payload == null ? null : payload.getOrderNo());
  }
}
