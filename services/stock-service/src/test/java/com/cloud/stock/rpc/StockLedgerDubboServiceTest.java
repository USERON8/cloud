package com.cloud.stock.rpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import com.cloud.common.domain.vo.stock.StockLedgerVO;
import com.cloud.stock.service.StockLedgerService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockLedgerDubboServiceTest {

  @Mock private StockLedgerService stockLedgerService;

  private StockLedgerDubboService stockLedgerDubboService;

  @BeforeEach
  void setUp() {
    stockLedgerDubboService = new StockLedgerDubboService(stockLedgerService);
  }

  @Test
  void getLedgerBySkuId_delegates() {
    StockLedgerVO vo = new StockLedgerVO();
    when(stockLedgerService.getLedgerBySkuId(1L)).thenReturn(vo);

    StockLedgerVO result = stockLedgerDubboService.getLedgerBySkuId(1L);

    assertThat(result).isSameAs(vo);
  }

  @Test
  void reserve_delegates() {
    StockOperateCommandDTO command = new StockOperateCommandDTO();
    when(stockLedgerService.reserve(command)).thenReturn(true);

    Boolean result = stockLedgerDubboService.reserve(command);

    assertThat(result).isTrue();
    verify(stockLedgerService).reserve(command);
  }

  @Test
  void listLedgersBySkuIds_delegates() {
    StockLedgerVO vo = new StockLedgerVO();
    vo.setSkuId(11L);
    when(stockLedgerService.listLedgersBySkuIds(List.of(11L))).thenReturn(List.of(vo));

    List<StockLedgerVO> result = stockLedgerDubboService.listLedgersBySkuIds(List.of(11L));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getSkuId()).isEqualTo(11L);
    verify(stockLedgerService).listLedgersBySkuIds(List.of(11L));
  }
}
