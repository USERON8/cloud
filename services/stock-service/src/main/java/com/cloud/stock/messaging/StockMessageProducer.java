package com.cloud.stock.messaging;

import com.cloud.common.messaging.event.StockAlertEvent;
import com.cloud.common.messaging.event.StockFreezeFailedEvent;
import com.cloud.common.messaging.outbox.OutboxEventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockMessageProducer {

  private final OutboxEventService outboxEventService;
  private final ObjectMapper objectMapper;

  @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
  public boolean sendStockFreezeFailedEvent(String orderNo, String reason) {
    if (!StringUtils.hasText(orderNo)) {
      log.warn("Skip stock-freeze-failed event: orderNo missing");
      return false;
    }
    try {
      StockFreezeFailedEvent event = new StockFreezeFailedEvent();
      event.setOrderNo(orderNo.trim());
      event.setReason(reason);
      event.setTimestamp(System.currentTimeMillis());
      String trimmedOrderNo = orderNo.trim();
      event.setEventId(
          trimmedOrderNo.length() <= 64 ? trimmedOrderNo : UUID.randomUUID().toString());
      event.setEventType("STOCK_FREEZE_FAILED");

      String payload = objectMapper.writeValueAsString(event);
      outboxEventService.enqueue(
          "STOCK", trimmedOrderNo, event.getEventType(), payload, event.getEventId());
      return true;
    } catch (Exception ex) {
      log.warn("Send stock-freeze-failed event failed: orderNo={}", orderNo, ex);
      return false;
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
  public boolean sendStockAlertEvent(StockAlertEvent event) {
    if (event == null || event.getSkuId() == null || event.getMerchantId() == null) {
      log.warn("Skip stock-alert event: skuId or merchantId missing");
      return false;
    }
    try {
      if (!StringUtils.hasText(event.getEventType())) {
        event.setEventType("STOCK_ALERT");
      }
      if (!StringUtils.hasText(event.getEventId())) {
        event.setEventId(UUID.randomUUID().toString());
      }
      if (event.getTimestamp() == null) {
        event.setTimestamp(System.currentTimeMillis());
      }
      String payload = objectMapper.writeValueAsString(event);
      outboxEventService.enqueue(
          "STOCK",
          String.valueOf(event.getSkuId()),
          event.getEventType(),
          payload,
          event.getEventId());
      return true;
    } catch (Exception ex) {
      log.warn("Send stock-alert event failed: skuId={}", event.getSkuId(), ex);
      return false;
    }
  }
}
