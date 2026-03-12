package com.cloud.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
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
import com.cloud.order.messaging.OrderMessageProducer;
import com.cloud.order.service.OrderService;
import com.cloud.order.service.support.PaymentOrderRemoteService;
import com.cloud.order.service.support.StockReservationRemoteService;
import com.cloud.common.metrics.TradeMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cn.hutool.core.util.StrUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.cloud.common.domain.dto.payment.PaymentRefundCommandDTO;
import com.cloud.common.domain.vo.payment.PaymentOrderVO;

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
    private final StockReservationRemoteService stockReservationRemoteService;
    private final PaymentOrderRemoteService paymentOrderRemoteService;
    private final OrderMessageProducer orderMessageProducer;
    private final TradeMetrics tradeMetrics;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderMain createMainOrder(CreateMainOrderRequest request) {
        try {
            String idempotencyKey = buildScopedIdempotencyKey(request.getUserId(), request.getIdempotencyKey());
            if (request.getSubOrders() == null || request.getSubOrders().isEmpty()) {
                throw new BusinessException("subOrders is required");
            }

            OrderMain existing = findActiveMainOrderByIdempotencyKey(idempotencyKey);
            if (existing != null) {
                tradeMetrics.incrementOrder("success");
                return existing;
            }

            OrderMain main = new OrderMain();
            main.setMainOrderNo("M" + System.currentTimeMillis());
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
            List<Long> subOrderIds = subOrders.stream()
                    .map(OrderSub::getId)
                    .filter(id -> id != null)
                    .toList();
            if (!subOrderIds.isEmpty()) {
                List<OrderItem> items = orderItemMapper.listActiveBySubOrderIds(subOrderIds);
                for (OrderItem item : items) {
                    if (item.getSubOrderId() == null) {
                        continue;
                    }
                    itemsBySubOrder.computeIfAbsent(item.getSubOrderId(), ignored -> new ArrayList<>()).add(item);
                }
            }
        }

        for (OrderSub subOrder : subOrders) {
            List<OrderItem> items = itemsBySubOrder.getOrDefault(subOrder.getId(), List.of());
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
        return orderSubMapper.listActiveByMainOrderId(mainOrderId);
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

        if (requiresStockRelease(sub.getOrderStatus(), targetStatus)) {
            releaseReservedStock(sub);
        }

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
        boolean refundProcess = false;
        boolean refundSuccess = false;
        try {
            AfterSale afterSale = afterSaleMapper.selectById(afterSaleId);
            if (afterSale == null || afterSale.getDeleted() == 1) {
                throw new BusinessException("after sale not found");
            }
            String targetStatus = AFTER_SALE_ACTION_TARGET.get(action.toUpperCase());
            if (targetStatus == null) {
                throw new BusinessException("unsupported after-sale action: " + action);
            }
            validateAfterSaleTransition(afterSale.getStatus(), targetStatus);
            refundProcess = "PROCESS".equalsIgnoreCase(action);
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

            if (refundProcess) {
                dispatchRefundProcess(afterSale, remark);
            }
            if (refundSuccess) {
                tradeMetrics.incrementRefund("success");
            }
            return afterSale;
        } catch (Exception ex) {
            if (refundProcess) {
                tradeMetrics.incrementRefund("failed");
            }
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

    private void validateSubTransition(String current, String target) {
        Set<String> allowed = SUB_STATUS_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(target)) {
            throw new BusinessException("invalid order status transition: " + current + " -> " + target);
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
        boolean anyReserved = subOrders.stream().anyMatch(s -> "STOCK_RESERVED".equals(s.getOrderStatus()));
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
        } else if (anyReserved) {
            mainOrder.setOrderStatus("STOCK_RESERVED");
        } else {
            mainOrder.setOrderStatus("CREATED");
        }
        orderMainMapper.updateById(mainOrder);
    }

    private void dispatchRefundProcess(AfterSale afterSale, String remark) {
        if (afterSale == null) {
            return;
        }
        OrderMain mainOrder = requireOrderMain(afterSale.getMainOrderId());
        OrderSub subOrder = requireSubOrder(afterSale.getSubOrderId());
        PaymentOrderVO paymentOrder = paymentOrderRemoteService.getPaymentOrderByOrderNo(
                mainOrder.getMainOrderNo(), subOrder.getSubOrderNo()
        );
        if (paymentOrder == null) {
            throw new BusinessException("payment order not found for refund process");
        }

        PaymentRefundCommandDTO command = new PaymentRefundCommandDTO();
        command.setRefundNo(buildRefundNo(afterSale));
        command.setPaymentNo(paymentOrder.getPaymentNo());
        command.setAfterSaleNo(afterSale.getAfterSaleNo());
        command.setRefundAmount(resolveRefundAmount(afterSale));
        command.setReason(buildRefundReason(afterSale, remark));
        command.setIdempotencyKey("after-sale:refund:" + afterSale.getAfterSaleNo());

        boolean sent = orderMessageProducer.sendRefundProcessEvent(command);
        if (!sent) {
            throw new BusinessException("failed to dispatch refund process event");
        }
    }

    private OrderMain requireOrderMain(Long mainOrderId) {
        if (mainOrderId == null) {
            throw new BusinessException("main order id is required");
        }
        OrderMain mainOrder = orderMainMapper.selectById(mainOrderId);
        if (mainOrder == null || Integer.valueOf(1).equals(mainOrder.getDeleted())) {
            throw new BusinessException("main order not found");
        }
        return mainOrder;
    }

    private OrderSub requireSubOrder(Long subOrderId) {
        if (subOrderId == null) {
            throw new BusinessException("sub order id is required");
        }
        OrderSub subOrder = orderSubMapper.selectById(subOrderId);
        if (subOrder == null || Integer.valueOf(1).equals(subOrder.getDeleted())) {
            throw new BusinessException("sub order not found");
        }
        return subOrder;
    }

    private String buildRefundNo(AfterSale afterSale) {
        return "RF" + afterSale.getAfterSaleNo();
    }

    private BigDecimal resolveRefundAmount(AfterSale afterSale) {
        if (afterSale.getApprovedAmount() != null && afterSale.getApprovedAmount().compareTo(BigDecimal.ZERO) > 0) {
            return afterSale.getApprovedAmount();
        }
        if (afterSale.getApplyAmount() != null) {
            return afterSale.getApplyAmount();
        }
        return BigDecimal.ZERO;
    }

    private String buildRefundReason(AfterSale afterSale, String remark) {
        if (remark != null && !remark.isBlank()) {
            return remark;
        }
        if (afterSale.getReason() != null && !afterSale.getReason().isBlank()) {
            return afterSale.getReason();
        }
        return "after-sale refund";
    }

    private BigDecimal defaultAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private String buildScopedIdempotencyKey(Long userId, String idempotencyKey) {
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
}


