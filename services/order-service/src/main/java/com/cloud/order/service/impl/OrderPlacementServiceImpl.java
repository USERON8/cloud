package com.cloud.order.service.impl;

import cn.hutool.core.util.StrUtil;
import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import com.cloud.common.exception.BusinessException;
import com.cloud.order.dto.CreateMainOrderRequest;
import com.cloud.order.dto.OrderAggregateResponse;
import com.cloud.order.entity.OrderItem;
import com.cloud.order.entity.OrderMain;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.mapper.OrderItemMapper;
import com.cloud.order.mapper.OrderMainMapper;
import com.cloud.order.mapper.OrderSubMapper;
import com.cloud.order.service.OrderPlacementService;
import com.cloud.order.service.OrderService;
import com.cloud.order.service.support.StockReservationRemoteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.Queue;

@Service
@Slf4j
public class OrderPlacementServiceImpl implements OrderPlacementService {

    private static final Set<String> RESERVE_REQUIRED_STATUSES = Set.of("CREATED");
    private static final Set<String> RESERVE_COMPLETED_STATUSES = Set.of("STOCK_RESERVED", "PAID", "SHIPPED", "RECEIVED", "DONE");

    private final OrderMainMapper orderMainMapper;
    private final OrderSubMapper orderSubMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderService orderService;
    private final StockReservationRemoteService stockReservationRemoteService;
    private final TransactionOperations transactionOperations;
    private final Executor orderStockReservationExecutor;

    public OrderPlacementServiceImpl(OrderMainMapper orderMainMapper,
                                     OrderSubMapper orderSubMapper,
                                     OrderItemMapper orderItemMapper,
                                     OrderService orderService,
                                     StockReservationRemoteService stockReservationRemoteService,
                                     TransactionOperations transactionOperations,
                                     @Qualifier("orderStockReservationExecutor") Executor orderStockReservationExecutor) {
        this.orderMainMapper = orderMainMapper;
        this.orderSubMapper = orderSubMapper;
        this.orderItemMapper = orderItemMapper;
        this.orderService = orderService;
        this.stockReservationRemoteService = stockReservationRemoteService;
        this.transactionOperations = transactionOperations;
        this.orderStockReservationExecutor = orderStockReservationExecutor;
    }

    @Override
    public OrderAggregateResponse createOrder(CreateMainOrderRequest request) {
        OrderAggregateResponse aggregate = transactionOperations.execute(status -> createOrLoadAggregate(request));
        if (aggregate == null) {
            throw new BusinessException("failed to create order aggregate");
        }
        reserveStockIfNeeded(aggregate);
        return transactionOperations.execute(status -> {
            persistReservedStatuses(aggregate);
            return aggregate;
        });
    }

    private OrderAggregateResponse createOrLoadAggregate(CreateMainOrderRequest request) {
        String idempotencyKey = normalizeIdempotencyKey(request.getIdempotencyKey(), request.getUserId());
        OrderMain existing = orderMainMapper.selectActiveByIdempotencyKey(idempotencyKey);
        if (existing != null) {
            return loadExistingAggregate(existing.getId());
        }

        OrderMain mainOrder = buildMainOrder(request, idempotencyKey);
        try {
            orderMainMapper.insert(mainOrder);
        } catch (DuplicateKeyException duplicateKeyException) {
            OrderMain duplicated = orderMainMapper.selectActiveByIdempotencyKey(idempotencyKey);
            if (duplicated != null) {
                return loadExistingAggregate(duplicated.getId());
            }
            throw duplicateKeyException;
        }

        List<OrderAggregateResponse.SubOrderWithItems> wrappedSubOrders = new ArrayList<>(request.getSubOrders().size());
        for (CreateMainOrderRequest.CreateSubOrderRequest subRequest : request.getSubOrders()) {
            OrderSub subOrder = buildSubOrder(mainOrder, subRequest);
            orderSubMapper.insert(subOrder);

            List<OrderItem> items = new ArrayList<>(subRequest.getItems().size());
            for (CreateMainOrderRequest.CreateOrderItemRequest itemRequest : subRequest.getItems()) {
                OrderItem item = buildOrderItem(mainOrder, subOrder, itemRequest);
                orderItemMapper.insert(item);
                items.add(item);
            }

            OrderAggregateResponse.SubOrderWithItems wrapped = new OrderAggregateResponse.SubOrderWithItems();
            wrapped.setSubOrder(subOrder);
            wrapped.setItems(items);
            wrappedSubOrders.add(wrapped);
        }

        OrderAggregateResponse aggregate = new OrderAggregateResponse();
        aggregate.setMainOrder(mainOrder);
        aggregate.setSubOrders(wrappedSubOrders);
        return requireAggregate(aggregate);
    }

    private OrderAggregateResponse loadExistingAggregate(Long mainOrderId) {
        return requireAggregate(orderService.getOrderAggregate(mainOrderId));
    }

    private OrderAggregateResponse requireAggregate(OrderAggregateResponse aggregate) {
        if (aggregate == null || aggregate.getMainOrder() == null) {
            throw new BusinessException("order aggregate not found");
        }
        if (aggregate.getSubOrders() == null || aggregate.getSubOrders().isEmpty()) {
            throw new BusinessException("order aggregate has no sub orders");
        }
        return aggregate;
    }

    private void reserveStockIfNeeded(OrderAggregateResponse aggregate) {
        List<StockReservationTask> tasks = collectReservationTasks(aggregate);
        if (tasks.isEmpty()) {
            return;
        }

        Queue<StockReservationTask> reservedTasks = new ConcurrentLinkedQueue<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (List<StockReservationTask> skuTasks : groupTasksBySku(tasks).values()) {
            futures.add(CompletableFuture.runAsync(() -> reserveTaskGroup(skuTasks, reservedTasks), orderStockReservationExecutor));
        }

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (CompletionException ex) {
            releaseReservedTasks(reservedTasks);
            throw unwrapReservationException(ex);
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

    private Map<Long, List<StockReservationTask>> groupTasksBySku(List<StockReservationTask> tasks) {
        Map<Long, List<StockReservationTask>> grouped = new LinkedHashMap<>();
        for (StockReservationTask task : tasks) {
            grouped.computeIfAbsent(task.getSkuId(), ignored -> new ArrayList<>()).add(task);
        }
        return grouped;
    }

    private List<StockReservationTask> buildReservationTasks(OrderMain mainOrder, OrderSub subOrder, List<OrderItem> items) {
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

        List<StockReservationTask> tasks = new ArrayList<>(skuQuantities.size());
        for (Map.Entry<Long, Integer> entry : skuQuantities.entrySet()) {
            tasks.add(new StockReservationTask(
                    mainOrder.getMainOrderNo(),
                    subOrder.getSubOrderNo(),
                    entry.getKey(),
                    entry.getValue()
            ));
        }
        return tasks;
    }

    private void reserveTaskGroup(List<StockReservationTask> tasks, Queue<StockReservationTask> reservedTasks) {
        for (StockReservationTask task : tasks) {
            reserveTask(task, reservedTasks);
        }
    }

    private void reserveTask(StockReservationTask task, Queue<StockReservationTask> reservedTasks) {
        StockOperateCommandDTO command = new StockOperateCommandDTO();
        command.setSubOrderNo(task.getSubOrderNo());
        command.setOrderNo(task.getMainOrderNo());
        command.setSkuId(task.getSkuId());
        command.setQuantity(task.getQuantity());
        command.setReason("reserve stock for " + task.getMainOrderNo());

        Boolean reserved = stockReservationRemoteService.reserve(command);
        if (!Boolean.TRUE.equals(reserved)) {
            throw new BusinessException("reserve stock failed for skuId=" + task.getSkuId());
        }
        reservedTasks.add(task);
    }

    private void releaseReservedTasks(Queue<StockReservationTask> reservedTasks) {
        if (reservedTasks == null || reservedTasks.isEmpty()) {
            return;
        }
        for (StockReservationTask task : reservedTasks) {
            try {
                StockOperateCommandDTO command = new StockOperateCommandDTO();
                command.setSubOrderNo(task.getSubOrderNo());
                command.setOrderNo(task.getMainOrderNo());
                command.setSkuId(task.getSkuId());
                command.setQuantity(task.getQuantity());
                command.setReason("release reserved stock after reservation failure");
                stockReservationRemoteService.release(command);
            } catch (Exception ex) {
                log.error("Failed to release reserved stock after reservation failure, skuId={}", task.getSkuId(), ex);
            }
        }
    }

    private RuntimeException unwrapReservationException(Throwable ex) {
        Throwable cursor = ex;
        while (cursor instanceof CompletionException && cursor.getCause() != null) {
            cursor = cursor.getCause();
        }
        if (cursor instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new BusinessException("reserve stock failed", cursor);
    }

    private void persistReservedStatuses(OrderAggregateResponse aggregate) {
        boolean subOrderChanged = false;
        for (OrderAggregateResponse.SubOrderWithItems wrapped : aggregate.getSubOrders()) {
            OrderSub subOrder = wrapped.getSubOrder();
            if (subOrder == null) {
                continue;
            }
            if (RESERVE_REQUIRED_STATUSES.contains(subOrder.getOrderStatus())) {
                subOrder.setOrderStatus("STOCK_RESERVED");
                orderSubMapper.updateById(subOrder);
                subOrderChanged = true;
                continue;
            }
            if (!RESERVE_COMPLETED_STATUSES.contains(subOrder.getOrderStatus())) {
                throw new BusinessException("sub order status invalid for stock-reserved order: " + subOrder.getOrderStatus());
            }
        }
        if (!subOrderChanged) {
            return;
        }

        OrderMain mainOrder = aggregate.getMainOrder();
        String targetStatus = resolveMainOrderStatus(aggregate.getSubOrders());
        if (!Objects.equals(mainOrder.getOrderStatus(), targetStatus)) {
            mainOrder.setOrderStatus(targetStatus);
            orderMainMapper.updateById(mainOrder);
        }
    }

    private OrderMain buildMainOrder(CreateMainOrderRequest request, String idempotencyKey) {
        if (request.getSubOrders() == null || request.getSubOrders().isEmpty()) {
            throw new BusinessException("subOrders is required");
        }

        OrderMain mainOrder = new OrderMain();
        mainOrder.setMainOrderNo("M" + UUID.randomUUID().toString().replace("-", ""));
        mainOrder.setUserId(request.getUserId());
        mainOrder.setOrderStatus("CREATED");
        mainOrder.setTotalAmount(defaultAmount(request.getTotalAmount()));
        mainOrder.setPayableAmount(defaultAmount(request.getPayableAmount()));
        mainOrder.setRemark(request.getRemark());
        mainOrder.setIdempotencyKey(idempotencyKey);
        return mainOrder;
    }

    private OrderSub buildSubOrder(OrderMain mainOrder,
                                   CreateMainOrderRequest.CreateSubOrderRequest request) {
        OrderSub subOrder = new OrderSub();
        subOrder.setSubOrderNo("S" + UUID.randomUUID().toString().replace("-", ""));
        subOrder.setMainOrderId(mainOrder.getId());
        subOrder.setMerchantId(request.getMerchantId());
        subOrder.setOrderStatus("CREATED");
        subOrder.setShippingStatus("PENDING");
        subOrder.setAfterSaleStatus("NONE");
        subOrder.setItemAmount(defaultAmount(request.getItemAmount()));
        subOrder.setShippingFee(defaultAmount(request.getShippingFee()));
        subOrder.setDiscountAmount(defaultAmount(request.getDiscountAmount()));
        subOrder.setPayableAmount(defaultAmount(request.getPayableAmount()));
        subOrder.setReceiverName(request.getReceiverName());
        subOrder.setReceiverPhone(request.getReceiverPhone());
        subOrder.setReceiverAddress(request.getReceiverAddress());
        return subOrder;
    }

    private OrderItem buildOrderItem(OrderMain mainOrder,
                                     OrderSub subOrder,
                                     CreateMainOrderRequest.CreateOrderItemRequest request) {
        OrderItem item = new OrderItem();
        item.setMainOrderId(mainOrder.getId());
        item.setSubOrderId(subOrder.getId());
        item.setSpuId(request.getSpuId());
        item.setSkuId(request.getSkuId());
        item.setSkuCode(request.getSkuCode());
        item.setSkuName(request.getSkuName());
        item.setSkuSnapshot(request.getSkuSnapshot());
        item.setQuantity(request.getQuantity());
        item.setUnitPrice(defaultAmount(request.getUnitPrice()));
        item.setTotalPrice(defaultAmount(request.getTotalPrice()));
        return item;
    }

    private String normalizeIdempotencyKey(String idempotencyKey, Long userId) {
        if (StrUtil.isBlank(idempotencyKey)) {
            throw new BusinessException("idempotency key is required");
        }
        if (userId == null) {
            throw new BusinessException("user id is required for idempotency");
        }
        String trimmed = idempotencyKey.trim();
        String prefix = userId + ":";
        if (trimmed.startsWith(prefix)) {
            return trimmed;
        }
        return prefix + trimmed;
    }

    private String resolveMainOrderStatus(List<OrderAggregateResponse.SubOrderWithItems> subOrders) {
        boolean anyReserved = false;
        boolean anyPaidOrLater = false;
        boolean allDone = true;
        boolean allClosed = true;

        for (OrderAggregateResponse.SubOrderWithItems wrapped : subOrders) {
            OrderSub subOrder = wrapped.getSubOrder();
            if (subOrder == null) {
                continue;
            }
            String status = subOrder.getOrderStatus();
            if (!"DONE".equals(status)) {
                allDone = false;
            }
            if (!"CANCELLED".equals(status) && !"CLOSED".equals(status)) {
                allClosed = false;
            }
            if ("STOCK_RESERVED".equals(status)) {
                anyReserved = true;
            }
            if (Set.of("PAID", "SHIPPED", "RECEIVED", "DONE").contains(status)) {
                anyPaidOrLater = true;
            }
        }

        if (allDone) {
            return "DONE";
        }
        if (allClosed) {
            return "CANCELLED";
        }
        if (anyPaidOrLater) {
            return "PAID";
        }
        if (anyReserved) {
            return "STOCK_RESERVED";
        }
        return "CREATED";
    }

    private BigDecimal defaultAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private static final class StockReservationTask {
        private final String mainOrderNo;
        private final String subOrderNo;
        private final Long skuId;
        private final Integer quantity;

        private StockReservationTask(String mainOrderNo, String subOrderNo, Long skuId, Integer quantity) {
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
