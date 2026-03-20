package com.cloud.order.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.order.enums.OrderAction;
import com.cloud.order.service.OrderQueryService;
import com.cloud.order.service.OrderService;
import com.cloud.order.service.OrderShippingService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderBatchServiceImplTest {

  @Mock private OrderService orderService;

  @Mock private OrderQueryService orderQueryService;

  @Mock private OrderShippingService orderShippingService;

  private OrderBatchServiceImpl orderBatchService;

  @BeforeEach
  void setUp() {
    orderBatchService =
        new OrderBatchServiceImpl(orderService, orderQueryService, orderShippingService);
  }

  @Test
  void applyOrderActionShouldRejectDirectPay() {
    assertThatThrownBy(
            () -> orderBatchService.applyOrderAction(1L, null, OrderAction.PAY, null, null, null))
        .isInstanceOf(BizException.class)
        .satisfies(
            ex ->
                org.assertj.core.api.Assertions.assertThat(((BizException) ex).getCode())
                    .isEqualTo(ResultCode.BAD_REQUEST.getCode()))
        .hasMessageContaining("direct pay actions are disabled");

    verifyNoInteractions(orderService, orderQueryService, orderShippingService);
  }

  @Test
  void batchApplyShouldRejectDirectPay() {
    assertThatThrownBy(
            () ->
                orderBatchService.batchApply(
                    List.of(1L, 2L), null, OrderAction.PAY, null, null, null))
        .isInstanceOf(BizException.class)
        .satisfies(
            ex ->
                org.assertj.core.api.Assertions.assertThat(((BizException) ex).getCode())
                    .isEqualTo(ResultCode.BAD_REQUEST.getCode()))
        .hasMessageContaining("direct pay actions are disabled");

    verifyNoInteractions(orderService, orderQueryService, orderShippingService);
  }
}
