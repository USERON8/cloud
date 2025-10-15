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
import java.util.ArrayList;
import java.util.List;

/**
 * 订单超时处理服务实现
 *
 * @author what's up
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderTimeoutServiceImpl implements OrderTimeoutService {

    private final OrderService orderService;

    // 从配置文件读取超时时间(分钟),默认30分钟
    @Value("${order.timeout.minutes:30}")
    private Integer timeoutMinutes;

    @Override
    public int checkAndHandleTimeoutOrders() {
        log.info("⏰ 开始检查超时未支付订单, 超时时间: {}分钟", timeoutMinutes);

        try {
            // 获取超时订单列表
            List<Order> timeoutOrders = getTimeoutOrders(timeoutMinutes);

            if (timeoutOrders.isEmpty()) {
                log.info("当前没有超时未支付订单");
                return 0;
            }

            log.warn("⚠️ 发现{}个超时未支付订单", timeoutOrders.size());

            // 批量取消超时订单
            List<Long> orderIds = timeoutOrders.stream()
                    .map(Order::getId)
                    .toList();

            int cancelCount = batchCancelTimeoutOrders(orderIds);

            log.info("✅ 超时订单处理完成, 取消订单数: {}", cancelCount);
            return cancelCount;

        } catch (Exception e) {
            log.error("❌ 检查超时订单失败", e);
            throw new BusinessException("检查超时订单失败", e);
        }
    }

    @Override
    public List<Order> getTimeoutOrders(Integer timeoutMinutes) {
        if (timeoutMinutes == null || timeoutMinutes <= 0) {
            timeoutMinutes = this.timeoutMinutes;
        }

        // 计算超时时间点
        LocalDateTime timeoutTime = LocalDateTime.now().minusMinutes(timeoutMinutes);

        log.info("查询超时未支付订单, 超时时间点: {}", timeoutTime);

        // 查询待支付状态且创建时间早于超时时间点的订单
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getOrderStatus, "PENDING")  // 待支付状态
                .lt(Order::getCreatedAt, timeoutTime)      // 创建时间早于超时时间点
                .orderByAsc(Order::getCreatedAt);

        List<Order> timeoutOrders = orderService.list(queryWrapper);

        log.info("查询到超时未支付订单数量: {}", timeoutOrders.size());
        return timeoutOrders;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelTimeoutOrder(Long orderId) {
        log.info("取消超时订单, orderId: {}", orderId);

        try {
            // 调用订单服务的取消方法
            boolean success = orderService.cancelOrder(orderId);

            if (success) {
                log.info("超时订单取消成功, orderId: {}", orderId);

                // TODO: 发送RocketMQ通知消息
                // TODO: 释放库存
                // TODO: 通知用户订单已取消
            } else {
                log.warn("超时订单取消失败, orderId: {}", orderId);
            }

            return success;

        } catch (Exception e) {
            log.error("取消超时订单异常, orderId: {}", orderId, e);
            throw new BusinessException("取消超时订单失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchCancelTimeoutOrders(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return 0;
        }

        log.info("批量取消超时订单, 数量: {}", orderIds.size());

        int successCount = 0;
        List<Long> failedOrderIds = new ArrayList<>();

        for (Long orderId : orderIds) {
            try {
                boolean success = cancelTimeoutOrder(orderId);
                if (success) {
                    successCount++;
                } else {
                    failedOrderIds.add(orderId);
                }
            } catch (Exception e) {
                log.error("取消订单失败, orderId: {}", orderId, e);
                failedOrderIds.add(orderId);
            }
        }

        if (!failedOrderIds.isEmpty()) {
            log.warn("部分订单取消失败, 失败订单ID: {}", failedOrderIds);
        }

        log.info("批量取消超时订单完成, 成功: {}, 失败: {}", successCount, failedOrderIds.size());
        return successCount;
    }

    @Override
    public Integer getTimeoutConfig() {
        return timeoutMinutes;
    }

    @Override
    public boolean updateTimeoutConfig(Integer timeoutMinutes) {
        if (timeoutMinutes == null || timeoutMinutes <= 0) {
            throw new BusinessException("超时时间必须大于0");
        }

        this.timeoutMinutes = timeoutMinutes;
        log.info("订单超时配置已更新: {}分钟", timeoutMinutes);

        // TODO: 可以将配置持久化到数据库或配置中心
        return true;
    }
}
