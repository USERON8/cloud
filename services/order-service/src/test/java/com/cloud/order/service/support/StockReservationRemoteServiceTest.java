package com.cloud.order.service.support;

import com.cloud.api.stock.StockDubboApi;
import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockReservationRemoteServiceTest {

    @Mock
    private StockDubboApi stockDubboApi;

    private StockReservationRemoteService stockReservationRemoteService;

    @BeforeEach
    void setUp() {
        stockReservationRemoteService = new StockReservationRemoteService();
        ReflectionTestUtils.setField(stockReservationRemoteService, "stockDubboApi", stockDubboApi);
    }

    @Test
    void reserve_insufficientStock_translatesToResultCode() {
        StockOperateCommandDTO command = new StockOperateCommandDTO();
        when(stockDubboApi.reserve(command))
                .thenThrow(new RuntimeException("insufficient salable stock for sku"));

        assertThatThrownBy(() -> stockReservationRemoteService.reserve(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("insufficient salable stock")
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    org.assertj.core.api.Assertions.assertThat(be.getCode())
                            .isEqualTo(ResultCode.STOCK_INSUFFICIENT.getCode());
                });
    }

    @Test
    void reserve_businessException_passthrough() {
        StockOperateCommandDTO command = new StockOperateCommandDTO();
        BusinessException original = new BusinessException(422, "invalid");
        when(stockDubboApi.reserve(command)).thenThrow(original);

        assertThatThrownBy(() -> stockReservationRemoteService.reserve(command))
                .isSameAs(original);
    }
}
