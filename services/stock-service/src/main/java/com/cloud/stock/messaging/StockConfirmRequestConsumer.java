package com.cloud.stock.messaging;

import com.cloud.common.messaging.consumer.AbstractJsonMqConsumer;
import com.cloud.common.messaging.event.StockConfirmRequestEvent;
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
    topic = "stock-confirm-request",
    consumerGroup = "stock-confirm-request-consumer-group",
    selectorExpression = "STOCK_CONFIRM_REQUEST")
public class StockConfirmRequestConsumer extends AbstractJsonMqConsumer<StockConfirmRequestEvent> {

  private static final String NS_STOCK_CONFIRM_REQUEST = "stock:confirm:request";

  private final StockInventoryCommandService stockInventoryCommandService;
  private final TradeMetrics tradeMetrics;

  @Override
  protected void doConsume(StockConfirmRequestEvent event, MessageExt msgExt) {
    if (event == null || event.getSubOrderNo() == null || event.getSubOrderNo().isBlank()) {
      tradeMetrics.incrementMessageConsume("stock_confirm_request", "failed");
      return;
    }
    stockInventoryCommandService.handleConfirmRequest(event);
    tradeMetrics.incrementMessageConsume("stock_confirm_request", "success");
  }

  @Override
  protected Class<StockConfirmRequestEvent> payloadClass() {
    return StockConfirmRequestEvent.class;
  }

  @Override
  protected String payloadDescription() {
    return "StockConfirmRequestEvent";
  }

  @Override
  protected String resolveIdempotentNamespace(
      String topic, MessageExt msgExt, StockConfirmRequestEvent payload) {
    return NS_STOCK_CONFIRM_REQUEST;
  }

  @Override
  protected String buildIdempotentKey(
      String topic, String msgId, StockConfirmRequestEvent payload, MessageExt msgExt) {
    return resolveEventId(
        "STOCK_CONFIRM_REQUEST",
        payload == null ? null : payload.getEventId(),
        payload == null ? null : payload.getSubOrderNo());
  }
}
