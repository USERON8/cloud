package com.cloud.order.service.impl;

import cn.hutool.core.util.StrUtil;
import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.common.messaging.event.OrderAutoReceiveEvent;
import com.cloud.common.messaging.event.OrderShippedEvent;
import com.cloud.common.metrics.TradeMetrics;
import com.cloud.order.dto.CreateMainOrderRequest;
import com.cloud.order.dto.OrderAggregateResponse;
import com.cloud.order.entity.AfterSale;
import com.cloud.order.entity.OrderItem;
import com.cloud.order.entity.OrderMain;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.enums.AfterSaleAction;
import com.cloud.order.enums.OrderAction;
import com.cloud.order.mapper.AfterSaleMapper;
import com.cloud.order.mapper.OrderItemMapper;
import com.cloud.order.mapper.OrderMainMapper;
import com.cloud.order.mapper.OrderSubMapper;
import com.cloud.order.messaging.OrderAutoReceiveMessageProducer;
import com.cloud.order.messaging.OrderShippedMessageProducer;
import com.cloud.order.service.OrderService;
import com.cloud.order.service.support.OrderAggregateCacheService;
import com.cloud.order.service.support.OrderRefundSagaCoordinator;
import com.cloud.order.service.support.StockReservationRemoteService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

  private static final Set<String> AFTER_SALE_ELIGIBLE_SUB_STATUSES =
      Set.of("PAID", "SHIPPED", "DONE");
  private static final Set<String> REAPPLY_ALLOWED_AFTER_SALE_STATUSES =
      Set.of("NONE", "CANCELLED", "REJECTED", "CLOSED");

  private static final Map<String, Set<String>> SUB_STATUS_TRANSITIONS =
      Map.of(
          "CREATED", Set.of("STOCK_RESERVED", "CANCELLED", "CLOSED"),
          "STOCK_RESERVED", Set.of("PAID", "CANCELLED", "CLOSED"),
          "PAID", Set.of("SHIPPED", "CANCELLED", "CLOSED"),
          "SHIPPED", Set.of("DONE", "CLOSED"),
          "DONE", Set.of(),
          "CANCELLED", Set.of(),
          "CLOSED", Set.of());

  private static final Map<String, Set<String>> AFTER_SALE_TRANSITIONS =
      Map.ofEntries(
          Map.entry("APPLIED", Set.of("AUDITING", "CANCELLED")),
          Map.entry("AUDITING", Set.of("APPROVED", "REJECTED", "CLOSED")),
          Map.entry("APPROVED", Set.of("WAIT_RETURN", "REFUNDING", "CLOSED")),
          Map.entry("WAIT_RETURN", Set.of("RETURNED", "CANCELLED", "CLOSED")),
          Map.entry("RETURNED", Set.of("RECEIVED", "CLOSED")),
          Map.entry("RECEIVED", Set.of("REFUNDING", "CLOSED")),
          Map.entry("REFUNDING", Set.of("REFUNDED", "CLOSED")),
          Map.entry("REFUNDED", Set.of()),
          Map.entry("REJECTED", Set.of("CLOSED")),
          Map.entry("CANCELLED", Set.of()),
          Map.entry("CLOSED", Set.of()));

  private final OrderMainMapper orderMainMapper;
  private final OrderSubMapper orderSubMapper;
  private final OrderItemMapper orderItemMapper;
  private final AfterSaleMapper afterSaleMapper;
  private final StockReservationRemoteService stockReservationRemoteService;
  private final ObjectProvider<OrderRefundSagaCoordinator> orderRefundSagaCoordinatorProvider;
  private final TradeMetrics tradeMetrics;
  private final OrderAggregateCacheService orderAggregateCacheService;
  private final OrderShippedMessageProducer orderShippedMessageProducer;
  private final OrderAutoReceiveMessageProducer orderAutoReceiveMessageProducer;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public OrderMain createMainOrder(CreateMainOrderRequest request) {
    try {
      String idempotencyKey =
          buildScopedIdempotencyKey(request.getUserId(), request.getIdempotencyKey());
      if (request.getSubOrders() == null || request.getSubOrders().isEmpty()) {
        throw new BizException("subOrders is required");
      }

      OrderMain existing = findActiveMainOrderByIdempotencyKey(idempotencyKey);
      if (existing != null) {
        tradeMetrics.incrementOrder("success");
        return existing;
      }

      OrderMain main = new OrderMain();
      main.setMainOrderNo("M" + UUID.randomUUID().toString().replace("-", ""));
      main.setUserId(request.getUserId());
      main.setOrderStatus("CREATED");
      main.setTotalAmount(defaultAmount(request.getTotalAmount()));
      main.setPayableAmount(defaultAmount(request.getPayableAmount()));
      main.setRemark(request.getRemark());
      main.setIdempotencyKey(idempotencyKey);

      try {
        orderMainMapper.insert(main);
      } catch (DuplicateKeyException duplicateKeyException) {
        OrderMain duplicated = findActiveMainOrderByIdempotencyKey(idempotencyKey);
        if (duplicated != null) {
          tradeMetrics.incrementOrder("success");
          return duplicated;
        }
        throw duplicateKeyException;
      }

      for (CreateMainOrderRequest.CreateSubOrderRequest subRequest : request.getSubOrders()) {
        OrderSub sub = new OrderSub();
        sub.setSubOrderNo("S" + UUID.randomUUID().toString().replace("-", ""));
        sub.setMainOrderId(main.getId());
        sub.setMerchantId(subRequest.getMerchantId());
        sub.setOrderStatus("CREATED");
        sub.setShippingStatus("PENDING");
        sub.setAfterSaleStatus("NONE");
        sub.setItemAmount(defaultAmount(subRequest.getItemAmount()));
        sub.setShippingFee(defaultAmount(subRequest.getShippingFee()));
        sub.setDiscountAmount(defaultAmount(subRequest.getDiscountAmount()));
        sub.setPayableAmount(defaultAmount(subRequest.getPayableAmount()));
        sub.setReceiverName(subRequest.getReceiverName());
        sub.setReceiverPhone(subRequest.getReceiverPhone());
        sub.setReceiverAddress(subRequest.getReceiverAddress());
        orderSubMapper.insert(sub);

        for (CreateMainOrderRequest.CreateOrderItemRequest itemRequest : subRequest.getItems()) {
          OrderItem item = new OrderItem();
          item.setMainOrderId(main.getId());
          item.setSubOrderId(sub.getId());
          item.setSpuId(itemRequest.getSpuId());
          item.setSkuId(itemRequest.getSkuId());
          item.setSkuCode(itemRequest.getSkuCode());
          item.setSkuName(itemRequest.getSkuName());
          item.setSkuSnapshot(itemRequest.getSkuSnapshot());
          item.setQuantity(itemRequest.getQuantity());
          item.setUnitPrice(defaultAmount(itemRequest.getUnitPrice()));
          item.setTotalPrice(defaultAmount(itemRequest.getTotalPrice()));
          orderItemMapper.insert(item);
        }
      }
      tradeMetrics.incrementOrder("success");
      return main;
    } catch (Exception ex) {
      tradeMetrics.incrementOrder("failed");
      throw ex;
    }
  }

  private OrderMain findActiveMainOrderByIdempotencyKey(String idempotencyKey) {
    return orderMainMapper.selectActiveByIdempotencyKey(idempotencyKey);
  }

  @Override
  public OrderMain getMainOrder(Long mainOrderId) {
    return orderMainMapper.selectById(mainOrderId);
  }

  @Override
  public OrderAggregateResponse getOrderAggregate(Long mainOrderId) {
    OrderAggregateResponse cached = orderAggregateCacheService.get(mainOrderId);
    if (cached != null) {
      return cached;
    }
    OrderMain main = orderMainMapper.selectById(mainOrderId);
    if (main == null || Integer.valueOf(1).equals(main.getDeleted())) {
      return null;
    }
    List<OrderSub> subOrders = listSubOrders(mainOrderId);

    OrderAggregateResponse response = new OrderAggregateResponse();
    response.setMainOrder(main);
    List<OrderAggregateResponse.SubOrderWithItems> wrapped = new ArrayList<>(subOrders.size());

    Map<Long, List<OrderItem>> itemsBySubOrder = new LinkedHashMap<>();
    if (!subOrders.isEmpty()) {
      List<Long> subOrderIds =
          subOrders.stream().map(OrderSub::getId).filter(id -> id != null).toList();
      if (!subOrderIds.isEmpty()) {
        List<OrderItem> items = orderItemMapper.listActiveBySubOrderIds(subOrderIds);
        for (OrderItem item : items) {
          if (item.getSubOrderId() == null) {
            continue;
          }
          itemsBySubOrder
              .computeIfAbsent(item.getSubOrderId(), ignored -> new ArrayList<>())
              .add(item);
        }
      }
    }

    for (OrderSub subOrder : subOrders) {
      List<OrderItem> items = itemsBySubOrder.getOrDefault(subOrder.getId(), List.of());
      OrderAggregateResponse.SubOrderWithItems item =
          new OrderAggregateResponse.SubOrderWithItems();
      item.setSubOrder(subOrder);
      item.setItems(items);
      wrapped.add(item);
    }
    response.setSubOrders(wrapped);
    if ("DONE".equals(main.getOrderStatus())) {
      orderAggregateCacheService.put(mainOrderId, response);
    }
    return response;
  }

  @Override
  public List<OrderSub> listSubOrders(Long mainOrderId) {
    return orderSubMapper.listActiveByMainOrderId(mainOrderId);
  }

  @Override
  public OrderSub getSubOrder(Long subOrderId) {
    return orderSubMapper.selectById(subOrderId);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public OrderSub advanceSubOrderStatus(Long subOrderId, OrderAction action) {
    OrderSub sub = orderSubMapper.selectById(subOrderId);
    if (sub == null || sub.getDeleted() == 1) {
      throw new BizException("sub order not found");
    }
    String targetStatus = action.targetStatus();
    validateSubTransition(sub.getOrderStatus(), targetStatus);

    if (requiresStockRelease(sub.getOrderStatus(), targetStatus)) {
      releaseReservedStock(sub);
    }
    if ("SHIPPED".equals(targetStatus)) {
      assertLogisticsInfoReady(sub.getShippingCompany(), sub.getTrackingNumber());
    }

    sub.setOrderStatus(targetStatus);
    if ("SHIPPED".equals(targetStatus)) {
      if (sub.getShippedAt() == null) {
        sub.setShippedAt(LocalDateTime.now());
      }
      if (sub.getEstimatedArrival() == null) {
        sub.setEstimatedArrival(LocalDate.now().plusDays(3));
      }
    } else if ("DONE".equals(targetStatus)) {
      if (sub.getReceivedAt() == null) {
        sub.setReceivedAt(LocalDateTime.now());
      }
      sub.setDoneAt(LocalDateTime.now());
    } else if ("CLOSED".equals(targetStatus) || "CANCELLED".equals(targetStatus)) {
      sub.setClosedAt(LocalDateTime.now());
    }
    orderSubMapper.updateById(sub);
    if ("SHIPPED".equals(targetStatus)) {
      orderShippedMessageProducer.sendAfterCommit(buildOrderShippedEvent(sub));
      orderAutoReceiveMessageProducer.sendAfterCommit(buildOrderAutoReceiveEvent(sub));
    }
    refreshMainOrderStatus(sub.getMainOrderId());
    orderAggregateCacheService.evict(sub.getMainOrderId());
    return sub;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public AfterSale applyAfterSale(AfterSale afterSale) {
    validateAfterSaleCreation(afterSale);
    afterSale.setAfterSaleNo("AS" + System.currentTimeMillis());
    afterSale.setStatus("APPLIED");
    afterSale.setApprovedAmount(null);
    afterSale.setRefundedAt(null);
    afterSale.setClosedAt(null);
    afterSale.setCloseReason(null);
    afterSaleMapper.insert(afterSale);
    syncSubOrderAfterSaleStatus(afterSale.getSubOrderId(), afterSale.getStatus());
    orderAggregateCacheService.evict(afterSale.getMainOrderId());
    return afterSale;
  }

  @Override
  public AfterSale getAfterSale(Long afterSaleId) {
    return afterSaleMapper.selectById(afterSaleId);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public AfterSale advanceAfterSaleStatus(Long afterSaleId, AfterSaleAction action, String remark) {
    if (action == AfterSaleAction.PROCESS) {
      return startRefundSaga(afterSaleId, remark);
    }

    boolean refundSuccess = false;
    try {
      AfterSale afterSale = afterSaleMapper.selectById(afterSaleId);
      if (afterSale == null || afterSale.getDeleted() == 1) {
        throw new BizException("after sale not found");
      }
      String targetStatus = action.targetStatus();
      validateAfterSaleTransition(afterSale.getStatus(), targetStatus);
      refundSuccess = "REFUNDED".equals(targetStatus);

      afterSale.setStatus(targetStatus);
      if ("REFUNDED".equals(targetStatus)) {
        afterSale.setRefundedAt(LocalDateTime.now());
      }
      if ("CLOSED".equals(targetStatus)) {
        afterSale.setClosedAt(LocalDateTime.now());
        afterSale.setCloseReason(remark);
      }
      afterSaleMapper.updateById(afterSale);
      syncSubOrderAfterSaleStatus(afterSale.getSubOrderId(), afterSale.getStatus());
      orderAggregateCacheService.evict(afterSale.getMainOrderId());

      if (refundSuccess) {
        tradeMetrics.incrementRefund("success");
      }
      return afterSale;
    } catch (Exception ex) {
      throw ex;
    }
  }

  private void syncSubOrderAfterSaleStatus(Long subOrderId, String afterSaleStatus) {
    if (subOrderId == null || StrUtil.isBlank(afterSaleStatus)) {
      return;
    }
    OrderSub subOrder = orderSubMapper.selectById(subOrderId);
    if (subOrder == null || Integer.valueOf(1).equals(subOrder.getDeleted())) {
      return;
    }
    subOrder.setAfterSaleStatus(afterSaleStatus);
    orderSubMapper.updateById(subOrder);
  }

  private void validateAfterSaleCreation(AfterSale afterSale) {
    if (afterSale == null) {
      throw new BizException(ResultCode.BAD_REQUEST, "after sale payload is required");
    }
    OrderMain mainOrder = requireOrderMain(afterSale.getMainOrderId());
    OrderSub subOrder = requireSubOrder(afterSale.getSubOrderId());
    if (!Objects.equals(subOrder.getMainOrderId(), mainOrder.getId())) {
      throw new BizException(ResultCode.BAD_REQUEST, "sub order does not belong to main order");
    }
    if (afterSale.getUserId() != null
        && !Objects.equals(afterSale.getUserId(), mainOrder.getUserId())) {
      throw new BizException(
          ResultCode.FORBIDDEN, "after sale user does not match the order owner");
    }
    if (afterSale.getMerchantId() != null
        && !Objects.equals(afterSale.getMerchantId(), subOrder.getMerchantId())) {
      throw new BizException(
          ResultCode.FORBIDDEN, "after sale merchant does not match the sub order merchant");
    }
    if (!AFTER_SALE_ELIGIBLE_SUB_STATUSES.contains(subOrder.getOrderStatus())) {
      throw new BizException(
          ResultCode.BAD_REQUEST,
          "after sale is not allowed for sub order status: " + subOrder.getOrderStatus());
    }
    validateAfterSaleAvailability(subOrder);
    BigDecimal applyAmount = requirePositiveAmount(afterSale.getApplyAmount(), "apply amount");
    BigDecimal payableAmount = defaultAmount(subOrder.getPayableAmount());
    if (applyAmount.compareTo(payableAmount) > 0) {
      throw new BizException(
          ResultCode.BAD_REQUEST, "apply amount cannot exceed sub order payable amount");
    }
    afterSale.setMainOrderId(mainOrder.getId());
    afterSale.setSubOrderId(subOrder.getId());
    afterSale.setUserId(mainOrder.getUserId());
    afterSale.setMerchantId(subOrder.getMerchantId());
    afterSale.setApplyAmount(applyAmount);
  }

  private void validateAfterSaleAvailability(OrderSub subOrder) {
    String afterSaleStatus = subOrder.getAfterSaleStatus();
    String normalizedStatus =
        afterSaleStatus == null || afterSaleStatus.isBlank() ? "NONE" : afterSaleStatus.trim();
    if (REAPPLY_ALLOWED_AFTER_SALE_STATUSES.contains(normalizedStatus)) {
      return;
    }
    if ("REFUNDED".equals(normalizedStatus)) {
      throw new BizException(
          ResultCode.BAD_REQUEST, "after sale is already completed for this sub order");
    }
    throw new BizException(
        ResultCode.BAD_REQUEST, "an active after-sale request already exists for this sub order");
  }

  private void assertLogisticsInfoReady(String company, String trackingNumber) {
    if (StrUtil.isBlank(company) || StrUtil.isBlank(trackingNumber)) {
      throw new BizException("shipping company and tracking number are required");
    }
  }

  private void validateSubTransition(String current, String target) {
    Set<String> allowed = SUB_STATUS_TRANSITIONS.getOrDefault(current, Set.of());
    if (!allowed.contains(target)) {
      throw new BizException("invalid order status transition: " + current + " -> " + target);
    }
  }

  private boolean requiresStockRelease(String currentStatus, String targetStatus) {
    if (!Set.of("CANCELLED", "CLOSED").contains(targetStatus)) {
      return false;
    }
    return "STOCK_RESERVED".equals(currentStatus);
  }

  private void releaseReservedStock(OrderSub subOrder) {
    List<OrderItem> items = orderItemMapper.listActiveBySubOrderId(subOrder.getId());
    if (items == null || items.isEmpty()) {
      return;
    }
    String orderNo = null;
    if (subOrder.getMainOrderId() != null) {
      OrderMain mainOrder = orderMainMapper.selectById(subOrder.getMainOrderId());
      if (mainOrder != null && mainOrder.getDeleted() != null && mainOrder.getDeleted() == 0) {
        orderNo = mainOrder.getMainOrderNo();
      }
    }
    Map<Long, Integer> skuQuantities = new LinkedHashMap<>();
    for (OrderItem item : items) {
      if (item.getSkuId() == null || item.getQuantity() == null) {
        continue;
      }
      skuQuantities.merge(item.getSkuId(), item.getQuantity(), Integer::sum);
    }
    for (Map.Entry<Long, Integer> entry : skuQuantities.entrySet()) {
      StockOperateCommandDTO command = new StockOperateCommandDTO();
      command.setSubOrderNo(subOrder.getSubOrderNo());
      command.setOrderNo(orderNo);
      command.setSkuId(entry.getKey());
      command.setQuantity(entry.getValue());
      command.setReason("cancel order " + subOrder.getSubOrderNo());
      stockReservationRemoteService.release(command);
    }
  }

  private void validateAfterSaleTransition(String current, String target) {
    Set<String> allowed = AFTER_SALE_TRANSITIONS.getOrDefault(current, Set.of());
    if (!allowed.contains(target)) {
      throw new BizException("invalid after-sale status transition: " + current + " -> " + target);
    }
  }

  private void refreshMainOrderStatus(Long mainOrderId) {
    if (mainOrderId == null) {
      return;
    }
    OrderMain mainOrder = orderMainMapper.selectById(mainOrderId);
    if (mainOrder == null || Integer.valueOf(1).equals(mainOrder.getDeleted())) {
      return;
    }
    List<OrderSub> subOrders = listSubOrders(mainOrderId);
    if (subOrders.isEmpty()) {
      return;
    }

    boolean allDone = subOrders.stream().allMatch(s -> "DONE".equals(s.getOrderStatus()));
    boolean allClosed =
        subOrders.stream()
            .allMatch(
                s -> "CANCELLED".equals(s.getOrderStatus()) || "CLOSED".equals(s.getOrderStatus()));
    boolean anyReserved =
        subOrders.stream().anyMatch(s -> "STOCK_RESERVED".equals(s.getOrderStatus()));
    boolean anyPaidOrLater =
        subOrders.stream()
            .anyMatch(s -> Set.of("PAID", "SHIPPED", "DONE").contains(s.getOrderStatus()));

    if (allDone) {
      mainOrder.setOrderStatus("DONE");
    } else if (allClosed) {
      mainOrder.setOrderStatus("CANCELLED");
      mainOrder.setCancelledAt(LocalDateTime.now());
    } else if (anyPaidOrLater) {
      mainOrder.setOrderStatus("PAID");
      if (mainOrder.getPaidAt() == null) {
        mainOrder.setPaidAt(LocalDateTime.now());
      }
    } else if (anyReserved) {
      mainOrder.setOrderStatus("STOCK_RESERVED");
    } else {
      mainOrder.setOrderStatus("CREATED");
    }
    orderMainMapper.updateById(mainOrder);
  }

  private OrderShippedEvent buildOrderShippedEvent(OrderSub subOrder) {
    OrderMain mainOrder =
        subOrder.getMainOrderId() == null
            ? null
            : orderMainMapper.selectById(subOrder.getMainOrderId());
    return OrderShippedEvent.builder()
        .subOrderId(subOrder.getId())
        .mainOrderNo(mainOrder == null ? null : mainOrder.getMainOrderNo())
        .subOrderNo(subOrder.getSubOrderNo())
        .userId(mainOrder == null ? null : mainOrder.getUserId())
        .shippingCompany(subOrder.getShippingCompany())
        .trackingNumber(subOrder.getTrackingNumber())
        .shippedAt(subOrder.getShippedAt())
        .estimatedArrival(subOrder.getEstimatedArrival())
        .build();
  }

  private OrderAutoReceiveEvent buildOrderAutoReceiveEvent(OrderSub subOrder) {
    OrderMain mainOrder =
        subOrder.getMainOrderId() == null
            ? null
            : orderMainMapper.selectById(subOrder.getMainOrderId());
    return OrderAutoReceiveEvent.builder()
        .subOrderId(subOrder.getId())
        .mainOrderNo(mainOrder == null ? null : mainOrder.getMainOrderNo())
        .subOrderNo(subOrder.getSubOrderNo())
        .userId(mainOrder == null ? null : mainOrder.getUserId())
        .build();
  }

  private AfterSale startRefundSaga(Long afterSaleId, String remark) {
    AfterSale afterSale = afterSaleMapper.selectById(afterSaleId);
    if (afterSale == null || afterSale.getDeleted() == 1) {
      throw new BizException("after sale not found");
    }
    String targetStatus = AfterSaleAction.PROCESS.targetStatus();
    validateAfterSaleTransition(afterSale.getStatus(), targetStatus);

    OrderRefundSagaCoordinator coordinator = orderRefundSagaCoordinatorProvider.getIfAvailable();
    if (coordinator == null) {
      throw new BizException("refund saga is disabled");
    }

    try {
      coordinator.startRefundSaga(afterSale, remark);
      AfterSale latest = afterSaleMapper.selectById(afterSaleId);
      return latest == null ? afterSale : latest;
    } catch (Exception ex) {
      tradeMetrics.incrementRefund("failed");
      throw ex;
    }
  }

  private OrderMain requireOrderMain(Long mainOrderId) {
    if (mainOrderId == null) {
      throw new BizException("main order id is required");
    }
    OrderMain mainOrder = orderMainMapper.selectById(mainOrderId);
    if (mainOrder == null || Integer.valueOf(1).equals(mainOrder.getDeleted())) {
      throw new BizException("main order not found");
    }
    return mainOrder;
  }

  private OrderSub requireSubOrder(Long subOrderId) {
    if (subOrderId == null) {
      throw new BizException("sub order id is required");
    }
    OrderSub subOrder = orderSubMapper.selectById(subOrderId);
    if (subOrder == null || Integer.valueOf(1).equals(subOrder.getDeleted())) {
      throw new BizException("sub order not found");
    }
    return subOrder;
  }

  private BigDecimal requirePositiveAmount(BigDecimal amount, String fieldName) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new BizException(ResultCode.BAD_REQUEST, fieldName + " must be greater than 0");
    }
    return amount;
  }

  private BigDecimal defaultAmount(BigDecimal amount) {
    return amount == null ? BigDecimal.ZERO : amount;
  }

  private String buildScopedIdempotencyKey(Long userId, String idempotencyKey) {
    if (StrUtil.isBlank(idempotencyKey)) {
      throw new BizException("idempotency key is required");
    }
    if (userId == null) {
      throw new BizException("user id is required for idempotency");
    }
    String trimmed = idempotencyKey.trim();
    String prefix = userId + ":";
    if (trimmed.startsWith(prefix)) {
      return trimmed;
    }
    return prefix + trimmed;
  }
}
