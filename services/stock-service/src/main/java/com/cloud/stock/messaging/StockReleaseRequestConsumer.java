package com.cloud.stock.messaging;

import com.cloud.common.messaging.consumer.AbstractJsonMqConsumer;
import com.cloud.common.messaging.event.StockReleaseRequestEvent;
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
    topic = "stock-release-request",
    consumerGroup = "stock-release-request-consumer-group",
    selectorExpression = "STOCK_RELEASE_REQUEST")
public class StockReleaseRequestConsumer extends AbstractJsonMqConsumer<StockReleaseRequestEvent> {

  private static final String NS_STOCK_RELEASE_REQUEST = "stock:release:request";

  private final StockInventoryCommandService stockInventoryCommandService;
  private final TradeMetrics tradeMetrics;

  @Override
  protected void doConsume(StockReleaseRequestEvent event, MessageExt msgExt) {
    if (event == null || event.getSubOrderNo() == null || event.getSubOrderNo().isBlank()) {
      tradeMetrics.incrementMessageConsume("stock_release_request", "failed");
      return;
    }
    stockInventoryCommandService.handleReleaseRequest(event);
    tradeMetrics.incrementMessageConsume("stock_release_request", "success");
  }

  @Override
  protected Class<StockReleaseRequestEvent> payloadClass() {
    return StockReleaseRequestEvent.class;
  }

  @Override
  protected String payloadDescription() {
    return "StockReleaseRequestEvent";
  }

  @Override
  protected String resolveIdempotentNamespace(
      String topic, MessageExt msgExt, StockReleaseRequestEvent payload) {
    return NS_STOCK_RELEASE_REQUEST;
  }

  @Override
  protected String buildIdempotentKey(
      String topic, String msgId, StockReleaseRequestEvent payload, MessageExt msgExt) {
    return resolveEventId(
        "STOCK_RELEASE_REQUEST",
        payload == null ? null : payload.getEventId(),
        payload == null ? null : payload.getSubOrderNo());
  }
}
