package com.cloud.order.service.impl;

import com.cloud.order.config.OrderAutomationProperties;
import com.cloud.order.entity.AfterSale;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.mapper.AfterSaleMapper;
import com.cloud.order.mapper.OrderSubMapper;
import com.cloud.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderAutomationServiceImplTest {

    @Mock
    private OrderSubMapper orderSubMapper;

    @Mock
    private AfterSaleMapper afterSaleMapper;

    @Mock
    private OrderService orderService;

    private OrderAutomationServiceImpl orderAutomationService;

    @BeforeEach
    void setUp() {
        OrderAutomationProperties properties = new OrderAutomationProperties();
        orderAutomationService = new OrderAutomationServiceImpl(orderSubMapper, afterSaleMapper, orderService, properties);
    }

    @Test
    void autoConfirmShippedOrders_countsOnlySuccess() {
        OrderSub sub1 = new OrderSub();
        sub1.setId(1L);
        OrderSub sub2 = new OrderSub();
        sub2.setId(2L);
        when(orderSubMapper.selectList(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(sub1, sub2));
        when(orderService.advanceSubOrderStatus(1L, "DONE")).thenReturn(sub1);
        when(orderService.advanceSubOrderStatus(2L, "DONE")).thenThrow(new RuntimeException("fail"));

        int handled = orderAutomationService.autoConfirmShippedOrders();

        assertThat(handled).isEqualTo(1);
    }

    @Test
    void autoApproveTimedOutAfterSales_triggersRefund() {
        AfterSale afterSale = new AfterSale();
        afterSale.setId(10L);
        afterSale.setStatus("APPROVED");
        afterSale.setAfterSaleType("REFUND");
        when(afterSaleMapper.selectList(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(afterSale));

        when(orderService.advanceAfterSaleStatus(eq(10L), eq("PROCESS"), anyString()))
                .thenReturn(afterSale);

        int handled = orderAutomationService.autoApproveTimedOutAfterSales();

        assertThat(handled).isEqualTo(1);
    }
}
