package com.cloud.order.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.common.domain.vo.payment.PaymentOrderVO;
import com.cloud.common.exception.RemoteException;
import com.cloud.common.exception.SystemException;
import com.cloud.order.entity.OrderMain;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.enums.OrderAction;
import com.cloud.order.mapper.OrderMainMapper;
import com.cloud.order.mapper.OrderSubMapper;
import com.cloud.order.service.OrderService;
import com.cloud.order.service.support.PaymentOrderRemoteService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrderTimeoutServiceImplTest {

  @Mock private OrderSubMapper orderSubMapper;
  @Mock private OrderMainMapper orderMainMapper;
  @Mock private OrderService orderService;
  @Mock private PaymentOrderRemoteService paymentOrderRemoteService;

  @InjectMocks private OrderTimeoutServiceImpl orderTimeoutService;

  @Test
  void getTimeoutSubOrderIdsUsesDedicatedMapperMethod() {
    ReflectionTestUtils.setField(orderTimeoutService, "timeoutMinutes", 30);
    ReflectionTestUtils.setField(orderTimeoutService, "timeoutBatchSize", 50);
    when(orderSubMapper.listTimeoutSubOrderIds(any(), any(), eq(50))).thenReturn(List.of(10L, 11L));

    List<Long> result = orderTimeoutService.getTimeoutSubOrderIds(null);

    assertTrue(result.containsAll(List.of(10L, 11L)));
    verify(orderSubMapper).listTimeoutSubOrderIds(any(), any(), eq(50));
    verify(orderSubMapper, never()).selectList(any());
  }

  @Test
  void cancelTimeoutOrderSkipsWhenRemotePaymentAlreadyConfirmed() {
    OrderSub subOrder = buildSubOrder(10L, 100L, "STOCK_RESERVED");
    OrderMain mainOrder = buildMainOrder(100L);
    PaymentOrderVO paymentOrder = new PaymentOrderVO();
    paymentOrder.setStatus("PAID");

    when(orderSubMapper.selectById(10L)).thenReturn(subOrder);
    when(orderMainMapper.selectById(100L)).thenReturn(mainOrder);
    when(paymentOrderRemoteService.getPaymentOrderByOrderNo("M100", "S10"))
        .thenReturn(paymentOrder);

    assertFalse(orderTimeoutService.cancelTimeoutOrder(10L));

    verify(orderService, never()).advanceSubOrderStatus(eq(10L), eq(OrderAction.CANCEL));
  }

  @Test
  void cancelTimeoutOrderCancelsWhenRemotePaymentIsNotConfirmed() {
    OrderSub subOrder = buildSubOrder(10L, 100L, "STOCK_RESERVED");
    OrderSub cancelledOrder = buildSubOrder(10L, 100L, "CANCELLED");
    OrderMain mainOrder = buildMainOrder(100L);

    when(orderSubMapper.selectById(10L)).thenReturn(subOrder);
    when(orderMainMapper.selectById(100L)).thenReturn(mainOrder);
    when(paymentOrderRemoteService.getPaymentOrderByOrderNo("M100", "S10")).thenReturn(null);
    when(orderService.advanceSubOrderStatus(10L, OrderAction.CANCEL)).thenReturn(cancelledOrder);
    when(orderSubMapper.countActiveByMainOrderIdAndStatuses(
            100L, List.of("CREATED", "STOCK_RESERVED", "PAID", "SHIPPED")))
        .thenReturn(0L);

    assertTrue(orderTimeoutService.cancelTimeoutOrder(10L));

    verify(orderService).advanceSubOrderStatus(10L, OrderAction.CANCEL);
    verify(orderMainMapper).updateById(mainOrder);
    verify(orderSubMapper)
        .countActiveByMainOrderIdAndStatuses(
            100L, List.of("CREATED", "STOCK_RESERVED", "PAID", "SHIPPED"));
  }

  @Test
  void cancelTimeoutOrderPropagatesRemoteExceptionForMqRetry() {
    OrderSub subOrder = buildSubOrder(10L, 100L, "STOCK_RESERVED");
    OrderMain mainOrder = buildMainOrder(100L);
    RemoteException remoteException =
        new RemoteException("payment unavailable", new RuntimeException());

    when(orderSubMapper.selectById(10L)).thenReturn(subOrder);
    when(orderMainMapper.selectById(100L)).thenReturn(mainOrder);
    when(paymentOrderRemoteService.getPaymentOrderByOrderNo("M100", "S10"))
        .thenThrow(remoteException);

    RemoteException thrown =
        assertThrows(RemoteException.class, () -> orderTimeoutService.cancelTimeoutOrder(10L));

    verify(orderService, never()).advanceSubOrderStatus(eq(10L), eq(OrderAction.CANCEL));
    org.junit.jupiter.api.Assertions.assertSame(remoteException, thrown);
  }

  @Test
  void cancelTimeoutOrderPropagatesSystemExceptionForMqRetry() {
    OrderSub subOrder = buildSubOrder(10L, 100L, "STOCK_RESERVED");
    OrderMain mainOrder = buildMainOrder(100L);
    SystemException systemException =
        new SystemException("cancel timeout order failed", new RuntimeException());

    when(orderSubMapper.selectById(10L)).thenReturn(subOrder);
    when(orderMainMapper.selectById(100L)).thenReturn(mainOrder);
    when(paymentOrderRemoteService.getPaymentOrderByOrderNo("M100", "S10")).thenReturn(null);
    when(orderService.advanceSubOrderStatus(10L, OrderAction.CANCEL)).thenThrow(systemException);

    SystemException thrown =
        assertThrows(SystemException.class, () -> orderTimeoutService.cancelTimeoutOrder(10L));

    verify(orderMainMapper, never()).updateById(any(OrderMain.class));
    org.junit.jupiter.api.Assertions.assertSame(systemException, thrown);
  }

  private OrderMain buildMainOrder(Long id) {
    OrderMain mainOrder = new OrderMain();
    mainOrder.setId(id);
    mainOrder.setMainOrderNo("M" + id);
    return mainOrder;
  }

  private OrderSub buildSubOrder(Long id, Long mainOrderId, String status) {
    OrderSub subOrder = new OrderSub();
    subOrder.setId(id);
    subOrder.setMainOrderId(mainOrderId);
    subOrder.setSubOrderNo("S" + id);
    subOrder.setOrderStatus(status);
    return subOrder;
  }
}
