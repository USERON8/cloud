package com.cloud.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.common.exception.BusinessException;
import com.cloud.order.service.OrderTimeoutService;
import com.cloud.order.v2.entity.OrderMainV2;
import com.cloud.order.v2.entity.OrderSubV2;
import com.cloud.order.v2.mapper.OrderMainV2Mapper;
import com.cloud.order.v2.mapper.OrderSubV2Mapper;
import com.cloud.order.v2.service.OrderV2Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderTimeoutServiceImpl implements OrderTimeoutService {

    private final OrderSubV2Mapper orderSubV2Mapper;
    private final OrderMainV2Mapper orderMainV2Mapper;
    private final OrderV2Service orderV2Service;

    @Value("${order.timeout.minutes:30}")
    private Integer timeoutMinutes;

    @Override
    public int checkAndHandleTimeoutOrders() {
        List<Long> timeoutSubOrderIds = getTimeoutSubOrderIds(timeoutMinutes);
        if (timeoutSubOrderIds.isEmpty()) {
            return 0;
        }
        return batchCancelTimeoutOrders(timeoutSubOrderIds);
    }

    @Override
    public List<Long> getTimeoutSubOrderIds(Integer timeoutMinutes) {
        int effectiveTimeout = (timeoutMinutes == null || timeoutMinutes <= 0)
                ? this.timeoutMinutes
                : timeoutMinutes;

        LocalDateTime timeoutPoint = LocalDateTime.now().minusMinutes(effectiveTimeout);
        return orderSubV2Mapper.selectList(
                new LambdaQueryWrapper<OrderSubV2>()
                        .eq(OrderSubV2::getOrderStatus, "CREATED")
                        .lt(OrderSubV2::getCreatedAt, timeoutPoint)
                        .eq(OrderSubV2::getDeleted, 0)
                        .orderByAsc(OrderSubV2::getCreatedAt)
        ).stream().map(OrderSubV2::getId).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelTimeoutOrder(Long subOrderId) {
        try {
            OrderSubV2 updated = orderV2Service.advanceSubOrderStatus(subOrderId, "CANCEL");
            if (updated == null) {
                return false;
            }
            refreshMainOrderStatusIfAllSubsClosed(updated.getMainOrderId());
            return true;
        } catch (Exception e) {
            log.error("Cancel timeout order failed: subOrderId={}", subOrderId, e);
            throw new BusinessException("Cancel timeout order failed", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchCancelTimeoutOrders(List<Long> subOrderIds) {
        if (subOrderIds == null || subOrderIds.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        for (Long subOrderId : subOrderIds) {
            try {
                if (cancelTimeoutOrder(subOrderId)) {
                    successCount++;
                }
            } catch (Exception e) {
                log.warn("Skip timeout order cancel failure: subOrderId={}", subOrderId);
            }
        }
        return successCount;
    }

    @Override
    public Integer getTimeoutConfig() {
        return timeoutMinutes;
    }

    @Override
    public boolean updateTimeoutConfig(Integer timeoutMinutes) {
        if (timeoutMinutes == null || timeoutMinutes <= 0) {
            throw new BusinessException("timeoutMinutes must be greater than 0");
        }
        this.timeoutMinutes = timeoutMinutes;
        return true;
    }

    private void refreshMainOrderStatusIfAllSubsClosed(Long mainOrderId) {
        if (mainOrderId == null) {
            return;
        }
        List<OrderSubV2> remainingActiveSubs = orderSubV2Mapper.selectList(
                new LambdaQueryWrapper<OrderSubV2>()
                        .eq(OrderSubV2::getMainOrderId, mainOrderId)
                        .eq(OrderSubV2::getDeleted, 0)
                        .in(OrderSubV2::getOrderStatus, List.of("CREATED", "STOCK_RESERVED", "PAID", "SHIPPED", "RECEIVED"))
        );
        if (!remainingActiveSubs.isEmpty()) {
            return;
        }
        OrderMainV2 mainOrder = orderMainV2Mapper.selectById(mainOrderId);
        if (mainOrder == null || Integer.valueOf(1).equals(mainOrder.getDeleted())) {
            return;
        }
        mainOrder.setOrderStatus("CANCELLED");
        mainOrder.setCancelledAt(LocalDateTime.now());
        orderMainV2Mapper.updateById(mainOrder);
    }
}
