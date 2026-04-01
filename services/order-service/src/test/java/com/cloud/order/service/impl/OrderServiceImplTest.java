package com.cloud.order.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.common.metrics.TradeMetrics;
import com.cloud.order.dto.CreateMainOrderRequest;
import com.cloud.order.entity.AfterSale;
import com.cloud.order.entity.OrderItem;
import com.cloud.order.entity.OrderMain;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.enums.OrderAction;
import com.cloud.order.mapper.AfterSaleMapper;
import com.cloud.order.mapper.OrderItemMapper;
import com.cloud.order.mapper.OrderMainMapper;
import com.cloud.order.mapper.OrderSubMapper;
import com.cloud.order.messaging.OrderAutoReceiveMessageProducer;
import com.cloud.order.messaging.OrderMessageProducer;
import com.cloud.order.messaging.OrderShippedMessageProducer;
import com.cloud.order.service.support.OrderAggregateCacheService;
import com.cloud.order.service.support.OrderRefundSagaCoordinator;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

  @Mock private OrderMainMapper orderMainMapper;

  @Mock private OrderSubMapper orderSubMapper;

  @Mock private OrderItemMapper orderItemMapper;

  @Mock private AfterSaleMapper afterSaleMapper;

  @Mock private OrderRefundSagaCoordinator orderRefundSagaCoordinator;

  @Mock private ObjectProvider<OrderRefundSagaCoordinator> orderRefundSagaCoordinatorProvider;

  @Mock private TradeMetrics tradeMetrics;

  @Mock private OrderAggregateCacheService orderAggregateCacheService;

  @Mock private OrderShippedMessageProducer orderShippedMessageProducer;

  @Mock private OrderAutoReceiveMessageProducer orderAutoReceiveMessageProducer;

  @Mock private OrderMessageProducer orderMessageProducer;

  private OrderServiceImpl orderService;

  @BeforeEach
  void setUp() {
    orderService =
        new OrderServiceImpl(
            orderMainMapper,
            orderSubMapper,
            orderItemMapper,
            afterSaleMapper,
            orderRefundSagaCoordinatorProvider,
            tradeMetrics,
            orderAggregateCacheService,
            orderShippedMessageProducer,
            orderAutoReceiveMessageProducer,
            orderMessageProducer);
  }

  @Test
  void createMainOrderShouldReturnExistingOnIdempotencyHit() {
    CreateMainOrderRequest request = buildRequest();
    OrderMain existing = new OrderMain();
    existing.setId(99L);
    existing.setMainOrderNo("M-existing");

    when(orderMainMapper.selectActiveByIdempotencyKey(any())).thenReturn(existing);

    OrderMain result = orderService.createMainOrder(request);

    assertThat(result).isSameAs(existing);
    verify(orderMainMapper, never()).insert(any(OrderMain.class));
    verify(orderSubMapper, never()).insert(any(OrderSub.class));
    verify(orderItemMapper, never()).insert(any(OrderItem.class));
    verify(tradeMetrics).incrementOrder("success");
  }

  @Test
  void createMainOrderShouldInsertSubOrdersAndItems() {
    CreateMainOrderRequest request = buildRequest();

    when(orderMainMapper.selectActiveByIdempotencyKey(any())).thenReturn(null);
    doAnswer(
            invocation -> {
              OrderMain main = invocation.getArgument(0);
              main.setId(101L);
              return 1;
            })
        .when(orderMainMapper)
        .insert(any(OrderMain.class));

    OrderMain created = orderService.createMainOrder(request);

    assertThat(created.getId()).isEqualTo(101L);
    verify(orderSubMapper).insert(any(OrderSub.class));
    verify(orderItemMapper).insert(any(OrderItem.class));
    verify(tradeMetrics).incrementOrder("success");
  }

  @Test
  void advanceSubOrderStatusShouldRejectMissingLogisticsInfo() {
    OrderSub subOrder = new OrderSub();
    subOrder.setId(12L);
    subOrder.setOrderStatus("PAID");
    subOrder.setShippingCompany(null);
    subOrder.setTrackingNumber(null);

    when(orderSubMapper.selectById(12L)).thenReturn(subOrder);

    assertThatThrownBy(() -> orderService.advanceSubOrderStatus(12L, OrderAction.SHIP))
        .isInstanceOf(BizException.class)
        .hasMessageContaining("shipping company and tracking number are required");
  }

  @Test
  void advanceSubOrderStatusCancelShouldReleaseReservedStock() {
    OrderSub subOrder = new OrderSub();
    subOrder.setId(13L);
    subOrder.setSubOrderNo("S-13");
    subOrder.setOrderStatus("STOCK_RESERVED");
    subOrder.setMainOrderId(21L);

    OrderItem item1 = new OrderItem();
    item1.setSkuId(1001L);
    item1.setQuantity(2);
    OrderItem item2 = new OrderItem();
    item2.setSkuId(1001L);
    item2.setQuantity(3);
    OrderItem item3 = new OrderItem();
    item3.setSkuId(1002L);
    item3.setQuantity(1);

    OrderMain mainOrder = new OrderMain();
    mainOrder.setId(21L);
    mainOrder.setMainOrderNo("M-21");
    mainOrder.setDeleted(0);

    when(orderSubMapper.selectById(13L)).thenReturn(subOrder);
    when(orderItemMapper.listActiveBySubOrderId(13L)).thenReturn(List.of(item1, item2, item3));
    when(orderMainMapper.selectById(21L)).thenReturn(mainOrder);
    when(orderMessageProducer.sendStockReleaseRequestEvent(any())).thenReturn(true);

    orderService.advanceSubOrderStatus(13L, OrderAction.CANCEL);

    ArgumentCaptor<com.cloud.common.messaging.event.StockReleaseRequestEvent> captor =
        ArgumentCaptor.forClass(com.cloud.common.messaging.event.StockReleaseRequestEvent.class);
    verify(orderMessageProducer).sendStockReleaseRequestEvent(captor.capture());

    Map<Long, Integer> quantities =
        captor.getValue().getItems().stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    StockOperateCommandDTO::getSkuId, StockOperateCommandDTO::getQuantity));
    assertThat(quantities).containsEntry(1001L, 5).containsEntry(1002L, 1);
    verify(orderSubMapper).updateById(subOrder);
  }

  @Test
  void advanceSubOrderStatusShipShouldSendEvents() {
    OrderSub subOrder = new OrderSub();
    subOrder.setId(14L);
    subOrder.setSubOrderNo("S-14");
    subOrder.setOrderStatus("PAID");
    subOrder.setMainOrderId(31L);
    subOrder.setShippingCompany("SF");
    subOrder.setTrackingNumber("T-100");

    OrderMain mainOrder = new OrderMain();
    mainOrder.setId(31L);
    mainOrder.setMainOrderNo("M-31");
    mainOrder.setUserId(9L);
    mainOrder.setDeleted(0);

    when(orderSubMapper.selectById(14L)).thenReturn(subOrder);
    when(orderMainMapper.selectById(31L)).thenReturn(mainOrder);
    when(orderSubMapper.listActiveByMainOrderId(31L)).thenReturn(List.of(subOrder));

    OrderSub updated = orderService.advanceSubOrderStatus(14L, OrderAction.SHIP);

    assertThat(updated.getOrderStatus()).isEqualTo("SHIPPED");
    assertThat(updated.getShippedAt()).isNotNull();
    assertThat(updated.getEstimatedArrival()).isNotNull();
    verify(orderShippedMessageProducer).sendAfterCommit(any());
    verify(orderAutoReceiveMessageProducer).sendAfterCommit(any());
    verify(orderAggregateCacheService).evict(31L);
  }

  @Test
  void applyAfterSaleShouldRejectMismatchedSubOrderRelationship() {
    AfterSale afterSale = buildAfterSale();
    OrderMain mainOrder = buildMainOrder(50L, 9L);
    OrderSub subOrder = buildSubOrder(60L, 999L, 88L, "PAID", BigDecimal.valueOf(40));

    when(orderMainMapper.selectById(50L)).thenReturn(mainOrder);
    when(orderSubMapper.selectById(60L)).thenReturn(subOrder);

    assertThatThrownBy(() -> orderService.applyAfterSale(afterSale))
        .isInstanceOf(BizException.class)
        .satisfies(
            ex ->
                assertThat(((BizException) ex).getCode())
                    .isEqualTo(ResultCode.BAD_REQUEST.getCode()))
        .hasMessageContaining("sub order does not belong to main order");

    verify(afterSaleMapper, never()).insert(any(AfterSale.class));
  }

  @Test
  void applyAfterSaleShouldRejectOrderOwnerMismatch() {
    AfterSale afterSale = buildAfterSale();
    afterSale.setUserId(1000L);
    OrderMain mainOrder = buildMainOrder(50L, 9L);
    OrderSub subOrder = buildSubOrder(60L, 50L, 88L, "PAID", BigDecimal.valueOf(40));

    when(orderMainMapper.selectById(50L)).thenReturn(mainOrder);
    when(orderSubMapper.selectById(60L)).thenReturn(subOrder);

    assertThatThrownBy(() -> orderService.applyAfterSale(afterSale))
        .isInstanceOf(BizException.class)
        .satisfies(
            ex ->
                assertThat(((BizException) ex).getCode()).isEqualTo(ResultCode.FORBIDDEN.getCode()))
        .hasMessageContaining("after sale user does not match the order owner");

    verify(afterSaleMapper, never()).insert(any(AfterSale.class));
  }

  @Test
  void applyAfterSaleShouldRejectMerchantMismatch() {
    AfterSale afterSale = buildAfterSale();
    afterSale.setMerchantId(999L);
    OrderMain mainOrder = buildMainOrder(50L, 9L);
    OrderSub subOrder = buildSubOrder(60L, 50L, 88L, "PAID", BigDecimal.valueOf(40));

    when(orderMainMapper.selectById(50L)).thenReturn(mainOrder);
    when(orderSubMapper.selectById(60L)).thenReturn(subOrder);

    assertThatThrownBy(() -> orderService.applyAfterSale(afterSale))
        .isInstanceOf(BizException.class)
        .satisfies(
            ex ->
                assertThat(((BizException) ex).getCode()).isEqualTo(ResultCode.FORBIDDEN.getCode()))
        .hasMessageContaining("after sale merchant does not match the sub order merchant");

    verify(afterSaleMapper, never()).insert(any(AfterSale.class));
  }

  @Test
  void applyAfterSaleShouldRejectAmountExceedingPayableAmount() {
    AfterSale afterSale = buildAfterSale();
    afterSale.setApplyAmount(BigDecimal.valueOf(60));
    OrderMain mainOrder = buildMainOrder(50L, 9L);
    OrderSub subOrder = buildSubOrder(60L, 50L, 88L, "PAID", BigDecimal.valueOf(40));

    when(orderMainMapper.selectById(50L)).thenReturn(mainOrder);
    when(orderSubMapper.selectById(60L)).thenReturn(subOrder);

    assertThatThrownBy(() -> orderService.applyAfterSale(afterSale))
        .isInstanceOf(BizException.class)
        .satisfies(
            ex ->
                assertThat(((BizException) ex).getCode())
                    .isEqualTo(ResultCode.BAD_REQUEST.getCode()))
        .hasMessageContaining("apply amount cannot exceed sub order payable amount");

    verify(afterSaleMapper, never()).insert(any(AfterSale.class));
  }

  @Test
  void applyAfterSaleShouldRejectIneligibleSubOrderStatus() {
    AfterSale afterSale = buildAfterSale();
    OrderMain mainOrder = buildMainOrder(50L, 9L);
    OrderSub subOrder = buildSubOrder(60L, 50L, 88L, "STOCK_RESERVED", BigDecimal.valueOf(40));

    when(orderMainMapper.selectById(50L)).thenReturn(mainOrder);
    when(orderSubMapper.selectById(60L)).thenReturn(subOrder);

    assertThatThrownBy(() -> orderService.applyAfterSale(afterSale))
        .isInstanceOf(BizException.class)
        .satisfies(
            ex ->
                assertThat(((BizException) ex).getCode())
                    .isEqualTo(ResultCode.BAD_REQUEST.getCode()))
        .hasMessageContaining("after sale is not allowed for sub order status");

    verify(afterSaleMapper, never()).insert(any(AfterSale.class));
  }

  @Test
  void applyAfterSaleShouldRejectActiveAfterSaleStatus() {
    AfterSale afterSale = buildAfterSale();
    OrderMain mainOrder = buildMainOrder(50L, 9L);
    OrderSub subOrder = buildSubOrder(60L, 50L, 88L, "PAID", BigDecimal.valueOf(40));
    subOrder.setAfterSaleStatus("APPLIED");

    when(orderMainMapper.selectById(50L)).thenReturn(mainOrder);
    when(orderSubMapper.selectById(60L)).thenReturn(subOrder);

    assertThatThrownBy(() -> orderService.applyAfterSale(afterSale))
        .isInstanceOf(BizException.class)
        .satisfies(
            ex ->
                assertThat(((BizException) ex).getCode())
                    .isEqualTo(ResultCode.BAD_REQUEST.getCode()))
        .hasMessageContaining("active after-sale request already exists");

    verify(afterSaleMapper, never()).insert(any(AfterSale.class));
  }

  @Test
  void applyAfterSaleShouldAllowReapplyAfterTerminalStatus() {
    AfterSale afterSale = buildAfterSale();
    OrderMain mainOrder = buildMainOrder(50L, 9L);
    OrderSub subOrder = buildSubOrder(60L, 50L, 88L, "PAID", BigDecimal.valueOf(40));
    subOrder.setAfterSaleStatus("REJECTED");

    when(orderMainMapper.selectById(50L)).thenReturn(mainOrder);
    when(orderSubMapper.selectById(60L)).thenReturn(subOrder);

    AfterSale created = orderService.applyAfterSale(afterSale);

    assertThat(created.getStatus()).isEqualTo("APPLIED");
    verify(afterSaleMapper).insert(afterSale);
  }

  @Test
  void applyAfterSaleShouldCanonicalizeProtectedFields() {
    AfterSale afterSale = buildAfterSale();
    afterSale.setStatus("APPROVED");
    afterSale.setMerchantId(null);
    afterSale.setApprovedAmount(BigDecimal.valueOf(20));
    OrderMain mainOrder = buildMainOrder(50L, 9L);
    OrderSub subOrder = buildSubOrder(60L, 50L, 88L, "PAID", BigDecimal.valueOf(40));

    when(orderMainMapper.selectById(50L)).thenReturn(mainOrder);
    when(orderSubMapper.selectById(60L)).thenReturn(subOrder);

    AfterSale created = orderService.applyAfterSale(afterSale);

    assertThat(created.getStatus()).isEqualTo("APPLIED");
    assertThat(created.getMerchantId()).isEqualTo(88L);
    assertThat(created.getUserId()).isEqualTo(9L);
    assertThat(created.getApprovedAmount()).isNull();
    assertThat(created.getAfterSaleNo()).startsWith("AS");
    verify(afterSaleMapper).insert(afterSale);
    verify(orderAggregateCacheService).evict(50L);
  }

  private CreateMainOrderRequest buildRequest() {
    CreateMainOrderRequest request = new CreateMainOrderRequest();
    request.setUserId(1L);
    request.setIdempotencyKey("key-1");
    request.setTotalAmount(BigDecimal.valueOf(100));
    request.setPayableAmount(BigDecimal.valueOf(100));
    request.setCartId(10L);

    CreateMainOrderRequest.CreateOrderItemRequest item =
        new CreateMainOrderRequest.CreateOrderItemRequest();
    item.setSpuId(1L);
    item.setSkuId(2L);
    item.setQuantity(1);
    item.setUnitPrice(BigDecimal.valueOf(100));
    item.setTotalPrice(BigDecimal.valueOf(100));

    CreateMainOrderRequest.CreateSubOrderRequest sub =
        new CreateMainOrderRequest.CreateSubOrderRequest();
    sub.setMerchantId(101L);
    sub.setItemAmount(BigDecimal.valueOf(100));
    sub.setShippingFee(BigDecimal.ZERO);
    sub.setDiscountAmount(BigDecimal.ZERO);
    sub.setPayableAmount(BigDecimal.valueOf(100));
    sub.setReceiverName("name");
    sub.setReceiverPhone("13800000000");
    sub.setReceiverAddress("address");
    sub.setItems(List.of(item));

    request.setSubOrders(List.of(sub));
    return request;
  }

  private AfterSale buildAfterSale() {
    AfterSale afterSale = new AfterSale();
    afterSale.setMainOrderId(50L);
    afterSale.setSubOrderId(60L);
    afterSale.setUserId(9L);
    afterSale.setMerchantId(88L);
    afterSale.setApplyAmount(BigDecimal.valueOf(30));
    afterSale.setAfterSaleType("REFUND");
    afterSale.setReason("damaged");
    return afterSale;
  }

  private OrderMain buildMainOrder(Long mainOrderId, Long userId) {
    OrderMain mainOrder = new OrderMain();
    mainOrder.setId(mainOrderId);
    mainOrder.setUserId(userId);
    mainOrder.setDeleted(0);
    return mainOrder;
  }

  private OrderSub buildSubOrder(
      Long subOrderId, Long mainOrderId, Long merchantId, String status, BigDecimal payableAmount) {
    OrderSub subOrder = new OrderSub();
    subOrder.setId(subOrderId);
    subOrder.setMainOrderId(mainOrderId);
    subOrder.setMerchantId(merchantId);
    subOrder.setOrderStatus(status);
    subOrder.setPayableAmount(payableAmount);
    subOrder.setDeleted(0);
    return subOrder;
  }
}
