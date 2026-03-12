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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
        properties.getAutoConfirm().setBatchSize(20);
        properties.getAfterSale().setBatchSize(20);
        orderAutomationService = new OrderAutomationServiceImpl(
                orderSubMapper,
                afterSaleMapper,
                orderService,
                properties
        );
    }

    @Test
    void autoConfirmShippedOrdersShouldAdvanceReceipt() {
        OrderSub shipped = new OrderSub();
        shipped.setId(11L);
        shipped.setOrderStatus("SHIPPED");
        shipped.setShippedAt(LocalDateTime.now().minusDays(11));
        when(orderSubMapper.selectList(any())).thenReturn(List.of(shipped));

        int handled = orderAutomationService.autoConfirmShippedOrders();

        assertThat(handled).isEqualTo(1);
        verify(orderService).advanceSubOrderStatus(11L, "RECEIVE");
    }

    @Test
    void autoApproveTimedOutAfterSalesShouldCreateRefundForRefundType() {
        AfterSale applied = new AfterSale();
        applied.setId(21L);
        applied.setAfterSaleNo("AS100");
        applied.setMainOrderId(31L);
        applied.setSubOrderId(41L);
        applied.setAfterSaleType("REFUND");
        applied.setStatus("APPLIED");
        applied.setApplyAmount(new BigDecimal("18.80"));
        applied.setReason("timeout");
        applied.setCreatedAt(LocalDateTime.now().minusDays(3));

        AfterSale auditing = copyAfterSale(applied, "AUDITING");
        AfterSale approved = copyAfterSale(applied, "APPROVED");
        AfterSale refunding = copyAfterSale(applied, "REFUNDING");

        when(afterSaleMapper.selectList(any())).thenReturn(List.of(applied));
        when(orderService.advanceAfterSaleStatus(21L, "AUDIT", "system timeout auto audit")).thenReturn(auditing);
        when(orderService.advanceAfterSaleStatus(21L, "APPROVE", "system timeout auto approve")).thenReturn(approved);
        when(orderService.advanceAfterSaleStatus(21L, "PROCESS", "system timeout auto refund")).thenReturn(refunding);

        int handled = orderAutomationService.autoApproveTimedOutAfterSales();

        assertThat(handled).isEqualTo(1);
        verify(orderService).advanceAfterSaleStatus(21L, "PROCESS", "system timeout auto refund");
    }

    private AfterSale copyAfterSale(AfterSale source, String status) {
        AfterSale target = new AfterSale();
        target.setId(source.getId());
        target.setAfterSaleNo(source.getAfterSaleNo());
        target.setMainOrderId(source.getMainOrderId());
        target.setSubOrderId(source.getSubOrderId());
        target.setAfterSaleType(source.getAfterSaleType());
        target.setApplyAmount(source.getApplyAmount());
        target.setReason(source.getReason());
        target.setStatus(status);
        return target;
    }
}
