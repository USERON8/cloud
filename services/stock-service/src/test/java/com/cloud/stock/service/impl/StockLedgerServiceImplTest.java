package com.cloud.stock.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
    StockLedger ledger = new StockLedger();
    ledger.setId(1L);
    ledger.setSkuId(12L);
    ledger.setSalableQty(7);
    when(stockLedgerMapper.listActiveBySkuIds(List.of(12L))).thenReturn(List.of(ledger));

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
}
