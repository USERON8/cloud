package com.cloud.stock.messaging;

import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import com.cloud.common.messaging.MessageIdempotencyService;
import com.cloud.common.messaging.event.StockRestoreEvent;
import com.cloud.common.metrics.TradeMetrics;
import com.cloud.stock.service.StockLedgerService;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockRestoreConsumer {

  private static final String NS_STOCK_RESTORE = "stock:restore";

  private final MessageIdempotencyService messageIdempotencyService;
  private final StockLedgerService stockLedgerService;
  private final TradeMetrics tradeMetrics;

  @Bean
  public Consumer<List<Message<StockRestoreEvent>>> stockRestoreConsumer() {
    return messages -> {
      if (messages == null || messages.isEmpty()) {
        return;
      }
      Set<String> inFlight = new HashSet<>();

      try {
        for (Message<StockRestoreEvent> message : messages) {
          if (message == null) {
            continue;
          }
          StockRestoreEvent event = message.getPayload();
          String eventId = resolveEventId(event);
          if (!messageIdempotencyService.tryAcquire(NS_STOCK_RESTORE, eventId)) {
            log.warn("Duplicate stock restore event, skip: eventId={}", eventId);
            continue;
          }
          inFlight.add(eventId);

          try {
            if (event == null || event.getItems() == null) {
              tradeMetrics.incrementMessageConsume("stock_restore", "failed");
              messageIdempotencyService.markSuccess(NS_STOCK_RESTORE, eventId);
              inFlight.remove(eventId);
              continue;
            }
            List<StockOperateCommandDTO> items = event.getItems();
            stockLedgerService.rollbackBatch(items);
            tradeMetrics.incrementMessageConsume("stock_restore", "success");
            messageIdempotencyService.markSuccess(NS_STOCK_RESTORE, eventId);
            inFlight.remove(eventId);
          } catch (Exception ex) {
            tradeMetrics.incrementMessageConsume("stock_restore", "retry");
            log.error(
                "Handle stock restore failed: eventId={}, refundNo={}",
                eventId,
                event == null ? null : event.getRefundNo(),
                ex);
            throw new RuntimeException("Handle stock restore failed", ex);
          }
        }
      } catch (Exception ex) {
        for (String eventId : inFlight) {
          messageIdempotencyService.release(NS_STOCK_RESTORE, eventId);
        }
        throw ex;
      }
    };
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
