package com.cloud.order.service.impl;

import cn.hutool.core.util.StrUtil;
import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import com.cloud.common.exception.BizException;
import com.cloud.common.messaging.event.OrderTimeoutEvent;
import com.cloud.common.messaging.event.StockReserveRequestEvent;
import com.cloud.order.dto.CreateMainOrderRequest;
import com.cloud.order.dto.OrderAggregateResponse;
import com.cloud.order.entity.OrderItem;
import com.cloud.order.entity.OrderMain;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.mapper.OrderMainMapper;
import com.cloud.order.messaging.OrderMessageProducer;
import com.cloud.order.messaging.OrderTimeoutMessageProducer;
import com.cloud.order.service.OrderPlacementService;
import com.cloud.order.service.OrderService;
import com.cloud.order.service.support.OrderPlacementSupport;
import com.cloud.order.service.support.StockReservationRemoteService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderPlacementServiceImpl implements OrderPlacementService {

  private final OrderMainMapper orderMainMapper;
  private final OrderService orderService;
  private final OrderPlacementSupport orderPlacementSupport;
  private final StockReservationRemoteService stockReservationRemoteService;
  private final OrderMessageProducer orderMessageProducer;
  private final OrderTimeoutMessageProducer orderTimeoutMessageProducer;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public OrderAggregateResponse createOrder(CreateMainOrderRequest request) {
    String clientOrderId = normalizeClientOrderId(request.getClientOrderId());
    request.setClientOrderId(clientOrderId);
    String idempotencyKey =
        normalizeIdempotencyKey(request.getIdempotencyKey(), request.getUserId());
    request.setIdempotencyKey(idempotencyKey);

    OrderMain existingByClientOrderId =
        orderMainMapper.selectActiveByClientOrderId(request.getUserId(), clientOrderId);
    if (existingByClientOrderId != null) {
      return requireAggregate(orderService.getOrderAggregate(existingByClientOrderId.getId()));
    }

    OrderMain existing = orderMainMapper.selectActiveByIdempotencyKey(idempotencyKey);
    if (existing != null) {
      return requireAggregate(orderService.getOrderAggregate(existing.getId()));
    }

    orderPlacementSupport.prepareRequest(request);
    List<StockOperateCommandDTO> stockCommands = buildStockCommands(request);
    if (!Boolean.TRUE.equals(stockReservationRemoteService.preCheck(stockCommands))) {
      throw new BizException("insufficient available stock");
    }

    OrderMain mainOrder = orderService.createMainOrder(request);
    if (request.getCartId() != null) {
      orderPlacementSupport.markCartCheckedOut(request.getCartId(), request.getUserId());
    }
    OrderAggregateResponse aggregate =
        requireAggregate(orderService.getOrderAggregate(mainOrder.getId()));
    emitReserveRequest(aggregate);
    emitTimeoutEvents(aggregate);
    return aggregate;
  }

  private void emitReserveRequest(OrderAggregateResponse aggregate) {
    StockReserveRequestEvent event =
        StockReserveRequestEvent.builder()
            .orderNo(aggregate.getMainOrder().getMainOrderNo())
            .items(flattenStockCommands(aggregate))
            .build();
    if (!orderMessageProducer.sendStockReserveRequestEvent(event)) {
      throw new IllegalStateException("failed to enqueue stock reserve request");
    }
  }

  private void emitTimeoutEvents(OrderAggregateResponse aggregate) {
    for (OrderAggregateResponse.SubOrderWithItems wrapped : aggregate.getSubOrders()) {
      OrderSub subOrder = wrapped.getSubOrder();
      if (subOrder == null) {
        continue;
      }
      OrderTimeoutEvent event =
          OrderTimeoutEvent.builder()
              .subOrderId(subOrder.getId())
              .subOrderNo(subOrder.getSubOrderNo())
              .mainOrderNo(aggregate.getMainOrder().getMainOrderNo())
              .userId(aggregate.getMainOrder().getUserId())
              .build();
      orderTimeoutMessageProducer.sendAfterCommit(event);
    }
  }

  private List<StockOperateCommandDTO> buildStockCommands(CreateMainOrderRequest request) {
    List<StockOperateCommandDTO> commands = new ArrayList<>();
    for (CreateMainOrderRequest.CreateSubOrderRequest subOrder : request.getSubOrders()) {
      Map<Long, Integer> skuQuantities = new LinkedHashMap<>();
      for (CreateMainOrderRequest.CreateOrderItemRequest item : subOrder.getItems()) {
        if (item == null || item.getSkuId() == null || item.getQuantity() == null) {
          continue;
        }
        skuQuantities.merge(item.getSkuId(), item.getQuantity(), Integer::sum);
      }
      for (Map.Entry<Long, Integer> entry : skuQuantities.entrySet()) {
        StockOperateCommandDTO command = new StockOperateCommandDTO();
        command.setSubOrderNo(
            buildVirtualSubOrderNo(request, subOrder.getMerchantId(), entry.getKey()));
        command.setSkuId(entry.getKey());
        command.setQuantity(entry.getValue());
        command.setReason("pre-check stock");
        commands.add(command);
      }
    }
    return commands;
  }

  private List<StockOperateCommandDTO> flattenStockCommands(OrderAggregateResponse aggregate) {
    List<StockOperateCommandDTO> commands = new ArrayList<>();
    for (OrderAggregateResponse.SubOrderWithItems wrapped : aggregate.getSubOrders()) {
      OrderSub subOrder = wrapped.getSubOrder();
      if (subOrder == null) {
        continue;
      }
      Map<Long, Integer> skuQuantities = new LinkedHashMap<>();
      for (OrderItem item : wrapped.getItems()) {
        if (item == null || item.getSkuId() == null || item.getQuantity() == null) {
          continue;
        }
        skuQuantities.merge(item.getSkuId(), item.getQuantity(), Integer::sum);
      }
      for (Map.Entry<Long, Integer> entry : skuQuantities.entrySet()) {
        StockOperateCommandDTO command = new StockOperateCommandDTO();
        command.setOrderNo(aggregate.getMainOrder().getMainOrderNo());
        command.setSubOrderNo(subOrder.getSubOrderNo());
        command.setSkuId(entry.getKey());
        command.setQuantity(entry.getValue());
        command.setReason("reserve stock for " + aggregate.getMainOrder().getMainOrderNo());
        commands.add(command);
      }
    }
    return commands;
  }

  private String buildVirtualSubOrderNo(
      CreateMainOrderRequest request, Long merchantId, Long skuId) {
    return request.getUserId() + ":" + merchantId + ":" + skuId;
  }

  private OrderAggregateResponse requireAggregate(OrderAggregateResponse aggregate) {
    if (aggregate == null || aggregate.getMainOrder() == null) {
      throw new BizException("order aggregate not found");
    }
    if (aggregate.getSubOrders() == null || aggregate.getSubOrders().isEmpty()) {
      throw new BizException("order aggregate has no sub orders");
    }
    return aggregate;
  }

  private String normalizeIdempotencyKey(String idempotencyKey, Long userId) {
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

  private String normalizeClientOrderId(String clientOrderId) {
    if (StrUtil.isBlank(clientOrderId)) {
      throw new BizException("clientOrderId is required");
    }
    return clientOrderId.trim();
  }
}
