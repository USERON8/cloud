package com.cloud.stock.service.support;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import com.cloud.common.messaging.event.StockConfirmRequestEvent;
import com.cloud.common.messaging.event.StockReleaseRequestEvent;
import com.cloud.common.messaging.event.StockReserveRequestEvent;
import com.cloud.stock.messaging.StockMessageProducer;
import com.cloud.stock.service.StockLedgerService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockInventoryCommandServiceTest {

  @Mock private StockLedgerService stockLedgerService;
  @Mock private StockMessageProducer stockMessageProducer;

  @InjectMocks private StockInventoryCommandService service;

  @Test
  void reserveRequestDoesNothingWhenEventIsEmpty() {
    service.handleReserveRequest(null);
    service.handleReserveRequest(new StockReserveRequestEvent());

    verifyNoInteractions(stockLedgerService, stockMessageProducer);
  }

  @Test
  void reserveRequestSendsFreezeFailedEventWhenPreCheckFails() {
    StockOperateCommandDTO command = stockCommand(10001L, 2);
    StockReserveRequestEvent event =
        StockReserveRequestEvent.builder().orderNo("ORD-1").items(List.of(command)).build();
    when(stockLedgerService.preCheck(List.of(command))).thenReturn(false);

    service.handleReserveRequest(event);

    verify(stockLedgerService).preCheck(List.of(command));
    verify(stockLedgerService, never()).reserve(command);
    verify(stockMessageProducer).sendStockFreezeFailedEvent("ORD-1", "insufficient available stock");
    verify(stockMessageProducer, never()).sendStockReservedEvent("ORD-1");
  }

  @Test
  void reserveRequestReservesEveryCommandAndPublishesReservedEvent() {
    StockOperateCommandDTO first = stockCommand(10001L, 2);
    StockOperateCommandDTO second = stockCommand(10002L, 1);
    StockReserveRequestEvent event =
        StockReserveRequestEvent.builder().orderNo("ORD-2").items(List.of(first, second)).build();
    when(stockLedgerService.preCheck(List.of(first, second))).thenReturn(true);
    when(stockMessageProducer.sendStockReservedEvent("ORD-2")).thenReturn(true);

    service.handleReserveRequest(event);

    verify(stockLedgerService).reserve(first);
    verify(stockLedgerService).reserve(second);
    verify(stockMessageProducer).sendStockReservedEvent("ORD-2");
  }

  @Test
  void reserveRequestThrowsWhenReservedEventCannotBeQueued() {
    StockOperateCommandDTO command = stockCommand(10001L, 2);
    StockReserveRequestEvent event =
        StockReserveRequestEvent.builder().orderNo("ORD-3").items(List.of(command)).build();
    when(stockLedgerService.preCheck(List.of(command))).thenReturn(true);
    when(stockMessageProducer.sendStockReservedEvent("ORD-3")).thenReturn(false);

    assertThrows(IllegalStateException.class, () -> service.handleReserveRequest(event));

    verify(stockLedgerService).reserve(command);
    verify(stockMessageProducer).sendStockReservedEvent("ORD-3");
  }

  @Test
  void confirmAndReleaseRequestsForwardEachCommand() {
    StockOperateCommandDTO first = stockCommand(10001L, 2);
    StockOperateCommandDTO second = stockCommand(10002L, 1);

    service.handleConfirmRequest(
        StockConfirmRequestEvent.builder().orderNo("ORD-4").items(List.of(first, second)).build());
    service.handleReleaseRequest(
        StockReleaseRequestEvent.builder().orderNo("ORD-4").items(List.of(first, second)).build());

    verify(stockLedgerService).confirm(first);
    verify(stockLedgerService).confirm(second);
    verify(stockLedgerService).release(first);
    verify(stockLedgerService).release(second);
  }

  private StockOperateCommandDTO stockCommand(Long skuId, int quantity) {
    StockOperateCommandDTO command = new StockOperateCommandDTO();
    command.setOrderNo("ORD");
    command.setSubOrderNo("SUB");
    command.setSkuId(skuId);
    command.setQuantity(quantity);
    return command;
  }
}
