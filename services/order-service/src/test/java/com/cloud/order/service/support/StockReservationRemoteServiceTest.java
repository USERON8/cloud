package com.cloud.order.service.support;

import com.cloud.api.stock.StockDubboApi;
import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StockReservationRemoteServiceTest {

    private final StockDubboApi stockDubboApi = mock(StockDubboApi.class);
    private final StockReservationRemoteService stockReservationRemoteService = new StockReservationRemoteService();

    StockReservationRemoteServiceTest() {
        ReflectionTestUtils.setField(stockReservationRemoteService, "stockDubboApi", stockDubboApi);
    }

    @Test
    void reserveShouldTranslateWrappedInsufficientStockToBusinessException() {
        StockOperateCommandDTO command = new StockOperateCommandDTO();
        when(stockDubboApi.reserve(command))
                .thenThrow(new RuntimeException(new BusinessException(
                        "com.cloud.common.exception.BusinessException: insufficient salable stock\r\n"
                                + "BusinessException(code=502, message=insufficient salable stock)"
                )));

        assertThatThrownBy(() -> stockReservationRemoteService.reserve(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException businessException = (BusinessException) ex;
                    assertTrue("insufficient salable stock".equals(businessException.getMessage()));
                    assertTrue(businessException.getCode() == ResultCode.STOCK_INSUFFICIENT.getCode());
                });
    }

    @Test
    void reserveShouldPreserveWrappedBusinessExceptionMessage() {
        StockOperateCommandDTO command = new StockOperateCommandDTO();
        when(stockDubboApi.reserve(command))
                .thenThrow(new RuntimeException(new BusinessException(502, "stock ledger not found for skuId=999")));

        assertThatThrownBy(() -> stockReservationRemoteService.reserve(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException businessException = (BusinessException) ex;
                    assertTrue(businessException.getMessage().contains("stock ledger not found"));
                    assertTrue(businessException.getCode() == 502);
                });
    }
}
