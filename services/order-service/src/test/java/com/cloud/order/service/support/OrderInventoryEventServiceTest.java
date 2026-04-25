package com.cloud.order.service.support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.common.domain.dto.payment.PaymentRefundCommandDTO;
import com.cloud.common.domain.vo.payment.PaymentOrderVO;
import com.cloud.common.messaging.event.PaymentSuccessEvent;
import com.cloud.order.dto.OrderAggregateResponse;
import com.cloud.order.entity.AfterSale;
import com.cloud.order.entity.OrderMain;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.mapper.AfterSaleMapper;
import com.cloud.order.mapper.OrderMainMapper;
import com.cloud.order.mapper.OrderSubMapper;
import com.cloud.order.service.OrderService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderInventoryEventServiceTest {

  @Mock private OrderMainMapper orderMainMapper;
  @Mock private AfterSaleMapper afterSaleMapper;
  @Mock private OrderSubMapper orderSubMapper;
  @Mock private OrderService orderService;
  @Mock private com.cloud.order.messaging.OrderMessageProducer orderMessageProducer;
  @Mock private PaymentOrderRemoteService paymentOrderRemoteService;
  @Mock private OrderAggregateCacheService orderAggregateCacheService;

  @InjectMocks private OrderInventoryEventService orderInventoryEventService;

  @Test
  void handlePaymentSuccessCreatesAutoRefundForCancelledSubOrder() {
    OrderMain mainOrder = new OrderMain();
    mainOrder.setId(10L);
    mainOrder.setMainOrderNo("M100");
    mainOrder.setUserId(88L);
    when(orderMainMapper.selectActiveByOrderNo("M100")).thenReturn(mainOrder);

    OrderSub subOrder = new OrderSub();
    subOrder.setId(20L);
    subOrder.setMainOrderId(10L);
    subOrder.setMerchantId(99L);
    subOrder.setSubOrderNo("S200");
    subOrder.setOrderStatus("CANCELLED");
    subOrder.setPayableAmount(new BigDecimal("19.90"));

    OrderAggregateResponse.SubOrderWithItems wrapped =
        new OrderAggregateResponse.SubOrderWithItems();
    wrapped.setSubOrder(subOrder);
    wrapped.setItems(List.of());
    OrderAggregateResponse aggregate = new OrderAggregateResponse();
    aggregate.setMainOrder(mainOrder);
    aggregate.setSubOrders(List.of(wrapped));
    when(orderService.getOrderAggregate(10L)).thenReturn(aggregate);

    PaymentOrderVO paymentOrder = new PaymentOrderVO();
    paymentOrder.setId(345L);
    paymentOrder.setPaymentNo("PAY-S200-ABC");
    paymentOrder.setStatus("PAID");
    paymentOrder.setChannel("ALIPAY");
    paymentOrder.setAmount(new BigDecimal("19.90"));
    when(paymentOrderRemoteService.getPaymentOrderByOrderNo("M100", "S200"))
        .thenReturn(paymentOrder);
    when(paymentOrderRemoteService.createRefund(any(PaymentRefundCommandDTO.class)))
        .thenReturn(901L);
    when(afterSaleMapper.selectOne(any())).thenReturn(null);

    PaymentSuccessEvent event =
        PaymentSuccessEvent.builder()
            .paymentId(345L)
            .orderNo("M100")
            .subOrderNo("S200")
            .amount(new BigDecimal("19.90"))
            .paymentMethod("ALIPAY")
            .build();

    orderInventoryEventService.handlePaymentSuccess(event);

    ArgumentCaptor<AfterSale> afterSaleCaptor = ArgumentCaptor.forClass(AfterSale.class);
    verify(afterSaleMapper).insert(afterSaleCaptor.capture());
    AfterSale afterSale = afterSaleCaptor.getValue();
    Assertions.assertEquals("ASAUTO345", afterSale.getAfterSaleNo());
    Assertions.assertEquals("REFUND", afterSale.getAfterSaleType());
    Assertions.assertEquals("REFUNDING", afterSale.getStatus());
    Assertions.assertEquals(new BigDecimal("19.90"), afterSale.getApprovedAmount());

    ArgumentCaptor<PaymentRefundCommandDTO> refundCaptor =
        ArgumentCaptor.forClass(PaymentRefundCommandDTO.class);
    verify(paymentOrderRemoteService).createRefund(refundCaptor.capture());
    PaymentRefundCommandDTO refundCommand = refundCaptor.getValue();
    Assertions.assertEquals("RFASAUTO345", refundCommand.getRefundNo());
    Assertions.assertEquals("PAY-S200-ABC", refundCommand.getPaymentNo());
    Assertions.assertEquals("ASAUTO345", refundCommand.getAfterSaleNo());
    Assertions.assertEquals("auto-refund:ASAUTO345", refundCommand.getIdempotencyKey());

    verify(orderSubMapper).updateById(eq(subOrder));
    verify(orderAggregateCacheService).evict(10L);
    verify(orderService, never()).advanceSubOrderStatus(any(), any());
  }

  @Test
  void handleStockFreezeFailedClosesPaidSubOrderAndCreatesAutoRefund() {
    OrderMain mainOrder = new OrderMain();
    mainOrder.setId(10L);
    mainOrder.setMainOrderNo("M100");
    mainOrder.setUserId(88L);
    when(orderMainMapper.selectActiveByOrderNo("M100")).thenReturn(mainOrder);

    OrderSub subOrder = new OrderSub();
    subOrder.setId(20L);
    subOrder.setMainOrderId(10L);
    subOrder.setMerchantId(99L);
    subOrder.setSubOrderNo("S200");
    subOrder.setOrderStatus("PAID");
    subOrder.setPayableAmount(new BigDecimal("29.90"));
    when(orderService.listSubOrders(10L)).thenReturn(List.of(subOrder));

    PaymentOrderVO paymentOrder = new PaymentOrderVO();
    paymentOrder.setId(456L);
    paymentOrder.setPaymentNo("PAY-S200-PAID");
    paymentOrder.setStatus("PAID");
    paymentOrder.setChannel("ALIPAY");
    paymentOrder.setAmount(new BigDecimal("29.90"));
    when(paymentOrderRemoteService.getPaymentOrderByOrderNo("M100", "S200"))
        .thenReturn(paymentOrder);
    when(paymentOrderRemoteService.createRefund(any(PaymentRefundCommandDTO.class)))
        .thenReturn(902L);
    when(afterSaleMapper.selectOne(any())).thenReturn(null);

    orderInventoryEventService.handleStockFreezeFailed("M100");

    verify(orderService).advanceSubOrderStatus(20L, com.cloud.order.enums.OrderAction.CLOSE);
    verify(afterSaleMapper).insert(any(AfterSale.class));
    verify(paymentOrderRemoteService).createRefund(any(PaymentRefundCommandDTO.class));
  }

  @Test
  void handleStockFreezeFailedCancelsUnpaidCreatedSubOrderWithoutRefund() {
    OrderMain mainOrder = new OrderMain();
    mainOrder.setId(10L);
    mainOrder.setMainOrderNo("M100");
    when(orderMainMapper.selectActiveByOrderNo("M100")).thenReturn(mainOrder);

    OrderSub subOrder = new OrderSub();
    subOrder.setId(21L);
    subOrder.setMainOrderId(10L);
    subOrder.setSubOrderNo("S201");
    subOrder.setOrderStatus("CREATED");
    when(orderService.listSubOrders(10L)).thenReturn(List.of(subOrder));
    when(paymentOrderRemoteService.getPaymentOrderByOrderNo("M100", "S201")).thenReturn(null);

    orderInventoryEventService.handleStockFreezeFailed("M100");

    verify(orderService).advanceSubOrderStatus(21L, com.cloud.order.enums.OrderAction.CANCEL);
    verify(paymentOrderRemoteService, never()).createRefund(any(PaymentRefundCommandDTO.class));
  }

  @Test
  void handleStockFreezeFailedTriggersRefundForAlreadyCancelledPaidSubOrder() {
    OrderMain mainOrder = new OrderMain();
    mainOrder.setId(10L);
    mainOrder.setMainOrderNo("M100");
    mainOrder.setUserId(88L);
    when(orderMainMapper.selectActiveByOrderNo("M100")).thenReturn(mainOrder);

    OrderSub subOrder = new OrderSub();
    subOrder.setId(22L);
    subOrder.setMainOrderId(10L);
    subOrder.setMerchantId(99L);
    subOrder.setSubOrderNo("S202");
    subOrder.setOrderStatus("CANCELLED");
    subOrder.setPayableAmount(new BigDecimal("39.90"));
    when(orderService.listSubOrders(10L)).thenReturn(List.of(subOrder));

    PaymentOrderVO paymentOrder = new PaymentOrderVO();
    paymentOrder.setId(457L);
    paymentOrder.setPaymentNo("PAY-S202-CANCELLED");
    paymentOrder.setStatus("PAID");
    paymentOrder.setChannel("ALIPAY");
    paymentOrder.setAmount(new BigDecimal("39.90"));
    when(paymentOrderRemoteService.getPaymentOrderByOrderNo("M100", "S202"))
        .thenReturn(paymentOrder);
    when(paymentOrderRemoteService.createRefund(any(PaymentRefundCommandDTO.class)))
        .thenReturn(903L);
    when(afterSaleMapper.selectOne(any())).thenReturn(null);

    orderInventoryEventService.handleStockFreezeFailed("M100");

    verify(orderService, never())
        .advanceSubOrderStatus(any(), eq(com.cloud.order.enums.OrderAction.CANCEL));
    verify(orderService, never())
        .advanceSubOrderStatus(any(), eq(com.cloud.order.enums.OrderAction.CLOSE));
    verify(paymentOrderRemoteService).createRefund(any(PaymentRefundCommandDTO.class));
  }
}
