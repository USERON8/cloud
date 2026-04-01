package com.cloud.stock.service.support;

import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import com.cloud.common.messaging.event.StockConfirmRequestEvent;
import com.cloud.common.messaging.event.StockReleaseRequestEvent;
import com.cloud.common.messaging.event.StockReserveRequestEvent;
import com.cloud.stock.messaging.StockMessageProducer;
import com.cloud.stock.service.StockLedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockInventoryCommandService {

  private final StockLedgerService stockLedgerService;
  private final StockMessageProducer stockMessageProducer;

  @Transactional(rollbackFor = Exception.class)
  public void handleReserveRequest(StockReserveRequestEvent event) {
    if (event == null || event.getItems() == null || event.getItems().isEmpty()) {
      return;
    }
    if (!Boolean.TRUE.equals(stockLedgerService.preCheck(event.getItems()))) {
      stockMessageProducer.sendStockFreezeFailedEvent(
          event.getOrderNo(), "insufficient available stock");
      return;
    }
    for (StockOperateCommandDTO command : event.getItems()) {
      stockLedgerService.reserve(command);
    }
  }

  @Transactional(rollbackFor = Exception.class)
  public void handleConfirmRequest(StockConfirmRequestEvent event) {
    if (event == null || event.getItems() == null || event.getItems().isEmpty()) {
      return;
    }
    for (StockOperateCommandDTO command : event.getItems()) {
      stockLedgerService.confirm(command);
    }
  }

  @Transactional(rollbackFor = Exception.class)
  public void handleReleaseRequest(StockReleaseRequestEvent event) {
    if (event == null || event.getItems() == null || event.getItems().isEmpty()) {
      return;
    }
    for (StockOperateCommandDTO command : event.getItems()) {
      stockLedgerService.release(command);
    }
  }
}
