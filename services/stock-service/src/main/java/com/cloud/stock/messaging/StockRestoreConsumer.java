package com.cloud.stock.messaging;

import com.cloud.common.messaging.consumer.AbstractMqConsumer;
import com.cloud.common.messaging.event.StockRestoreEvent;
import com.cloud.common.metrics.TradeMetrics;
import com.cloud.stock.service.StockLedgerService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class StockRestoreConsumer extends AbstractMqConsumer<StockRestoreEvent> {

  private static final String NS_STOCK_RESTORE = "stock:restore";

  private final StockLedgerService stockLedgerService;
  private final TradeMetrics tradeMetrics;
  private final ObjectMapper objectMapper;

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
  protected StockRestoreEvent deserialize(byte[] body) {
    try {
      return body == null ? null : objectMapper.readValue(body, StockRestoreEvent.class);
    } catch (Exception ex) {
      throw new IllegalArgumentException("Failed to deserialize StockRestoreEvent", ex);
    }
  }

  @Override
  protected String resolveIdempotentNamespace(
      String topic, MessageExt msgExt, StockRestoreEvent payload) {
    return NS_STOCK_RESTORE;
  }

  @Override
  protected String buildIdempotentKey(
      String topic, String msgId, StockRestoreEvent payload, MessageExt msgExt) {
    return resolveEventId(payload);
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

  private String resolveEventId(StockRestoreEvent event) {
    if (event != null && event.getEventId() != null && !event.getEventId().isBlank()) {
      return event.getEventId();
    }
    if (event != null && event.getRefundNo() != null && !event.getRefundNo().isBlank()) {
      return "STOCK_RESTORE:" + event.getRefundNo();
    }
    return "STOCK_RESTORE:" + System.currentTimeMillis();
  }
}
