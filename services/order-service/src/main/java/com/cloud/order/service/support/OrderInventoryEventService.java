package com.cloud.order.service.support;

import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import com.cloud.common.messaging.event.PaymentSuccessEvent;
import com.cloud.common.messaging.event.StockConfirmRequestEvent;
import com.cloud.order.dto.OrderAggregateResponse;
import com.cloud.order.entity.OrderItem;
import com.cloud.order.entity.OrderMain;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.enums.OrderAction;
import com.cloud.order.mapper.OrderMainMapper;
import com.cloud.order.service.OrderService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderInventoryEventService {

  private final OrderMainMapper orderMainMapper;
  private final OrderService orderService;
  private final com.cloud.order.messaging.OrderMessageProducer orderMessageProducer;

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
      if (!"CREATED".equals(previousStatus) && !"STOCK_RESERVED".equals(previousStatus)) {
        continue;
      }
      orderService.advanceSubOrderStatus(subOrder.getId(), OrderAction.PAY);
      if ("STOCK_RESERVED".equals(previousStatus)) {
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
      if ("CREATED".equals(subOrder.getOrderStatus())) {
        orderService.advanceSubOrderStatus(subOrder.getId(), OrderAction.RESERVE);
        continue;
      }
      if ("PAID".equals(subOrder.getOrderStatus())) {
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
      if (subOrder == null) {
        continue;
      }
      String status = subOrder.getOrderStatus();
      if ("CANCELLED".equals(status) || "CLOSED".equals(status)) {
        continue;
      }
      orderService.advanceSubOrderStatus(subOrder.getId(), OrderAction.CANCEL);
    }
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
}
