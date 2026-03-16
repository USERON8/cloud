package com.cloud.order.service.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.api.stock.StockReserveTccApi;
import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import com.cloud.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StockReserveTccRemoteServiceTest {

  @Mock private StockReserveTccApi stockReserveTccApi;

  private StockReserveTccRemoteService stockReserveTccRemoteService;

  @BeforeEach
  void setUp() {
    stockReserveTccRemoteService = new StockReserveTccRemoteService();
    ReflectionTestUtils.setField(
        stockReserveTccRemoteService, "stockReserveTccApi", stockReserveTccApi);
  }

  @Test
  void tryReserve_nullCommand_returnsFalse() {
    assertThat(stockReserveTccRemoteService.tryReserve(null)).isFalse();
  }

  @Test
  void tryReserve_businessException_passthrough() {
    StockOperateCommandDTO command = new StockOperateCommandDTO();
    command.setOrderNo("M1");
    command.setSubOrderNo("S1");
    command.setSkuId(2L);
    command.setQuantity(1);
    command.setReason("reason");

    BusinessException original = new BusinessException("fail");
    when(stockReserveTccApi.tryReserve(any(), any(), any(), any(), any(), any()))
        .thenThrow(new RuntimeException(original));

    assertThatThrownBy(() -> stockReserveTccRemoteService.tryReserve(command)).isSameAs(original);

    verify(stockReserveTccApi).tryReserve(null, "M1", "S1", 2L, 1, "reason");
  }
}
