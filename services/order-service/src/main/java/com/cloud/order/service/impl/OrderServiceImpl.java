package com.cloud.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.common.exception.BusinessException;
import com.cloud.order.dto.CreateMainOrderRequest;
import com.cloud.order.dto.OrderAggregateResponse;
import com.cloud.order.entity.AfterSale;
import com.cloud.order.entity.OrderItem;
import com.cloud.order.entity.OrderMain;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.mapper.AfterSaleMapper;
import com.cloud.order.mapper.OrderItemMapper;
import com.cloud.order.mapper.OrderMainMapper;
import com.cloud.order.mapper.OrderSubMapper;
import com.cloud.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cn.hutool.core.util.StrUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final Map<String, String> SUB_ACTION_TARGET = Map.of(
            "RESERVE", "STOCK_RESERVED",
            "PAY", "PAID",
            "SHIP", "SHIPPED",
            "RECEIVE", "RECEIVED",
            "DONE", "DONE",
            "CANCEL", "CANCELLED",
            "CLOSE", "CLOSED"
    );

    private static final Map<String, Set<String>> SUB_STATUS_TRANSITIONS = Map.of(
            "CREATED", Set.of("STOCK_RESERVED", "CANCELLED", "CLOSED"),
            "STOCK_RESERVED", Set.of("PAID", "CANCELLED", "CLOSED"),
            "PAID", Set.of("SHIPPED", "CANCELLED", "CLOSED"),
            "SHIPPED", Set.of("RECEIVED", "CLOSED"),
            "RECEIVED", Set.of("DONE", "CLOSED"),
            "DONE", Set.of(),
            "CANCELLED", Set.of(),
            "CLOSED", Set.of()
    );

    private static final Map<String, String> AFTER_SALE_ACTION_TARGET = Map.of(
            "AUDIT", "AUDITING",
            "APPROVE", "APPROVED",
            "REJECT", "REJECTED",
            "WAIT_RETURN", "WAIT_RETURN",
            "RETURN", "RETURNED",
            "RECEIVE", "RECEIVED",
            "PROCESS", "REFUNDING",
            "REFUND", "REFUNDED",
            "CANCEL", "CANCELLED",
            "CLOSE", "CLOSED"
    );

    private static final Map<String, Set<String>> AFTER_SALE_TRANSITIONS = Map.ofEntries(
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
            Map.entry("CLOSED", Set.of())
    );

    private final OrderMainMapper orderMainMapper;
    private final OrderSubMapper orderSubMapper;
    private final OrderItemMapper orderItemMapper;
    private final AfterSaleMapper afterSaleMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderMain createMainOrder(CreateMainOrderRequest request) {
        String idempotencyKey = request.getIdempotencyKey();
        if (StrUtil.isBlank(idempotencyKey)) {
            throw new BusinessException("idempotency key is required");
        }
        if (request.getSubOrders() == null || request.getSubOrders().isEmpty()) {
            throw new BusinessException("subOrders is required");
        }

        OrderMain existing = orderMainMapper.selectOne(
                new LambdaQueryWrapper<OrderMain>()
                        .eq(OrderMain::getIdempotencyKey, idempotencyKey.trim())
                        .eq(OrderMain::getDeleted, 0)
                        .last("LIMIT 1")
        );
        if (existing != null) {
            return existing;
        }

        OrderMain main = new OrderMain();
        main.setMainOrderNo("M" + System.currentTimeMillis());
        main.setUserId(request.getUserId());
        main.setOrderStatus("CREATED");
        main.setTotalAmount(defaultAmount(request.getTotalAmount()));
        main.setPayableAmount(defaultAmount(request.getPayableAmount()));
        main.setRemark(request.getRemark());
        main.setIdempotencyKey(idempotencyKey.trim());

        try {
            orderMainMapper.insert(main);
        } catch (DuplicateKeyException duplicateKeyException) {
            OrderMain duplicated = orderMainMapper.selectOne(
                    new LambdaQueryWrapper<OrderMain>()
                            .eq(OrderMain::getIdempotencyKey, idempotencyKey.trim())
                            .eq(OrderMain::getDeleted, 0)
                            .last("LIMIT 1")
            );
            if (duplicated != null) {
                return duplicated;
            }
            throw duplicateKeyException;
        }

        long seq = 1;
        for (CreateMainOrderRequest.CreateSubOrderRequest subRequest : request.getSubOrders()) {
            OrderSub sub = new OrderSub();
            sub.setSubOrderNo("S" + System.currentTimeMillis() + seq++);
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
        return main;
    }

    @Override
    public OrderMain getMainOrder(Long mainOrderId) {
        return orderMainMapper.selectById(mainOrderId);
    }

    @Override
    public OrderAggregateResponse getOrderAggregate(Long mainOrderId) {
        OrderMain main = orderMainMapper.selectById(mainOrderId);
        if (main == null || Integer.valueOf(1).equals(main.getDeleted())) {
            return null;
        }
        List<OrderSub> subOrders = listSubOrders(mainOrderId);

        OrderAggregateResponse response = new OrderAggregateResponse();
        response.setMainOrder(main);
        List<OrderAggregateResponse.SubOrderWithItems> wrapped = new ArrayList<>(subOrders.size());

        for (OrderSub subOrder : subOrders) {
            List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                    .eq(OrderItem::getSubOrderId, subOrder.getId())
                    .eq(OrderItem::getDeleted, 0));
            OrderAggregateResponse.SubOrderWithItems item = new OrderAggregateResponse.SubOrderWithItems();
            item.setSubOrder(subOrder);
            item.setItems(items);
            wrapped.add(item);
        }
        response.setSubOrders(wrapped);
        return response;
    }

    @Override
    public List<OrderSub> listSubOrders(Long mainOrderId) {
        return orderSubMapper.selectList(
                new LambdaQueryWrapper<OrderSub>()
                        .eq(OrderSub::getMainOrderId, mainOrderId)
                        .eq(OrderSub::getDeleted, 0)
        );
    }

    @Override
    public OrderSub getSubOrder(Long subOrderId) {
        return orderSubMapper.selectById(subOrderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderSub advanceSubOrderStatus(Long subOrderId, String action) {
        OrderSub sub = orderSubMapper.selectById(subOrderId);
        if (sub == null || sub.getDeleted() == 1) {
            throw new BusinessException("sub order not found");
        }
        String targetStatus = SUB_ACTION_TARGET.get(action.toUpperCase());
        if (targetStatus == null) {
            throw new BusinessException("unsupported order action: " + action);
        }
        validateSubTransition(sub.getOrderStatus(), targetStatus);

        sub.setOrderStatus(targetStatus);
        if ("SHIPPED".equals(targetStatus)) {
            sub.setShippedAt(LocalDateTime.now());
        } else if ("RECEIVED".equals(targetStatus)) {
            sub.setReceivedAt(LocalDateTime.now());
        } else if ("DONE".equals(targetStatus)) {
            sub.setDoneAt(LocalDateTime.now());
        } else if ("CLOSED".equals(targetStatus) || "CANCELLED".equals(targetStatus)) {
            sub.setClosedAt(LocalDateTime.now());
        }
        orderSubMapper.updateById(sub);
        refreshMainOrderStatus(sub.getMainOrderId());
        return sub;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AfterSale applyAfterSale(AfterSale afterSale) {
        afterSale.setAfterSaleNo("AS" + System.currentTimeMillis());
        if (afterSale.getStatus() == null) {
            afterSale.setStatus("APPLIED");
        }
        afterSaleMapper.insert(afterSale);
        syncSubOrderAfterSaleStatus(afterSale.getSubOrderId(), afterSale.getStatus());
        return afterSale;
    }

    @Override
    public AfterSale getAfterSale(Long afterSaleId) {
        return afterSaleMapper.selectById(afterSaleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AfterSale advanceAfterSaleStatus(Long afterSaleId, String action, String remark) {
        AfterSale afterSale = afterSaleMapper.selectById(afterSaleId);
        if (afterSale == null || afterSale.getDeleted() == 1) {
            throw new BusinessException("after sale not found");
        }
        String targetStatus = AFTER_SALE_ACTION_TARGET.get(action.toUpperCase());
        if (targetStatus == null) {
            throw new BusinessException("unsupported after-sale action: " + action);
        }
        validateAfterSaleTransition(afterSale.getStatus(), targetStatus);

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
        return afterSale;
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

    private void validateSubTransition(String current, String target) {
        Set<String> allowed = SUB_STATUS_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(target)) {
            throw new BusinessException("invalid order status transition: " + current + " -> " + target);
        }
    }

    private void validateAfterSaleTransition(String current, String target) {
        Set<String> allowed = AFTER_SALE_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(target)) {
            throw new BusinessException("invalid after-sale status transition: " + current + " -> " + target);
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
        boolean allClosed = subOrders.stream().allMatch(s -> "CANCELLED".equals(s.getOrderStatus()) || "CLOSED".equals(s.getOrderStatus()));
        boolean anyPaidOrLater = subOrders.stream().anyMatch(s -> Set.of("PAID", "SHIPPED", "RECEIVED", "DONE").contains(s.getOrderStatus()));

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
        } else {
            mainOrder.setOrderStatus("CREATED");
        }
        orderMainMapper.updateById(mainOrder);
    }

    private BigDecimal defaultAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}


