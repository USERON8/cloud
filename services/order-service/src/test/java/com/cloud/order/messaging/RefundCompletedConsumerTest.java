package com.cloud.order.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import com.cloud.common.messaging.event.RefundCompletedEvent;
import com.cloud.common.messaging.event.StockRestoreEvent;
import com.cloud.common.metrics.TradeMetrics;
import com.cloud.order.entity.AfterSale;
import com.cloud.order.entity.AfterSaleItem;
import com.cloud.order.entity.OrderItem;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.enums.AfterSaleAction;
import com.cloud.order.mapper.AfterSaleItemMapper;
import com.cloud.order.mapper.AfterSaleMapper;
import com.cloud.order.mapper.OrderItemMapper;
import com.cloud.order.mapper.OrderSubMapper;
import com.cloud.order.service.OrderService;
import java.math.BigDecimal;
import java.util.List;
import org.apache.rocketmq.common.message.MessageExt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefundCompletedConsumerTest {

  @Mock private AfterSaleMapper afterSaleMapper;

  @Mock private AfterSaleItemMapper afterSaleItemMapper;

  @Mock private OrderSubMapper orderSubMapper;

  @Mock private OrderItemMapper orderItemMapper;

  @Mock private OrderService orderService;

  @Mock private OrderMessageProducer orderMessageProducer;

  @Mock private TradeMetrics tradeMetrics;

  private RefundCompletedConsumer consumer;

  @BeforeEach
  void setUp() {
    consumer =
        new RefundCompletedConsumer(
            afterSaleMapper,
            afterSaleItemMapper,
            orderSubMapper,
            orderItemMapper,
            orderService,
            orderMessageProducer,
            tradeMetrics);
  }

  @Test
  void refundOnlyShouldNotRestoreStock() {
    AfterSale afterSale = buildAfterSale("REFUND", BigDecimal.valueOf(30), null);
    afterSale.setStatus("REFUNDING");
    RefundCompletedEvent event = buildEvent(null);

    when(afterSaleMapper.selectOne(any())).thenReturn(afterSale);

    consumer.doConsume(event, new MessageExt());

    verify(orderService)
        .advanceAfterSaleStatus(afterSale.getId(), AfterSaleAction.REFUND, "refund completed");
    verify(orderMessageProducer, never()).sendStockRestoreEvent(any());
    verify(tradeMetrics).incrementMessageConsume("refund_completed", "success");
  }

  @Test
  void returnRefundShouldPreferEventItems() {
    AfterSale afterSale = buildAfterSale("RETURN_REFUND", BigDecimal.valueOf(30), null);
    OrderSub subOrder = buildSubOrder(BigDecimal.valueOf(100));
    StockOperateCommandDTO eventItem = new StockOperateCommandDTO();
    eventItem.setSkuId(9001L);
    eventItem.setQuantity(2);
    RefundCompletedEvent event = buildEvent(List.of(eventItem));

    when(afterSaleMapper.selectOne(any())).thenReturn(afterSale);
    when(orderSubMapper.selectById(55L)).thenReturn(subOrder);
    when(orderMessageProducer.sendStockRestoreEvent(any())).thenReturn(true);

    consumer.doConsume(event, new MessageExt());

    ArgumentCaptor<StockRestoreEvent> captor = ArgumentCaptor.forClass(StockRestoreEvent.class);
    verify(orderMessageProducer).sendStockRestoreEvent(captor.capture());
    StockRestoreEvent restoreEvent = captor.getValue();
    assertThat(restoreEvent.getItems()).hasSize(1);
    assertThat(restoreEvent.getItems().get(0).getSkuId()).isEqualTo(9001L);
    assertThat(restoreEvent.getItems().get(0).getQuantity()).isEqualTo(2);
    assertThat(restoreEvent.getItems().get(0).getSubOrderNo()).isEqualTo("S-55");
    assertThat(restoreEvent.getItems().get(0).getOrderNo()).isEqualTo("M-10");
    verify(afterSaleItemMapper, never()).listActiveByAfterSaleId(any());
    verify(orderItemMapper, never()).listActiveBySubOrderId(any());
  }

  @Test
  void returnRefundShouldUseAfterSaleItemsBeforeFullOrderFallback() {
    AfterSale afterSale = buildAfterSale("RETURN_REFUND", BigDecimal.valueOf(30), null);
    OrderSub subOrder = buildSubOrder(BigDecimal.valueOf(100));
    AfterSaleItem item1 = new AfterSaleItem();
    item1.setSkuId(7001L);
    item1.setQuantity(1);
    AfterSaleItem item2 = new AfterSaleItem();
    item2.setSkuId(7001L);
    item2.setQuantity(2);
    RefundCompletedEvent event = buildEvent(null);

    when(afterSaleMapper.selectOne(any())).thenReturn(afterSale);
    when(orderSubMapper.selectById(55L)).thenReturn(subOrder);
    when(afterSaleItemMapper.listActiveByAfterSaleId(88L)).thenReturn(List.of(item1, item2));
    when(orderMessageProducer.sendStockRestoreEvent(any())).thenReturn(true);

    consumer.doConsume(event, new MessageExt());

    ArgumentCaptor<StockRestoreEvent> captor = ArgumentCaptor.forClass(StockRestoreEvent.class);
    verify(orderMessageProducer).sendStockRestoreEvent(captor.capture());
    assertThat(captor.getValue().getItems()).hasSize(1);
    assertThat(captor.getValue().getItems().get(0).getSkuId()).isEqualTo(7001L);
    assertThat(captor.getValue().getItems().get(0).getQuantity()).isEqualTo(3);
    verify(orderItemMapper, never()).listActiveBySubOrderId(any());
  }

  @Test
  void returnRefundShouldNotFallbackToWholeOrderForPartialAmount() {
    AfterSale afterSale = buildAfterSale("RETURN_REFUND", BigDecimal.valueOf(30), null);
    OrderSub subOrder = buildSubOrder(BigDecimal.valueOf(100));
    RefundCompletedEvent event = buildEvent(null);

    when(afterSaleMapper.selectOne(any())).thenReturn(afterSale);
    when(orderSubMapper.selectById(55L)).thenReturn(subOrder);
    when(afterSaleItemMapper.listActiveByAfterSaleId(88L)).thenReturn(List.of());

    consumer.doConsume(event, new MessageExt());

    verify(orderMessageProducer, never()).sendStockRestoreEvent(any());
    verify(orderItemMapper, never()).listActiveBySubOrderId(any());
  }

  @Test
  void returnRefundShouldFallbackToWholeOrderWhenAmountCoversEntireSubOrder() {
    AfterSale afterSale = buildAfterSale("RETURN_REFUND", BigDecimal.valueOf(120), null);
    OrderSub subOrder = buildSubOrder(BigDecimal.valueOf(100));
    OrderItem orderItem1 = new OrderItem();
    orderItem1.setSkuId(8001L);
    orderItem1.setQuantity(1);
    OrderItem orderItem2 = new OrderItem();
    orderItem2.setSkuId(8002L);
    orderItem2.setQuantity(2);
    RefundCompletedEvent event = buildEvent(null);

    when(afterSaleMapper.selectOne(any())).thenReturn(afterSale);
    when(orderSubMapper.selectById(55L)).thenReturn(subOrder);
    when(afterSaleItemMapper.listActiveByAfterSaleId(88L)).thenReturn(List.of());
    when(orderItemMapper.listActiveBySubOrderId(55L)).thenReturn(List.of(orderItem1, orderItem2));
    when(orderMessageProducer.sendStockRestoreEvent(any())).thenReturn(true);

    consumer.doConsume(event, new MessageExt());

    ArgumentCaptor<StockRestoreEvent> captor = ArgumentCaptor.forClass(StockRestoreEvent.class);
    verify(orderMessageProducer).sendStockRestoreEvent(captor.capture());
    assertThat(captor.getValue().getItems()).hasSize(2);
    verify(orderItemMapper).listActiveBySubOrderId(55L);
  }

  private AfterSale buildAfterSale(
      String afterSaleType, BigDecimal applyAmount, BigDecimal approvedAmount) {
    AfterSale afterSale = new AfterSale();
    afterSale.setId(88L);
    afterSale.setAfterSaleNo("AS-88");
    afterSale.setSubOrderId(55L);
    afterSale.setAfterSaleType(afterSaleType);
    afterSale.setApplyAmount(applyAmount);
    afterSale.setApprovedAmount(approvedAmount);
    afterSale.setStatus("REFUNDED");
    afterSale.setDeleted(0);
    return afterSale;
  }

  private OrderSub buildSubOrder(BigDecimal payableAmount) {
    OrderSub subOrder = new OrderSub();
    subOrder.setId(55L);
    subOrder.setSubOrderNo("S-55");
    subOrder.setPayableAmount(payableAmount);
    subOrder.setDeleted(0);
    return subOrder;
  }

  private RefundCompletedEvent buildEvent(List<StockOperateCommandDTO> items) {
    return RefundCompletedEvent.builder()
        .refundNo("RF-10")
        .afterSaleNo("AS-88")
        .mainOrderNo("M-10")
        .subOrderNo("S-55")
        .items(items)
        .build();
  }
}
