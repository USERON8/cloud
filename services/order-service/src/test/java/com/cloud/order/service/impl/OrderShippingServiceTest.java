package com.cloud.order.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.common.exception.BizException;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.enums.OrderAction;
import com.cloud.order.mapper.OrderSubMapper;
import com.cloud.order.service.OrderService;
import com.cloud.order.service.support.OrderAggregateCacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderShippingServiceTest {

  @Mock private OrderSubMapper orderSubMapper;

  @Mock private OrderService orderService;

  @Mock private OrderAggregateCacheService orderAggregateCacheService;

  @InjectMocks private OrderShippingServiceImpl orderShippingService;

  @Test
  void ship_success_updatesStatusAndEvictsCache() {
    OrderSub existing = new OrderSub();
    existing.setId(1L);
    existing.setMainOrderId(100L);
    existing.setOrderStatus("PAID");
    existing.setDeleted(0);

    OrderSub latest = new OrderSub();
    latest.setId(1L);
    latest.setMainOrderId(100L);
    latest.setOrderStatus("SHIPPED");
    latest.setDeleted(0);

    when(orderSubMapper.selectById(1L)).thenReturn(existing, latest);
    when(orderSubMapper.updateShippingInfo(
            org.mockito.ArgumentMatchers.eq(1L),
            org.mockito.ArgumentMatchers.eq("SF"),
            org.mockito.ArgumentMatchers.eq("TRACK1"),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.eq("SHIPPED")))
        .thenReturn(1);

    OrderSub result = orderShippingService.ship(1L, "SF", "TRACK1");

    assertThat(result).isSameAs(latest);
    verify(orderService).advanceSubOrderStatus(1L, OrderAction.SHIP);
    verify(orderAggregateCacheService).evict(100L);
  }

  @Test
  void ship_missingCarrier_throws() {
    assertThatThrownBy(() -> orderShippingService.ship(1L, " ", ""))
        .isInstanceOf(BizException.class)
        .hasMessageContaining("shipping company and tracking number are required");
  }
}
