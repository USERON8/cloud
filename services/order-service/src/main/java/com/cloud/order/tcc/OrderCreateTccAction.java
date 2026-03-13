package com.cloud.order.tcc;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.messaging.event.OrderTimeoutEvent;
import com.cloud.order.dto.CreateMainOrderRequest;
import com.cloud.order.entity.OrderItem;
import com.cloud.order.entity.OrderMain;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.entity.OrderTccLog;
import com.cloud.order.mapper.OrderItemMapper;
import com.cloud.order.mapper.OrderMainMapper;
import com.cloud.order.mapper.OrderSubMapper;
import com.cloud.order.mapper.OrderTccLogMapper;
import com.cloud.order.messaging.OrderTimeoutMessageProducer;
import com.cloud.order.service.support.OrderAggregateCacheService;
import org.apache.seata.rm.tcc.api.BusinessActionContext;
import org.apache.seata.rm.tcc.api.BusinessActionContextParameter;
import org.apache.seata.rm.tcc.api.LocalTCC;
import org.apache.seata.rm.tcc.api.TwoPhaseBusinessAction;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;



@LocalTCC
@Component
@RequiredArgsConstructor
public class OrderCreateTccAction {

    private static final String STATUS_TRY = "TRY";
    private static final String STATUS_CONFIRM = "CONFIRM";
    private static final String STATUS_CANCEL = "CANCEL";

    private static final Set<String> CANCELLABLE_STATUSES = Set.of("CREATED", "STOCK_RESERVED");

    private final OrderMainMapper orderMainMapper;
    private final OrderSubMapper orderSubMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderTccLogMapper orderTccLogMapper;
    private final OrderAggregateCacheService orderAggregateCacheService;
    private final OrderTimeoutMessageProducer orderTimeoutMessageProducer;

    @TwoPhaseBusinessAction(
            name = "orderCreateTcc",
            commitMethod = "commit",
            rollbackMethod = "rollback"
    )
    @Transactional(rollbackFor = Exception.class)
    public boolean prepare(BusinessActionContext actionContext,
                           @BusinessActionContextParameter(paramName = "idempotencyKey") String idempotencyKey,
                           CreateMainOrderRequest request) {
        if (StrUtil.isBlank(idempotencyKey)) {
            throw new BusinessException("idempotency key is required");
        }
        if (request == null || request.getSubOrders() == null || request.getSubOrders().isEmpty()) {
            throw new BusinessException("subOrders is required");
        }

        OrderTccLog existingLog = findLog(idempotencyKey);
        if (existingLog != null) {
            if (STATUS_CANCEL.equals(existingLog.getStatus())) {
                throw new BusinessException("order create tcc cancelled");
            }
            return true;
        }

        OrderTccLog logEntity = new OrderTccLog();
        logEntity.setBusinessKey(idempotencyKey);
        logEntity.setStatus(STATUS_TRY);
        orderTccLogMapper.insert(logEntity);

        OrderMain existing = orderMainMapper.selectActiveByIdempotencyKey(idempotencyKey);
        if (existing != null) {
            attachOrderInfo(logEntity, existing);
            return true;
        }

        OrderMain mainOrder = buildMainOrder(request, idempotencyKey);
        try {
            orderMainMapper.insert(mainOrder);
        } catch (DuplicateKeyException duplicateKeyException) {
            OrderMain duplicated = orderMainMapper.selectActiveByIdempotencyKey(idempotencyKey);
            if (duplicated != null) {
                attachOrderInfo(logEntity, duplicated);
                return true;
            }
            throw duplicateKeyException;
        }

        for (CreateMainOrderRequest.CreateSubOrderRequest subRequest : request.getSubOrders()) {
            OrderSub subOrder = buildSubOrder(mainOrder, subRequest);
            orderSubMapper.insert(subOrder);

            for (CreateMainOrderRequest.CreateOrderItemRequest itemRequest : subRequest.getItems()) {
                OrderItem item = buildOrderItem(mainOrder, subOrder, itemRequest);
                orderItemMapper.insert(item);
            }
        }

        attachOrderInfo(logEntity, mainOrder);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean commit(BusinessActionContext actionContext) {
        String idempotencyKey = resolveBusinessKey(actionContext);
        if (StrUtil.isBlank(idempotencyKey)) {
            return true;
        }
        OrderTccLog logEntity = findLog(idempotencyKey);
        if (logEntity == null) {
            return true;
        }
        if (STATUS_CONFIRM.equals(logEntity.getStatus()) || STATUS_CANCEL.equals(logEntity.getStatus())) {
            return true;
        }

        logEntity.setStatus(STATUS_CONFIRM);
        orderTccLogMapper.updateById(logEntity);

        OrderMain mainOrder = orderMainMapper.selectActiveByIdempotencyKey(idempotencyKey);
        if (mainOrder == null) {
            return true;
        }
        List<OrderSub> subOrders = orderSubMapper.listActiveByMainOrderId(mainOrder.getId());
        if (subOrders.isEmpty()) {
            return true;
        }
        for (OrderSub subOrder : subOrders) {
            if ("CREATED".equals(subOrder.getOrderStatus())) {
                subOrder.setOrderStatus("STOCK_RESERVED");
                orderSubMapper.updateById(subOrder);
            }
        }
        mainOrder.setOrderStatus("STOCK_RESERVED");
        orderMainMapper.updateById(mainOrder);
        orderAggregateCacheService.evict(mainOrder.getId());

        for (OrderSub subOrder : subOrders) {
            if (!"STOCK_RESERVED".equals(subOrder.getOrderStatus())) {
                continue;
            }
            OrderTimeoutEvent event = OrderTimeoutEvent.builder()
                    .subOrderId(subOrder.getId())
                    .subOrderNo(subOrder.getSubOrderNo())
                    .mainOrderNo(mainOrder.getMainOrderNo())
                    .userId(mainOrder.getUserId())
                    .build();
            orderTimeoutMessageProducer.sendAfterCommit(event);
        }
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean rollback(BusinessActionContext actionContext) {
        String idempotencyKey = resolveBusinessKey(actionContext);
        if (StrUtil.isBlank(idempotencyKey)) {
            return true;
        }
        OrderTccLog logEntity = findLog(idempotencyKey);
        if (logEntity == null) {
            OrderTccLog cancelLog = new OrderTccLog();
            cancelLog.setBusinessKey(idempotencyKey);
            cancelLog.setStatus(STATUS_CANCEL);
            orderTccLogMapper.insert(cancelLog);
            return true;
        }
        if (STATUS_CANCEL.equals(logEntity.getStatus())) {
            return true;
        }
        if (STATUS_CONFIRM.equals(logEntity.getStatus())) {
            return true;
        }

        logEntity.setStatus(STATUS_CANCEL);
        orderTccLogMapper.updateById(logEntity);

        OrderMain mainOrder = orderMainMapper.selectActiveByIdempotencyKey(idempotencyKey);
        if (mainOrder == null) {
            return true;
        }
        List<OrderSub> subOrders = orderSubMapper.listActiveByMainOrderId(mainOrder.getId());
        for (OrderSub subOrder : subOrders) {
            if (!CANCELLABLE_STATUSES.contains(subOrder.getOrderStatus())) {
                continue;
            }
            subOrder.setOrderStatus("CANCELLED");
            subOrder.setClosedAt(LocalDateTime.now());
            subOrder.setCloseReason("tcc rollback");
            orderSubMapper.updateById(subOrder);
        }
        mainOrder.setOrderStatus("CANCELLED");
        mainOrder.setCancelledAt(LocalDateTime.now());
        mainOrder.setCancelReason("tcc rollback");
        orderMainMapper.updateById(mainOrder);
        orderAggregateCacheService.evict(mainOrder.getId());
        return true;
    }

    private OrderTccLog findLog(String idempotencyKey) {
        return orderTccLogMapper.selectOne(new LambdaQueryWrapper<OrderTccLog>()
                .eq(OrderTccLog::getBusinessKey, idempotencyKey)
                .eq(OrderTccLog::getDeleted, 0)
                .last("LIMIT 1"));
    }

    private String resolveBusinessKey(BusinessActionContext actionContext) {
        if (actionContext == null) {
            return null;
        }
        Object value = actionContext.getActionContext("idempotencyKey");
        return value == null ? null : String.valueOf(value);
    }

    private void attachOrderInfo(OrderTccLog logEntity, OrderMain mainOrder) {
        if (logEntity == null || mainOrder == null) {
            return;
        }
        logEntity.setMainOrderId(mainOrder.getId());
        logEntity.setMainOrderNo(mainOrder.getMainOrderNo());
        orderTccLogMapper.updateById(logEntity);
    }

    private OrderMain buildMainOrder(CreateMainOrderRequest request, String idempotencyKey) {
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

    private BigDecimal defaultAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
