package com.cloud.stock.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.metrics.TradeMetrics;
import com.cloud.stock.mapper.StockLedgerMapper;
import com.cloud.stock.mapper.StockReservationMapper;
import com.cloud.stock.messaging.StockMessageProducer;
import com.cloud.stock.module.entity.StockLedger;
import com.cloud.stock.module.entity.StockReservation;
import com.cloud.stock.service.support.StockRedisCacheService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class StockLedgerServiceImplTest {

  @Mock private StockLedgerMapper stockLedgerMapper;

  @Mock private StockReservationMapper stockReservationMapper;

  @Mock private StockTxnAsyncWriter stockTxnAsyncWriter;

  @Mock private StockMessageProducer stockMessageProducer;

  @Mock private TradeMetrics tradeMetrics;

  @Mock private StockRedisCacheService stockRedisCacheService;

  @InjectMocks private StockLedgerServiceImpl stockLedgerService;

  @Test
  void listLedgersBySkuIds_returnsVoList() {
    StockLedger activeLedger = new StockLedger();
    activeLedger.setId(1L);
    activeLedger.setSkuId(12L);
    activeLedger.setSalableQty(7);
    activeLedger.setStatus(1);
    StockLedger inactiveLedger = new StockLedger();
    inactiveLedger.setId(2L);
    inactiveLedger.setSkuId(12L);
    inactiveLedger.setSalableQty(99);
    inactiveLedger.setStatus(0);
    when(stockLedgerMapper.listActiveBySkuIds(List.of(12L)))
        .thenReturn(List.of(activeLedger, inactiveLedger));

    List<com.cloud.common.domain.vo.stock.StockLedgerVO> result =
        stockLedgerService.listLedgersBySkuIds(java.util.Arrays.asList(12L, 12L, null));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getSkuId()).isEqualTo(12L);
    assertThat(result.get(0).getSalableQty()).isEqualTo(7);
  }

  @Test
  void reserve_duplicateReservationAlreadyReserved_skips() {
    StockOperateCommandDTO command = new StockOperateCommandDTO();
    command.setSubOrderNo("S1");
    command.setSkuId(10L);
    command.setQuantity(2);
    command.setOrderNo("M1");

    when(stockReservationMapper.insert(org.mockito.ArgumentMatchers.<StockReservation>any()))
        .thenThrow(new DuplicateKeyException("dup"));
    StockReservation existing = new StockReservation();
    existing.setSubOrderNo("S1");
    existing.setSkuId(10L);
    existing.setReservedQty(2);
    existing.setStatus("RESERVED");
    when(stockReservationMapper.selectActiveBySubOrderNoAndSkuId("S1", 10L)).thenReturn(existing);

    Boolean result = stockLedgerService.reserve(command);

    assertThat(result).isTrue();
    verify(stockLedgerMapper, never()).reserve(any(), any());
    verify(tradeMetrics).incrementStockFreeze("success");
  }

  @Test
  void reserve_insufficientStock_sendsFreezeFailedEvent() {
    StockOperateCommandDTO command = new StockOperateCommandDTO();
    command.setSubOrderNo("S2");
    command.setSkuId(11L);
    command.setQuantity(5);
    command.setOrderNo("M2");

    when(stockLedgerMapper.reserve(11L, 5)).thenReturn(0);

    assertThatThrownBy(() -> stockLedgerService.reserve(command))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("insufficient salable stock");

    verify(tradeMetrics).incrementStockFreeze("failed");
    verify(stockMessageProducer)
        .sendStockFreezeFailedEvent(eq("M2"), eq("insufficient salable stock"));
  }

  @Test
  void rollbackBatch_shouldRejectQuantityExceedingReservedQuantity() {
    StockOperateCommandDTO command = new StockOperateCommandDTO();
    command.setSubOrderNo("S3");
    command.setSkuId(15L);
    command.setQuantity(3);

    StockReservation reservation = new StockReservation();
    reservation.setId(9L);
    reservation.setSubOrderNo("S3");
    reservation.setSkuId(15L);
    reservation.setReservedQty(2);
    reservation.setStatus("CONFIRMED");
    when(stockReservationMapper.selectActiveBySubOrderNosAndSkuIds(List.of("S3"), List.of(15L)))
        .thenReturn(List.of(reservation));

    assertThatThrownBy(() -> stockLedgerService.rollbackBatch(List.of(command)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("rollback quantity exceeds reserved quantity");

    verify(stockLedgerMapper, never()).batchRollbackAfterConfirm(any());
    verify(stockReservationMapper, never()).adjustAfterRollback(any(), any(), any(), any());
  }

  @Test
  void rollbackBatch_shouldAggregateDuplicateCommandsAndKeepPartialReservation() {
    StockOperateCommandDTO first = new StockOperateCommandDTO();
    first.setSubOrderNo("S4");
    first.setSkuId(21L);
    first.setQuantity(1);
    first.setReason("refund");

    StockOperateCommandDTO second = new StockOperateCommandDTO();
    second.setSubOrderNo("S4");
    second.setSkuId(21L);
    second.setQuantity(1);
    second.setReason("refund");

    StockReservation reservation = new StockReservation();
    reservation.setId(11L);
    reservation.setSubOrderNo("S4");
    reservation.setSkuId(21L);
    reservation.setReservedQty(3);
    reservation.setStatus("CONFIRMED");
    when(stockReservationMapper.selectActiveBySubOrderNosAndSkuIds(List.of("S4"), List.of(21L)))
        .thenReturn(List.of(reservation));
    when(stockLedgerMapper.batchRollbackAfterConfirm(any())).thenReturn(1);
    when(stockReservationMapper.adjustAfterRollback(11L, 2, "CONFIRMED", "CONFIRMED"))
        .thenReturn(1);
    StockLedger ledger = new StockLedger();
    ledger.setSkuId(21L);
    ledger.setOnHandQty(12);
    ledger.setReservedQty(0);
    ledger.setSalableQty(10);
    when(stockLedgerMapper.selectActiveBySkuId(21L)).thenReturn(ledger);
    when(stockRedisCacheService.applyRollbackAfterConfirmIfCached(21L, 2))
        .thenReturn(StockRedisCacheService.CacheResult.OK);

    Boolean result = stockLedgerService.rollbackBatch(List.of(first, second));

    assertThat(result).isTrue();
    verify(stockLedgerMapper).batchRollbackAfterConfirm(any());
    verify(stockReservationMapper).adjustAfterRollback(11L, 2, "CONFIRMED", "CONFIRMED");
    verify(stockRedisCacheService).applyRollbackAfterConfirmIfCached(21L, 2);
    verify(stockTxnAsyncWriter, times(1)).write(any());
  }

  @Test
  void reserve_shouldPropagateTxnWriteFailure() {
    StockOperateCommandDTO command = new StockOperateCommandDTO();
    command.setSubOrderNo("S5");
    command.setSkuId(31L);
    command.setQuantity(2);
    command.setOrderNo("M5");
    command.setReason("reserve");

    when(stockReservationMapper.insert(org.mockito.ArgumentMatchers.<StockReservation>any()))
        .thenAnswer(
            invocation -> {
              StockReservation reservation = invocation.getArgument(0);
              reservation.setId(12L);
              return 1;
            });
    when(stockLedgerMapper.reserve(31L, 2)).thenReturn(1);
    when(stockRedisCacheService.applyReserveIfCached(31L, 2))
        .thenReturn(StockRedisCacheService.CacheResult.OK);
    doThrow(new RuntimeException("txn write failed")).when(stockTxnAsyncWriter).write(any());

    assertThatThrownBy(() -> stockLedgerService.reserve(command))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("txn write failed");

    verify(tradeMetrics).incrementStockFreeze("failed");
  }
}
