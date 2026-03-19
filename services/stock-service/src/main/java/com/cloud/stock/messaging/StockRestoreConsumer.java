package com.cloud.stock.messaging;

import com.cloud.common.messaging.consumer.AbstractJsonMqConsumer;
import com.cloud.common.messaging.event.StockRestoreEvent;
import com.cloud.common.metrics.TradeMetrics;
import com.cloud.stock.service.StockLedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
    topic = "stock-restore",
    consumerGroup = "stock-restore-consumer-group",
    selectorExpression = "STOCK_RESTORE")
public class StockRestoreConsumer extends AbstractJsonMqConsumer<StockRestoreEvent> {

  private static final String NS_STOCK_RESTORE = "stock:restore";

  private final StockLedgerService stockLedgerService;
  private final TradeMetrics tradeMetrics;

  @Override
  protected void doConsume(StockRestoreEvent event, MessageExt msgExt) {
    if (event == null || event.getItems() == null) {
      tradeMetrics.incrementMessageConsume("stock_restore", "failed");
      return;
    }
    stockLedgerService.rollbackBatch(event.getItems());
    tradeMetrics.incrementMessageConsume("stock_restore", "success");
  }

  @Override
  protected Class<StockRestoreEvent> payloadClass() {
    return StockRestoreEvent.class;
  }

  @Override
  protected String payloadDescription() {
    return "StockRestoreEvent";
  }

  @Override
  protected String resolveIdempotentNamespace(
      String topic, MessageExt msgExt, StockRestoreEvent payload) {
    return NS_STOCK_RESTORE;
  }

  @Override
  protected String buildIdempotentKey(
      String topic, String msgId, StockRestoreEvent payload, MessageExt msgExt) {
    return resolveEventId(
        "STOCK_RESTORE",
        payload == null ? null : payload.getEventId(),
        payload == null ? null : payload.getRefundNo());
  }

  @Override
  protected void onBizException(
      MessageExt msgExt, StockRestoreEvent payload, com.cloud.common.exception.BizException ex) {
    tradeMetrics.incrementMessageConsume("stock_restore", "biz");
  }

  @Override
  protected void onSystemException(
      MessageExt msgExt,
      StockRestoreEvent payload,
      com.cloud.common.exception.SystemException ex,
      boolean retryable) {
    tradeMetrics.incrementMessageConsume("stock_restore", retryable ? "retry" : "failed");
  }

  @Override
  protected void onUnknownException(MessageExt msgExt, StockRestoreEvent payload, Exception ex) {
    tradeMetrics.incrementMessageConsume("stock_restore", "retry");
  }
}
