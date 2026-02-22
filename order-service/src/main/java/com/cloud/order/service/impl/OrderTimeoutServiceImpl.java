package com.cloud.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.common.exception.BusinessException;
import com.cloud.order.module.entity.Order;
import com.cloud.order.service.OrderService;
import com.cloud.order.service.OrderTimeoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderTimeoutServiceImpl implements OrderTimeoutService {

    private final OrderService orderService;

    @Value("${order.timeout.minutes:30}")
    private Integer timeoutMinutes;

    @Override
    public int checkAndHandleTimeoutOrders() {
        List<Order> timeoutOrders = getTimeoutOrders(timeoutMinutes);
        if (timeoutOrders.isEmpty()) {
            return 0;
        }

        List<Long> orderIds = timeoutOrders.stream()
                .map(Order::getId)
                .collect(Collectors.toList());
        return batchCancelTimeoutOrders(orderIds);
    }

    @Override
    public List<Order> getTimeoutOrders(Integer timeoutMinutes) {
        int effectiveTimeout = (timeoutMinutes == null || timeoutMinutes <= 0)
                ? this.timeoutMinutes
                : timeoutMinutes;

        LocalDateTime timeoutPoint = LocalDateTime.now().minusMinutes(effectiveTimeout);
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getStatus, 0)
                .lt(Order::getCreatedAt, timeoutPoint)
                .orderByAsc(Order::getCreatedAt);

        return orderService.list(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelTimeoutOrder(Long orderId) {
        try {
            return orderService.cancelOrder(orderId);
        } catch (Exception e) {
            log.error("Cancel timeout order failed: orderId={}", orderId, e);
            throw new BusinessException("Cancel timeout order failed", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchCancelTimeoutOrders(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        for (Long orderId : orderIds) {
            try {
                if (cancelTimeoutOrder(orderId)) {
                    successCount++;
                }
            } catch (Exception e) {
                log.warn("Skip timeout order cancel failure: orderId={}", orderId);
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
}
