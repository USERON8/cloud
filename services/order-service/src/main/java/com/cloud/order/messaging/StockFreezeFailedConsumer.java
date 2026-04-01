package com.cloud.order.messaging;

import com.cloud.common.messaging.consumer.AbstractJsonMqConsumer;
import com.cloud.common.messaging.event.StockFreezeFailedEvent;
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
    topic = "stock-freeze-failed",
    consumerGroup = "order-stock-freeze-failed-consumer-group",
    selectorExpression = "STOCK_FREEZE_FAILED")
public class StockFreezeFailedConsumer extends AbstractJsonMqConsumer<StockFreezeFailedEvent> {

  private static final String NS_STOCK_FREEZE_FAILED = "order:stock:freeze-failed";

  private final OrderMainMapper orderMainMapper;
  private final OrderInventoryEventService orderInventoryEventService;
  private final TradeMetrics tradeMetrics;

  @Override
  protected void doConsume(StockFreezeFailedEvent event, MessageExt msgExt) {
    if (event == null || event.getOrderNo() == null || event.getOrderNo().isBlank()) {
      tradeMetrics.incrementMessageConsume("stock_freeze_failed", "failed");
      return;
    }
    OrderMain mainOrder = orderMainMapper.selectActiveByOrderNo(event.getOrderNo());
    if (mainOrder == null) {
      tradeMetrics.incrementMessageConsume("stock_freeze_failed", "failed");
      return;
    }
    orderInventoryEventService.handleStockFreezeFailed(event.getOrderNo());
    tradeMetrics.incrementMessageConsume("stock_freeze_failed", "success");
  }

  @Override
  protected Class<StockFreezeFailedEvent> payloadClass() {
    return StockFreezeFailedEvent.class;
  }

  @Override
  protected String payloadDescription() {
    return "StockFreezeFailedEvent";
  }

  @Override
  protected String resolveIdempotentNamespace(
      String topic, MessageExt msgExt, StockFreezeFailedEvent payload) {
    return NS_STOCK_FREEZE_FAILED;
  }

  @Override
  protected String buildIdempotentKey(
      String topic, String msgId, StockFreezeFailedEvent payload, MessageExt msgExt) {
    return resolveEventId(
        "STOCK_FREEZE_FAILED",
        payload == null ? null : payload.getEventId(),
        payload == null ? null : payload.getOrderNo());
  }

  @Override
  protected void onBizException(
      MessageExt msgExt,
      StockFreezeFailedEvent payload,
      com.cloud.common.exception.BizException ex) {
    tradeMetrics.incrementMessageConsume("stock_freeze_failed", "biz");
  }

  @Override
  protected void onSystemException(
      MessageExt msgExt,
      StockFreezeFailedEvent payload,
      com.cloud.common.exception.SystemException ex,
      boolean retryable) {
    tradeMetrics.incrementMessageConsume("stock_freeze_failed", retryable ? "retry" : "failed");
  }

  @Override
  protected void onUnknownException(
      MessageExt msgExt, StockFreezeFailedEvent payload, Exception ex) {
    tradeMetrics.incrementMessageConsume("stock_freeze_failed", "retry");
  }
}
