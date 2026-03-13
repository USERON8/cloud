package com.cloud.order.service.impl;

import com.cloud.common.exception.BusinessException;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.mapper.OrderSubMapper;
import com.cloud.order.service.OrderService;
import com.cloud.order.service.support.OrderAggregateCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderShippingServiceTest {

    @Mock
    private OrderSubMapper orderSubMapper;

    @Mock
    private OrderService orderService;

    @Mock
    private OrderAggregateCacheService orderAggregateCacheService;

    private OrderShippingService orderShippingService;

    @BeforeEach
    void setUp() {
        orderShippingService = new OrderShippingService(
                orderSubMapper,
                orderService,
                orderAggregateCacheService
        );
    }

    @Test
    void shipShouldUpdateShippingAndAdvanceStatus() {
        OrderSub subOrder = new OrderSub();
        subOrder.setId(11L);
        subOrder.setMainOrderId(21L);
        subOrder.setOrderStatus("PAID");
        subOrder.setDeleted(0);

        OrderSub latest = new OrderSub();
        latest.setId(11L);
        latest.setMainOrderId(21L);

        when(orderSubMapper.selectById(11L)).thenReturn(subOrder, latest);
        when(orderSubMapper.updateShippingInfo(eq(11L), eq("SF"), eq("TN001"),
                any(), any(), eq("SHIPPED"))).thenReturn(1);

        OrderSub result = orderShippingService.ship(11L, " SF ", " TN001 ");

        assertThat(result).isSameAs(latest);
        ArgumentCaptor<LocalDate> arrivalCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(orderSubMapper).updateShippingInfo(eq(11L), eq("SF"), eq("TN001"),
                arrivalCaptor.capture(), any(), eq("SHIPPED"));
        assertThat(arrivalCaptor.getValue()).isEqualTo(LocalDate.now().plusDays(3));
        verify(orderService).advanceSubOrderStatus(11L, "SHIP");
        verify(orderAggregateCacheService).evict(21L);
    }

    @Test
    void shipShouldNotAdvanceWhenAlreadyShipped() {
        OrderSub subOrder = new OrderSub();
        subOrder.setId(12L);
        subOrder.setMainOrderId(22L);
        subOrder.setOrderStatus("SHIPPED");
        subOrder.setDeleted(0);

        OrderSub latest = new OrderSub();
        latest.setId(12L);
        latest.setMainOrderId(22L);

        when(orderSubMapper.selectById(12L)).thenReturn(subOrder, latest);
        when(orderSubMapper.updateShippingInfo(eq(12L), eq("YTO"), eq("TN002"),
                any(), any(), eq("SHIPPED"))).thenReturn(1);

        orderShippingService.ship(12L, "YTO", "TN002");

        verify(orderService, never()).advanceSubOrderStatus(12L, "SHIP");
        verify(orderAggregateCacheService).evict(22L);
    }

    @Test
    void shipShouldRejectMissingFields() {
        assertThatThrownBy(() -> orderShippingService.ship(1L, "", "TN"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> orderShippingService.ship(1L, "SF", ""))
                .isInstanceOf(BusinessException.class);
    }
}
