package com.cloud.stock.rpc;

import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import com.cloud.common.domain.vo.stock.StockLedgerVO;
import com.cloud.stock.service.StockLedgerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockLedgerDubboServiceTest {

    @Mock
    private StockLedgerService stockLedgerService;

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
}
