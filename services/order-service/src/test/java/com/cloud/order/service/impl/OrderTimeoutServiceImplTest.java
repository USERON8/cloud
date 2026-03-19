package com.cloud.order.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.common.exception.BizException;
import com.cloud.order.entity.OrderMain;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.enums.OrderAction;
import com.cloud.order.mapper.OrderMainMapper;
import com.cloud.order.mapper.OrderSubMapper;
import com.cloud.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderTimeoutServiceImplTest {

  @Mock private OrderSubMapper orderSubMapper;

  @Mock private OrderMainMapper orderMainMapper;

  @Mock private OrderService orderService;

  @InjectMocks private OrderTimeoutServiceImpl orderTimeoutService;

  @Test
  void updateTimeoutConfig_invalid_throws() {
    assertThatThrownBy(() -> orderTimeoutService.updateTimeoutConfig(0))
        .isInstanceOf(BizException.class)
        .hasMessageContaining("timeoutMinutes must be greater than 0");
  }

  @Test
  void cancelTimeoutOrder_success_updatesMainOrderWhenAllClosed() {
    OrderSub subOrder = new OrderSub();
    subOrder.setId(11L);
    subOrder.setMainOrderId(99L);
    when(orderService.advanceSubOrderStatus(11L, OrderAction.CANCEL)).thenReturn(subOrder);
    when(orderSubMapper.selectCount(any())).thenReturn(0L);

    OrderMain mainOrder = new OrderMain();
    mainOrder.setId(99L);
    mainOrder.setDeleted(0);
    mainOrder.setOrderStatus("CREATED");
    when(orderMainMapper.selectById(99L)).thenReturn(mainOrder);
    when(orderMainMapper.updateById(mainOrder)).thenReturn(1);

    boolean result = orderTimeoutService.cancelTimeoutOrder(11L);

    assertThat(result).isTrue();
    assertThat(mainOrder.getOrderStatus()).isEqualTo("CANCELLED");
    verify(orderMainMapper).updateById(mainOrder);
  }
}
