package com.cloud.order.service.impl;

import cn.hutool.core.util.StrUtil;
import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import com.cloud.common.exception.BizException;
import com.cloud.order.dto.CreateMainOrderRequest;
import com.cloud.order.dto.OrderAggregateResponse;
import com.cloud.order.entity.OrderItem;
import com.cloud.order.entity.OrderMain;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.mapper.OrderMainMapper;
import com.cloud.order.service.OrderPlacementService;
import com.cloud.order.service.OrderService;
import com.cloud.order.service.support.StockReserveTccRemoteService;
import com.cloud.order.tcc.OrderCreateTccAction;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderPlacementServiceImpl implements OrderPlacementService {

  private static final Set<String> RESERVE_REQUIRED_STATUSES = Set.of("CREATED");

  private final OrderMainMapper orderMainMapper;
  private final OrderService orderService;
  private final OrderCreateTccAction orderCreateTccAction;
  private final StockReserveTccRemoteService stockReserveTccRemoteService;

  @Override
  @GlobalTransactional(rollbackFor = Exception.class)
  public OrderAggregateResponse createOrder(CreateMainOrderRequest request) {
    String idempotencyKey =
        normalizeIdempotencyKey(request.getIdempotencyKey(), request.getUserId());
    boolean prepared =
        orderCreateTccAction.prepare(null, idempotencyKey, request.getCartId(), request);
    if (!prepared) {
      throw new BizException("failed to create order aggregate");
    }

    OrderMain mainOrder = orderMainMapper.selectActiveByIdempotencyKey(idempotencyKey);
    if (mainOrder == null) {
      throw new BizException("order aggregate not found");
    }

    OrderAggregateResponse aggregate =
        requireAggregate(orderService.getOrderAggregate(mainOrder.getId()));
    reserveStockWithTcc(aggregate);
    return aggregate;
  }

  private void reserveStockWithTcc(OrderAggregateResponse aggregate) {
    List<StockReservationTask> tasks = collectReservationTasks(aggregate);
    if (tasks.isEmpty()) {
      return;
    }
    for (StockReservationTask task : tasks) {
      StockOperateCommandDTO command = new StockOperateCommandDTO();
      command.setSubOrderNo(task.getSubOrderNo());
      command.setOrderNo(task.getMainOrderNo());
      command.setSkuId(task.getSkuId());
      command.setQuantity(task.getQuantity());
      command.setReason("reserve stock for " + task.getMainOrderNo());

      Boolean reserved = stockReserveTccRemoteService.tryReserve(command);
      if (!Boolean.TRUE.equals(reserved)) {
        throw new BizException("reserve stock failed for skuId=" + task.getSkuId());
      }
    }
  }

  private List<StockReservationTask> collectReservationTasks(OrderAggregateResponse aggregate) {
    List<StockReservationTask> tasks = new ArrayList<>();
    for (OrderAggregateResponse.SubOrderWithItems wrapped : aggregate.getSubOrders()) {
      OrderSub subOrder = wrapped.getSubOrder();
      if (subOrder == null || !RESERVE_REQUIRED_STATUSES.contains(subOrder.getOrderStatus())) {
        continue;
      }
      tasks.addAll(buildReservationTasks(aggregate.getMainOrder(), subOrder, wrapped.getItems()));
    }
    return tasks;
  }

  private List<StockReservationTask> buildReservationTasks(
      OrderMain mainOrder, OrderSub subOrder, List<OrderItem> items) {
    if (items == null || items.isEmpty()) {
      throw new BizException("order items are required for stock reservation");
    }

    Map<Long, Integer> skuQuantities = new LinkedHashMap<>();
    for (OrderItem item : items) {
      if (item == null || item.getSkuId() == null || item.getQuantity() == null) {
        throw new BizException("invalid order item for stock reservation");
      }
      skuQuantities.merge(item.getSkuId(), item.getQuantity(), Integer::sum);
    }

    List<StockReservationTask> tasks = new ArrayList<>(skuQuantities.size());
    for (Map.Entry<Long, Integer> entry : skuQuantities.entrySet()) {
      tasks.add(
          new StockReservationTask(
              mainOrder.getMainOrderNo(),
              subOrder.getSubOrderNo(),
              entry.getKey(),
              entry.getValue()));
    }
    return tasks;
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

  private static final class StockReservationTask {
    private final String mainOrderNo;
    private final String subOrderNo;
    private final Long skuId;
    private final Integer quantity;

    private StockReservationTask(
        String mainOrderNo, String subOrderNo, Long skuId, Integer quantity) {
      this.mainOrderNo = mainOrderNo;
      this.subOrderNo = subOrderNo;
      this.skuId = skuId;
      this.quantity = quantity;
    }

    private String getMainOrderNo() {
      return mainOrderNo;
    }

    private String getSubOrderNo() {
      return subOrderNo;
    }

    private Long getSkuId() {
      return skuId;
    }

    private Integer getQuantity() {
      return quantity;
    }
  }
}
