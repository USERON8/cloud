package com.cloud.order.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.order.config.OrderAutomationProperties;
import com.cloud.order.entity.AfterSale;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.enums.OrderAction;
import com.cloud.order.mapper.AfterSaleMapper;
import com.cloud.order.mapper.OrderSubMapper;
import com.cloud.order.service.OrderService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderAutomationServiceImplTest {

  @Mock private OrderSubMapper orderSubMapper;
  @Mock private AfterSaleMapper afterSaleMapper;
  @Mock private OrderService orderService;

  private OrderAutomationProperties properties;
  private OrderAutomationServiceImpl orderAutomationService;

  @BeforeEach
  void setUp() {
    properties = buildProperties();
    orderAutomationService =
        new OrderAutomationServiceImpl(orderSubMapper, afterSaleMapper, orderService, properties);
  }

  @Test
  void autoConfirmShippedOrdersUsesDedicatedMapperMethod() {
    OrderSub first = new OrderSub();
    first.setId(101L);
    OrderSub second = new OrderSub();
    second.setId(102L);

    when(orderSubMapper.listAutoConfirmCandidates(any(LocalDateTime.class), eq(20)))
        .thenReturn(List.of(first, second));

    int result = orderAutomationService.autoConfirmShippedOrders();

    assertEquals(2, result);
    verify(orderSubMapper).listAutoConfirmCandidates(any(LocalDateTime.class), eq(20));
    verify(orderSubMapper, never()).selectList(any());
    verify(orderService).advanceSubOrderStatus(101L, OrderAction.DONE);
    verify(orderService).advanceSubOrderStatus(102L, OrderAction.DONE);
  }

  @Test
  void autoApproveTimedOutAfterSalesKeepsExistingFlow() {
    AfterSale afterSale = new AfterSale();
    afterSale.setId(201L);
    afterSale.setStatus("APPROVED");
    afterSale.setAfterSaleType("REFUND");

    when(afterSaleMapper.selectList(any())).thenReturn(List.of(afterSale));

    int result = orderAutomationService.autoApproveTimedOutAfterSales();

    assertEquals(1, result);
    verify(orderService)
        .advanceAfterSaleStatus(
            201L, com.cloud.order.enums.AfterSaleAction.PROCESS, "system timeout auto refund");
  }

  private OrderAutomationProperties buildProperties() {
    OrderAutomationProperties properties = new OrderAutomationProperties();
    properties.getAutoConfirm().setBatchSize(20);
    properties.getAutoConfirm().setAfterHours(24);
    properties.getAfterSale().setBatchSize(10);
    properties.getAfterSale().setAuditTimeoutHours(48);
    return properties;
  }
}
