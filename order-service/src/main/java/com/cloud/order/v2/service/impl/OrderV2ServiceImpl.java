package com.cloud.order.v2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.common.exception.BusinessException;
import com.cloud.order.v2.dto.CreateMainOrderRequest;
import com.cloud.order.v2.entity.AfterSaleV2;
import com.cloud.order.v2.entity.OrderMainV2;
import com.cloud.order.v2.entity.OrderSubV2;
import com.cloud.order.v2.mapper.AfterSaleV2Mapper;
import com.cloud.order.v2.mapper.OrderMainV2Mapper;
import com.cloud.order.v2.mapper.OrderSubV2Mapper;
import com.cloud.order.v2.service.OrderV2Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    private final OrderMainV2Mapper orderMainV2Mapper;
    private final OrderSubV2Mapper orderSubV2Mapper;
    private final AfterSaleV2Mapper afterSaleV2Mapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderMainV2 createMainOrder(CreateMainOrderRequest request) {
        String idempotencyKey = request.getIdempotencyKey();
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new BusinessException("idempotency key is required");
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
        main.setTotalAmount(request.getTotalAmount());
        main.setPayableAmount(request.getPayableAmount());
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
        return main;
    }

    @Override
    public OrderMainV2 getMainOrder(Long mainOrderId) {
        return orderMainV2Mapper.selectById(mainOrderId);
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
        sub.setOrderStatus(targetStatus);
        if ("SHIPPED".equals(targetStatus)) {
            sub.setShippedAt(LocalDateTime.now());
        } else if ("RECEIVED".equals(targetStatus)) {
            sub.setReceivedAt(LocalDateTime.now());
        } else if ("DONE".equals(targetStatus)) {
            sub.setDoneAt(LocalDateTime.now());
        } else if ("CLOSED".equals(targetStatus)) {
            sub.setClosedAt(LocalDateTime.now());
        }
        orderSubV2Mapper.updateById(sub);
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
}
