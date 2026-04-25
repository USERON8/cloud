package com.cloud.order.service.support;

import com.cloud.common.domain.dto.payment.PaymentRefundCommandDTO;
import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import com.cloud.common.domain.vo.payment.PaymentOrderVO;
import com.cloud.common.exception.SystemException;
import com.cloud.common.messaging.event.PaymentSuccessEvent;
import com.cloud.common.messaging.event.StockConfirmRequestEvent;
import com.cloud.order.dto.OrderAggregateResponse;
import com.cloud.order.entity.AfterSale;
import com.cloud.order.entity.OrderItem;
import com.cloud.order.entity.OrderMain;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.enums.OrderAction;
import com.cloud.order.mapper.AfterSaleMapper;
import com.cloud.order.mapper.OrderMainMapper;
import com.cloud.order.mapper.OrderSubMapper;
import com.cloud.order.service.OrderService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderInventoryEventService {

  private static final String STATUS_CREATED = "CREATED";
  private static final String STATUS_STOCK_RESERVED = "STOCK_RESERVED";
  private static final String STATUS_PAID = "PAID";
  private static final String STATUS_SHIPPED = "SHIPPED";
  private static final String STATUS_DONE = "DONE";
  private static final String STATUS_CANCELLED = "CANCELLED";
  private static final String STATUS_CLOSED = "CLOSED";
  private static final String STATUS_REFUNDING = "REFUNDING";
  private static final String STATUS_REFUNDED = "REFUNDED";
  private static final String PAYMENT_STATUS_PAID = "PAID";
  private static final String AUTO_REFUND_REASON_CANCELLED = "auto refund for canceled paid order";
  private static final String AUTO_REFUND_DESCRIPTION_CANCELLED =
      "payment succeeded after order cancellation";
  private static final String AUTO_REFUND_REASON_STOCK_FAILURE =
      "auto refund for stock freeze failure";
  private static final String AUTO_REFUND_DESCRIPTION_STOCK_FAILURE =
      "stock freeze failed after payment confirmation";
  private static final String AUTO_AFTER_SALE_PREFIX = "ASAUTO";
  private static final String AUTO_REFUND_IDEMPOTENCY_PREFIX = "auto-refund:";

  private final OrderMainMapper orderMainMapper;
  private final AfterSaleMapper afterSaleMapper;
  private final OrderSubMapper orderSubMapper;
  private final OrderService orderService;
  private final com.cloud.order.messaging.OrderMessageProducer orderMessageProducer;
  private final PaymentOrderRemoteService paymentOrderRemoteService;
  private final OrderAggregateCacheService orderAggregateCacheService;

  @Transactional(rollbackFor = Exception.class)
  public void handlePaymentSuccess(PaymentSuccessEvent event) {
    if (event == null || event.getOrderNo() == null || event.getOrderNo().isBlank()) {
      return;
    }
    OrderMain mainOrder = orderMainMapper.selectActiveByOrderNo(event.getOrderNo());
    if (mainOrder == null) {
      return;
    }
    OrderAggregateResponse aggregate = orderService.getOrderAggregate(mainOrder.getId());
    if (aggregate == null || aggregate.getSubOrders() == null) {
      return;
    }
    for (OrderAggregateResponse.SubOrderWithItems wrapped : aggregate.getSubOrders()) {
      OrderSub subOrder = wrapped.getSubOrder();
      if (subOrder == null || !matchesTargetSubOrder(event.getSubOrderNo(), subOrder)) {
        continue;
      }
      String previousStatus = subOrder.getOrderStatus();
      if (STATUS_CANCELLED.equals(previousStatus) || STATUS_CLOSED.equals(previousStatus)) {
        triggerAutoRefundForCancelledOrder(mainOrder, subOrder, event);
        continue;
      }
      if (!STATUS_CREATED.equals(previousStatus) && !STATUS_STOCK_RESERVED.equals(previousStatus)) {
        continue;
      }
      orderService.advanceSubOrderStatus(subOrder.getId(), OrderAction.PAY);
      if (STATUS_STOCK_RESERVED.equals(previousStatus)) {
        sendConfirmRequest(mainOrder.getMainOrderNo(), subOrder, wrapped.getItems());
      }
    }
  }

  @Transactional(rollbackFor = Exception.class)
  public void handleStockReserved(String orderNo) {
    if (orderNo == null || orderNo.isBlank()) {
      return;
    }
    OrderMain mainOrder = orderMainMapper.selectActiveByOrderNo(orderNo);
    if (mainOrder == null) {
      return;
    }
    OrderAggregateResponse aggregate = orderService.getOrderAggregate(mainOrder.getId());
    if (aggregate == null || aggregate.getSubOrders() == null) {
      return;
    }
    for (OrderAggregateResponse.SubOrderWithItems wrapped : aggregate.getSubOrders()) {
      OrderSub subOrder = wrapped.getSubOrder();
      if (subOrder == null) {
        continue;
      }
      if (STATUS_CREATED.equals(subOrder.getOrderStatus())) {
        orderService.advanceSubOrderStatus(subOrder.getId(), OrderAction.RESERVE);
        continue;
      }
      if (STATUS_PAID.equals(subOrder.getOrderStatus())) {
        sendConfirmRequest(mainOrder.getMainOrderNo(), subOrder, wrapped.getItems());
      }
    }
  }

  @Transactional(rollbackFor = Exception.class)
  public void handleStockFreezeFailed(String orderNo) {
    if (orderNo == null || orderNo.isBlank()) {
      return;
    }
    OrderMain mainOrder = orderMainMapper.selectActiveByOrderNo(orderNo);
    if (mainOrder == null) {
      return;
    }
    for (OrderSub subOrder : orderService.listSubOrders(mainOrder.getId())) {
      handleSubOrderStockFreezeFailed(mainOrder, subOrder);
    }
  }

  private void handleSubOrderStockFreezeFailed(OrderMain mainOrder, OrderSub subOrder) {
    if (subOrder == null) {
      return;
    }
    String status = subOrder.getOrderStatus();
    if (STATUS_SHIPPED.equals(status) || STATUS_DONE.equals(status)) {
      return;
    }

    PaymentOrderVO paidPaymentOrder = findPaidPaymentOrder(mainOrder, subOrder);
    if (paidPaymentOrder != null) {
      if (!STATUS_CANCELLED.equals(status) && !STATUS_CLOSED.equals(status)) {
        orderService.advanceSubOrderStatus(subOrder.getId(), OrderAction.CLOSE);
      }
      triggerAutoRefundForStockFreezeFailed(mainOrder, subOrder, paidPaymentOrder);
      return;
    }

    if (STATUS_PAID.equals(status)) {
      throw new SystemException(
          "paid payment order is missing for stock freeze compensation: subOrderNo="
              + subOrder.getSubOrderNo());
    }
    if (STATUS_CANCELLED.equals(status) || STATUS_CLOSED.equals(status)) {
      return;
    }
    orderService.advanceSubOrderStatus(subOrder.getId(), OrderAction.CANCEL);
  }

  private void sendConfirmRequest(String orderNo, OrderSub subOrder, List<OrderItem> items) {
    List<StockOperateCommandDTO> commands = buildCommands(orderNo, subOrder, items, "confirm");
    if (commands.isEmpty()) {
      return;
    }
    StockConfirmRequestEvent event =
        StockConfirmRequestEvent.builder()
            .orderNo(orderNo)
            .subOrderNo(subOrder.getSubOrderNo())
            .items(commands)
            .build();
    if (!orderMessageProducer.sendStockConfirmRequestEvent(event)) {
      throw new IllegalStateException(
          "failed to enqueue stock confirm request for subOrderNo=" + subOrder.getSubOrderNo());
    }
  }

  private List<StockOperateCommandDTO> buildCommands(
      String orderNo, OrderSub subOrder, List<OrderItem> items, String action) {
    if (items == null || items.isEmpty() || subOrder == null) {
      return List.of();
    }
    Map<Long, Integer> skuQuantities = new LinkedHashMap<>();
    for (OrderItem item : items) {
      if (item == null || item.getSkuId() == null || item.getQuantity() == null) {
        continue;
      }
      skuQuantities.merge(item.getSkuId(), item.getQuantity(), Integer::sum);
    }
    List<StockOperateCommandDTO> commands = new ArrayList<>(skuQuantities.size());
    for (Map.Entry<Long, Integer> entry : skuQuantities.entrySet()) {
      StockOperateCommandDTO command = new StockOperateCommandDTO();
      command.setOrderNo(orderNo);
      command.setSubOrderNo(subOrder.getSubOrderNo());
      command.setSkuId(entry.getKey());
      command.setQuantity(entry.getValue());
      command.setReason(action + " stock for " + subOrder.getSubOrderNo());
      commands.add(command);
    }
    return commands;
  }

  private boolean matchesTargetSubOrder(String targetSubOrderNo, OrderSub subOrder) {
    return targetSubOrderNo == null
        || targetSubOrderNo.isBlank()
        || targetSubOrderNo.equals(subOrder.getSubOrderNo());
  }

  private void triggerAutoRefundForCancelledOrder(
      OrderMain mainOrder, OrderSub subOrder, PaymentSuccessEvent event) {
    PaymentOrderVO paymentOrder = requirePaidPaymentOrder(mainOrder, subOrder);
    triggerAutoRefund(
        mainOrder,
        subOrder,
        paymentOrder,
        event,
        AUTO_REFUND_REASON_CANCELLED,
        AUTO_REFUND_DESCRIPTION_CANCELLED);
  }

  private void triggerAutoRefundForStockFreezeFailed(
      OrderMain mainOrder, OrderSub subOrder, PaymentOrderVO paymentOrder) {
    triggerAutoRefund(
        mainOrder,
        subOrder,
        paymentOrder,
        null,
        AUTO_REFUND_REASON_STOCK_FAILURE,
        AUTO_REFUND_DESCRIPTION_STOCK_FAILURE);
  }

  private void triggerAutoRefund(
      OrderMain mainOrder,
      OrderSub subOrder,
      PaymentOrderVO paymentOrder,
      PaymentSuccessEvent event,
      String reason,
      String description) {
    BigDecimal refundAmount = resolveRefundAmount(subOrder, paymentOrder, event);
    String afterSaleNo = buildAutoAfterSaleNo(paymentOrder, event, subOrder);
    AfterSale afterSale =
        ensureAutoRefundAfterSale(
            mainOrder, subOrder, paymentOrder, afterSaleNo, refundAmount, reason, description);
    if (STATUS_REFUNDED.equals(afterSale.getStatus())) {
      syncSubOrderAfterSaleStatus(subOrder, STATUS_REFUNDED);
      orderAggregateCacheService.evict(mainOrder.getId());
      return;
    }

    PaymentRefundCommandDTO refundCommand = new PaymentRefundCommandDTO();
    refundCommand.setRefundNo(
        com.cloud.order.service.support.OrderRefundSagaCoordinator.buildRefundNo(afterSaleNo));
    refundCommand.setPaymentNo(paymentOrder.getPaymentNo());
    refundCommand.setAfterSaleNo(afterSaleNo);
    refundCommand.setRefundAmount(refundAmount);
    refundCommand.setReason(reason);
    refundCommand.setIdempotencyKey(AUTO_REFUND_IDEMPOTENCY_PREFIX + afterSaleNo);

    Long refundId = paymentOrderRemoteService.createRefund(refundCommand);
    if (refundId == null) {
      throw new SystemException(
          "failed to create auto refund for subOrderNo=" + subOrder.getSubOrderNo());
    }
  }

  private AfterSale ensureAutoRefundAfterSale(
      OrderMain mainOrder,
      OrderSub subOrder,
      PaymentOrderVO paymentOrder,
      String afterSaleNo,
      BigDecimal refundAmount,
      String reason,
      String description) {
    AfterSale existing = findAfterSaleByNo(afterSaleNo);
    if (existing != null) {
      boolean changed = false;
      if (!STATUS_REFUNDED.equals(existing.getStatus())
          && !STATUS_REFUNDING.equals(existing.getStatus())) {
        existing.setStatus(STATUS_REFUNDING);
        changed = true;
      }
      if (!sameAmount(existing.getApplyAmount(), refundAmount)) {
        existing.setApplyAmount(refundAmount);
        changed = true;
      }
      if (!sameAmount(existing.getApprovedAmount(), refundAmount)) {
        existing.setApprovedAmount(refundAmount);
        changed = true;
      }
      if (isBlank(existing.getReason()) && reason != null) {
        existing.setReason(reason);
        changed = true;
      }
      if (isBlank(existing.getDescription()) && description != null) {
        existing.setDescription(description);
        changed = true;
      }
      if (isBlank(existing.getRefundChannel()) && paymentOrder.getChannel() != null) {
        existing.setRefundChannel(paymentOrder.getChannel());
        changed = true;
      }
      if (changed) {
        afterSaleMapper.updateById(existing);
      }
      syncSubOrderAfterSaleStatus(subOrder, existing.getStatus());
      orderAggregateCacheService.evict(mainOrder.getId());
      return existing;
    }

    AfterSale created = new AfterSale();
    created.setAfterSaleNo(afterSaleNo);
    created.setMainOrderId(mainOrder.getId());
    created.setSubOrderId(subOrder.getId());
    created.setUserId(mainOrder.getUserId());
    created.setMerchantId(subOrder.getMerchantId());
    created.setAfterSaleType("REFUND");
    created.setStatus(STATUS_REFUNDING);
    created.setReason(reason);
    created.setDescription(description);
    created.setApplyAmount(refundAmount);
    created.setApprovedAmount(refundAmount);
    created.setRefundChannel(paymentOrder.getChannel());
    try {
      afterSaleMapper.insert(created);
    } catch (DuplicateKeyException duplicateKeyException) {
      AfterSale duplicated = findAfterSaleByNo(afterSaleNo);
      if (duplicated == null) {
        throw duplicateKeyException;
      }
      syncSubOrderAfterSaleStatus(subOrder, duplicated.getStatus());
      orderAggregateCacheService.evict(mainOrder.getId());
      return duplicated;
    }
    syncSubOrderAfterSaleStatus(subOrder, STATUS_REFUNDING);
    orderAggregateCacheService.evict(mainOrder.getId());
    return created;
  }

  private AfterSale findAfterSaleByNo(String afterSaleNo) {
    if (afterSaleNo == null || afterSaleNo.isBlank()) {
      return null;
    }
    return afterSaleMapper.selectOne(
        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AfterSale>()
            .eq(AfterSale::getAfterSaleNo, afterSaleNo)
            .eq(AfterSale::getDeleted, 0)
            .last("LIMIT 1"));
  }

  private PaymentOrderVO findPaidPaymentOrder(OrderMain mainOrder, OrderSub subOrder) {
    if (mainOrder == null
        || mainOrder.getMainOrderNo() == null
        || mainOrder.getMainOrderNo().isBlank()
        || subOrder == null
        || subOrder.getSubOrderNo() == null
        || subOrder.getSubOrderNo().isBlank()) {
      return null;
    }
    PaymentOrderVO paymentOrder =
        paymentOrderRemoteService.getPaymentOrderByOrderNo(
            mainOrder.getMainOrderNo(), subOrder.getSubOrderNo());
    if (paymentOrder == null || !PAYMENT_STATUS_PAID.equals(paymentOrder.getStatus())) {
      return null;
    }
    return paymentOrder;
  }

  private PaymentOrderVO requirePaidPaymentOrder(OrderMain mainOrder, OrderSub subOrder) {
    PaymentOrderVO paymentOrder = findPaidPaymentOrder(mainOrder, subOrder);
    if (paymentOrder == null || paymentOrder.getPaymentNo() == null) {
      throw new SystemException(
          "payment order is not ready for auto refund: subOrderNo=" + subOrder.getSubOrderNo());
    }
    return paymentOrder;
  }

  private void syncSubOrderAfterSaleStatus(OrderSub subOrder, String status) {
    if (subOrder == null
        || status == null
        || Objects.equals(status, subOrder.getAfterSaleStatus())) {
      return;
    }
    subOrder.setAfterSaleStatus(status);
    orderSubMapper.updateById(subOrder);
  }

  private BigDecimal resolveRefundAmount(
      OrderSub subOrder, PaymentOrderVO paymentOrder, PaymentSuccessEvent event) {
    if (paymentOrder != null && isPositive(paymentOrder.getAmount())) {
      return paymentOrder.getAmount();
    }
    if (event != null && isPositive(event.getAmount())) {
      return event.getAmount();
    }
    if (subOrder != null && isPositive(subOrder.getPayableAmount())) {
      return subOrder.getPayableAmount();
    }
    throw new SystemException(
        "refund amount is missing for subOrderNo="
            + (subOrder == null ? null : subOrder.getSubOrderNo()));
  }

  private String buildAutoAfterSaleNo(
      PaymentOrderVO paymentOrder, PaymentSuccessEvent event, OrderSub subOrder) {
    if (paymentOrder != null && paymentOrder.getId() != null) {
      return AUTO_AFTER_SALE_PREFIX + paymentOrder.getId();
    }
    if (event != null && event.getPaymentId() != null) {
      return AUTO_AFTER_SALE_PREFIX + event.getPaymentId();
    }
    if (subOrder != null
        && subOrder.getSubOrderNo() != null
        && !subOrder.getSubOrderNo().isBlank()) {
      return AUTO_AFTER_SALE_PREFIX + subOrder.getSubOrderNo().replaceAll("[^A-Za-z0-9]", "");
    }
    throw new SystemException("failed to build auto after-sale number");
  }

  private boolean sameAmount(BigDecimal left, BigDecimal right) {
    if (left == null || right == null) {
      return left == null && right == null;
    }
    return left.compareTo(right) == 0;
  }

  private boolean isPositive(BigDecimal amount) {
    return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
