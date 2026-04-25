package com.cloud.order.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.common.domain.vo.payment.PaymentOrderVO;
import com.cloud.common.exception.BizException;
import com.cloud.order.entity.OrderMain;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.enums.OrderAction;
import com.cloud.order.service.OrderQueryService;
import com.cloud.order.service.OrderService;
import com.cloud.order.service.OrderShippingService;
import com.cloud.order.service.support.PaymentOrderRemoteService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderBatchServiceImplTest {

  @Mock private OrderService orderService;
  @Mock private OrderQueryService orderQueryService;
  @Mock private OrderShippingService orderShippingService;
  @Mock private PaymentOrderRemoteService paymentOrderRemoteService;

  @InjectMocks private OrderBatchServiceImpl orderBatchService;

  @Test
  void applyOrderActionRejectsPaidSubOrderCancellationBeforeMutation() {
    OrderMain mainOrder = buildMainOrder(100L);
    when(orderQueryService.requireAccessibleMainOrder(eq(100L), any())).thenReturn(mainOrder);
    when(orderService.listSubOrders(100L))
        .thenReturn(List.of(buildSubOrder(1L, "CREATED"), buildSubOrder(2L, "PAID")));

    assertThrows(
        BizException.class,
        () -> orderBatchService.applyOrderAction(100L, null, OrderAction.CANCEL, null, null, null));

    verify(orderService, never()).advanceSubOrderStatus(anyLong(), eq(OrderAction.CANCEL));
    verify(orderQueryService, never()).updateCancelReason(anyLong(), any());
  }

  @Test
  void applyOrderActionCancelsOnlyUnpaidSubOrders() {
    OrderMain mainOrder = buildMainOrder(100L);
    when(orderQueryService.requireAccessibleMainOrder(eq(100L), any())).thenReturn(mainOrder);
    when(orderService.listSubOrders(100L))
        .thenReturn(List.of(buildSubOrder(1L, "CREATED"), buildSubOrder(2L, "STOCK_RESERVED")));

    orderBatchService.applyOrderAction(
        100L, null, OrderAction.CANCEL, null, null, "user requested cancel");

    verify(orderService).advanceSubOrderStatus(1L, OrderAction.CANCEL);
    verify(orderService).advanceSubOrderStatus(2L, OrderAction.CANCEL);
    verify(orderQueryService).updateCancelReason(100L, "user requested cancel");
  }

  @Test
  void applyOrderActionRejectsCancellationWhenRemotePaymentAlreadyConfirmed() {
    OrderMain mainOrder = buildMainOrder(100L);
    OrderSub subOrder = buildSubOrder(1L, "STOCK_RESERVED");
    PaymentOrderVO paymentOrder = new PaymentOrderVO();
    paymentOrder.setStatus("PAID");

    when(orderQueryService.requireAccessibleMainOrder(eq(100L), any())).thenReturn(mainOrder);
    when(orderService.listSubOrders(100L)).thenReturn(List.of(subOrder));
    when(paymentOrderRemoteService.getPaymentOrderByOrderNo("M100", "S1")).thenReturn(paymentOrder);

    assertThrows(
        BizException.class,
        () -> orderBatchService.applyOrderAction(100L, null, OrderAction.CANCEL, null, null, null));

    verify(orderService, never()).advanceSubOrderStatus(anyLong(), eq(OrderAction.CANCEL));
    verify(orderQueryService, never()).updateCancelReason(anyLong(), any());
  }

  private OrderMain buildMainOrder(Long id) {
    OrderMain mainOrder = new OrderMain();
    mainOrder.setId(id);
    mainOrder.setMainOrderNo("M" + id);
    return mainOrder;
  }

  private OrderSub buildSubOrder(Long id, String status) {
    OrderSub subOrder = new OrderSub();
    subOrder.setId(id);
    subOrder.setSubOrderNo("S" + id);
    subOrder.setOrderStatus(status);
    return subOrder;
  }
}
