package com.cloud.order.service.impl;

import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import com.cloud.common.exception.BusinessException;
import com.cloud.order.dto.CreateMainOrderRequest;
import com.cloud.order.dto.OrderAggregateResponse;
import com.cloud.order.entity.OrderItem;
import com.cloud.order.entity.OrderMain;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.service.OrderPlacementService;
import com.cloud.order.service.OrderService;
import com.cloud.order.service.support.StockReservationRemoteService;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrderPlacementServiceImpl implements OrderPlacementService {

    private static final Set<String> RESERVE_REQUIRED_STATUSES = Set.of("CREATED");
    private static final Set<String> RESERVE_COMPLETED_STATUSES = Set.of("STOCK_RESERVED", "PAID", "SHIPPED", "RECEIVED", "DONE");

    private final OrderService orderService;
    private final StockReservationRemoteService stockReservationRemoteService;

    @Override
    @GlobalTransactional(name = "order-create-with-stock-reserve", rollbackFor = Exception.class)
    public OrderAggregateResponse createOrder(CreateMainOrderRequest request) {
        OrderMain mainOrder = orderService.createMainOrder(request);
        OrderAggregateResponse aggregate = requireAggregate(mainOrder.getId());

        reserveStockIfNeeded(aggregate);
        markSubOrdersReserved(aggregate);

        return requireAggregate(mainOrder.getId());
    }

    private OrderAggregateResponse requireAggregate(Long mainOrderId) {
        OrderAggregateResponse aggregate = orderService.getOrderAggregate(mainOrderId);
        if (aggregate == null || aggregate.getMainOrder() == null) {
            throw new BusinessException("order aggregate not found");
        }
        if (aggregate.getSubOrders() == null || aggregate.getSubOrders().isEmpty()) {
            throw new BusinessException("order aggregate has no sub orders");
        }
        return aggregate;
    }

    private void reserveStockIfNeeded(OrderAggregateResponse aggregate) {
        for (OrderAggregateResponse.SubOrderWithItems wrapped : aggregate.getSubOrders()) {
            OrderSub subOrder = wrapped.getSubOrder();
            if (subOrder == null || !RESERVE_REQUIRED_STATUSES.contains(subOrder.getOrderStatus())) {
                continue;
            }
            reserveSubOrder(aggregate.getMainOrder(), subOrder, wrapped.getItems());
        }
    }

    private void reserveSubOrder(OrderMain mainOrder, OrderSub subOrder, List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException("order items are required for stock reservation");
        }

        Map<Long, Integer> skuQuantities = new LinkedHashMap<>();
        for (OrderItem item : items) {
            if (item == null || item.getSkuId() == null || item.getQuantity() == null) {
                throw new BusinessException("invalid order item for stock reservation");
            }
            skuQuantities.merge(item.getSkuId(), item.getQuantity(), Integer::sum);
        }

        for (Map.Entry<Long, Integer> entry : skuQuantities.entrySet()) {
            StockOperateCommandDTO command = new StockOperateCommandDTO();
            command.setSubOrderNo(subOrder.getSubOrderNo());
            command.setSkuId(entry.getKey());
            command.setQuantity(entry.getValue());
            command.setReason("reserve stock for " + mainOrder.getMainOrderNo());

            Boolean reserved = stockReservationRemoteService.reserve(command);
            if (!Boolean.TRUE.equals(reserved)) {
                throw new BusinessException("reserve stock failed for skuId=" + entry.getKey());
            }
        }
    }

    private void markSubOrdersReserved(OrderAggregateResponse aggregate) {
        for (OrderAggregateResponse.SubOrderWithItems wrapped : aggregate.getSubOrders()) {
            OrderSub subOrder = wrapped.getSubOrder();
            if (subOrder == null) {
                continue;
            }
            if (RESERVE_REQUIRED_STATUSES.contains(subOrder.getOrderStatus())) {
                orderService.advanceSubOrderStatus(subOrder.getId(), "RESERVE");
                continue;
            }
            if (!RESERVE_COMPLETED_STATUSES.contains(subOrder.getOrderStatus())) {
                throw new BusinessException("sub order status invalid for stock-reserved order: " + subOrder.getOrderStatus());
            }
        }
    }
}
