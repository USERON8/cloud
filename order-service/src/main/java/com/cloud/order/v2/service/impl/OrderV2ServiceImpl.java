package com.cloud.order.v2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.common.exception.BusinessException;
import com.cloud.order.v2.dto.CreateMainOrderRequest;
import com.cloud.order.v2.dto.OrderAggregateResponse;
import com.cloud.order.v2.entity.AfterSaleV2;
import com.cloud.order.v2.entity.OrderItemV2;
import com.cloud.order.v2.entity.OrderMainV2;
import com.cloud.order.v2.entity.OrderSubV2;
import com.cloud.order.v2.mapper.AfterSaleV2Mapper;
import com.cloud.order.v2.mapper.OrderItemV2Mapper;
import com.cloud.order.v2.mapper.OrderMainV2Mapper;
import com.cloud.order.v2.mapper.OrderSubV2Mapper;
import com.cloud.order.v2.service.OrderV2Service;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrderV2ServiceImpl implements OrderV2Service {

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
            "RETURN", "RETURNED",
            "RECEIVE", "RECEIVED",
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

    private final OrderMainV2Mapper orderMainV2Mapper;
    private final OrderSubV2Mapper orderSubV2Mapper;
    private final OrderItemV2Mapper orderItemV2Mapper;
    private final AfterSaleV2Mapper afterSaleV2Mapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderMainV2 createMainOrder(CreateMainOrderRequest request) {
        String idempotencyKey = request.getIdempotencyKey();
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new BusinessException("idempotency key is required");
        }
        if (request.getSubOrders() == null || request.getSubOrders().isEmpty()) {
            throw new BusinessException("subOrders is required");
        }

        OrderMainV2 existing = orderMainV2Mapper.selectOne(
                new LambdaQueryWrapper<OrderMainV2>()
                        .eq(OrderMainV2::getIdempotencyKey, idempotencyKey.trim())
                        .eq(OrderMainV2::getDeleted, 0)
                        .last("LIMIT 1")
        );
        if (existing != null) {
            return existing;
        }

        OrderMainV2 main = new OrderMainV2();
        main.setMainOrderNo("M" + System.currentTimeMillis());
        main.setUserId(request.getUserId());
        main.setOrderStatus("CREATED");
        main.setTotalAmount(defaultAmount(request.getTotalAmount()));
        main.setPayableAmount(defaultAmount(request.getPayableAmount()));
        main.setRemark(request.getRemark());
        main.setIdempotencyKey(idempotencyKey.trim());

        try {
            orderMainV2Mapper.insert(main);
        } catch (DuplicateKeyException duplicateKeyException) {
            OrderMainV2 duplicated = orderMainV2Mapper.selectOne(
                    new LambdaQueryWrapper<OrderMainV2>()
                            .eq(OrderMainV2::getIdempotencyKey, idempotencyKey.trim())
                            .eq(OrderMainV2::getDeleted, 0)
                            .last("LIMIT 1")
            );
            if (duplicated != null) {
                return duplicated;
            }
            throw duplicateKeyException;
        }

        long seq = 1;
        for (CreateMainOrderRequest.CreateSubOrderRequest subRequest : request.getSubOrders()) {
            OrderSubV2 sub = new OrderSubV2();
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
            orderSubV2Mapper.insert(sub);

            for (CreateMainOrderRequest.CreateOrderItemRequest itemRequest : subRequest.getItems()) {
                OrderItemV2 item = new OrderItemV2();
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
                orderItemV2Mapper.insert(item);
            }
        }
        return main;
    }

    @Override
    public OrderMainV2 getMainOrder(Long mainOrderId) {
        return orderMainV2Mapper.selectById(mainOrderId);
    }

    @Override
    public OrderAggregateResponse getOrderAggregate(Long mainOrderId) {
        OrderMainV2 main = orderMainV2Mapper.selectById(mainOrderId);
        if (main == null || Integer.valueOf(1).equals(main.getDeleted())) {
            return null;
        }
        List<OrderSubV2> subOrders = listSubOrders(mainOrderId);

        OrderAggregateResponse response = new OrderAggregateResponse();
        response.setMainOrder(main);
        List<OrderAggregateResponse.SubOrderWithItems> wrapped = new ArrayList<>(subOrders.size());

        for (OrderSubV2 subOrder : subOrders) {
            List<OrderItemV2> items = orderItemV2Mapper.selectList(new LambdaQueryWrapper<OrderItemV2>()
                    .eq(OrderItemV2::getSubOrderId, subOrder.getId())
                    .eq(OrderItemV2::getDeleted, 0));
            OrderAggregateResponse.SubOrderWithItems item = new OrderAggregateResponse.SubOrderWithItems();
            item.setSubOrder(subOrder);
            item.setItems(items);
            wrapped.add(item);
        }
        response.setSubOrders(wrapped);
        return response;
    }

    @Override
    public List<OrderSubV2> listSubOrders(Long mainOrderId) {
        return orderSubV2Mapper.selectList(
                new LambdaQueryWrapper<OrderSubV2>()
                        .eq(OrderSubV2::getMainOrderId, mainOrderId)
                        .eq(OrderSubV2::getDeleted, 0)
        );
    }

    @Override
    public OrderSubV2 getSubOrder(Long subOrderId) {
        return orderSubV2Mapper.selectById(subOrderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderSubV2 advanceSubOrderStatus(Long subOrderId, String action) {
        OrderSubV2 sub = orderSubV2Mapper.selectById(subOrderId);
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
        orderSubV2Mapper.updateById(sub);
        refreshMainOrderStatus(sub.getMainOrderId());
        return sub;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AfterSaleV2 applyAfterSale(AfterSaleV2 afterSale) {
        afterSale.setAfterSaleNo("AS" + System.currentTimeMillis());
        if (afterSale.getStatus() == null) {
            afterSale.setStatus("APPLIED");
        }
        afterSaleV2Mapper.insert(afterSale);
        return afterSale;
    }

    @Override
    public AfterSaleV2 getAfterSale(Long afterSaleId) {
        return afterSaleV2Mapper.selectById(afterSaleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AfterSaleV2 advanceAfterSaleStatus(Long afterSaleId, String action, String remark) {
        AfterSaleV2 afterSale = afterSaleV2Mapper.selectById(afterSaleId);
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
        afterSaleV2Mapper.updateById(afterSale);
        return afterSale;
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
        OrderMainV2 mainOrder = orderMainV2Mapper.selectById(mainOrderId);
        if (mainOrder == null || Integer.valueOf(1).equals(mainOrder.getDeleted())) {
            return;
        }
        List<OrderSubV2> subOrders = listSubOrders(mainOrderId);
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
        orderMainV2Mapper.updateById(mainOrder);
    }

    private BigDecimal defaultAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
