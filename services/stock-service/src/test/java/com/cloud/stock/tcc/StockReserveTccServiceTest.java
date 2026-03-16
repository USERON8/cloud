package com.cloud.stock.tcc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import com.cloud.stock.service.StockLedgerService;
import org.apache.seata.rm.tcc.api.BusinessActionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockReserveTccServiceTest {

  @Mock private StockLedgerService stockLedgerService;

  private StockReserveTccService service;

  @BeforeEach
  void setUp() {
    service = new StockReserveTccService(stockLedgerService);
  }

  @Test
  void commitReserveShouldIgnoreEmptyContext() {
    boolean result = service.commitReserve(null);

    assertThat(result).isTrue();
    verify(stockLedgerService, never()).confirmReservation(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void cancelReserveShouldIgnoreMissingFields() {
    BusinessActionContext context = mock(BusinessActionContext.class);
    when(context.getActionContext("subOrderNo")).thenReturn(null);

    boolean result = service.cancelReserve(context);

    assertThat(result).isTrue();
    verify(stockLedgerService, never()).rollback(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void commitReserveShouldBuildCommandFromContext() {
    BusinessActionContext context = mock(BusinessActionContext.class);
    when(context.getActionContext("orderNo")).thenReturn("M-1");
    when(context.getActionContext("subOrderNo")).thenReturn("S-1");
    when(context.getActionContext("skuId")).thenReturn(101L);
    when(context.getActionContext("quantity")).thenReturn(2);
    when(context.getActionContext("reason")).thenReturn("reserve");
    when(stockLedgerService.confirmReservation(org.mockito.ArgumentMatchers.any()))
        .thenReturn(true);

    boolean result = service.commitReserve(context);

    assertThat(result).isTrue();
    ArgumentCaptor<StockOperateCommandDTO> captor =
        ArgumentCaptor.forClass(StockOperateCommandDTO.class);
    verify(stockLedgerService).confirmReservation(captor.capture());
    StockOperateCommandDTO command = captor.getValue();
    assertThat(command.getOrderNo()).isEqualTo("M-1");
    assertThat(command.getSubOrderNo()).isEqualTo("S-1");
    assertThat(command.getSkuId()).isEqualTo(101L);
    assertThat(command.getQuantity()).isEqualTo(2);
    assertThat(command.getReason()).isEqualTo("reserve");
  }
}
