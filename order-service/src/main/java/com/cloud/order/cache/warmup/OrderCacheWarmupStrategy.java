package com.cloud.order.cache.warmup;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.order.mapper.OrderMapper;
import com.cloud.order.module.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单缓存预热策略
 * <p>
 * 预热内容:
 * 1. 最近24小时的待支付订单 (status = 0, 按创建时间倒序)
 * 2. 最近7天的处理中订单 (status in [1,2,3], 按更新时间倒序, 取前100条)
 * <p>
 * 预热目标缓存:
 * - orderInfo: 订单信息缓存(15分钟TTL)
 *
 * @author CloudDevAgent
 * @since 2025-10-12
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderCacheWarmupStrategy implements com.cloud.common.cache.warmup.CacheWarmupStrategy {

    private final OrderMapper orderMapper;

    @Override
    public int warmup(CacheManager cacheManager) {
        int warmedUpCount = 0;

        try {
            log.info("开始执行订单缓存预热策略");

            // 1. 获取缓存实例
            Cache orderInfoCache = cacheManager.getCache("orderInfo");
            if (orderInfoCache == null) {
                log.warn("未找到 orderInfo 缓存实例,跳过预热");
                return 0;
            }

            // 2. 预热最近24小时的待支付订单
            warmedUpCount += warmupPendingOrders(orderInfoCache);

            // 3. 预热最近7天的处理中订单
            warmedUpCount += warmupProcessingOrders(orderInfoCache);

            log.info("订单缓存预热完成: 成功预热 {} 个订单", warmedUpCount);
            return warmedUpCount;

        } catch (Exception e) {
            log.error("订单缓存预热失败", e);
            return warmedUpCount;
        }
    }

    /**
     * 预热待支付订单 (最近24小时)
     */
    private int warmupPendingOrders(Cache cache) {
        try {
            LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);

            LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Order::getStatus, 0)  // 待支付状态
                    .ge(Order::getCreatedAt, oneDayAgo)  // 最近24小时
                    .orderByDesc(Order::getCreatedAt);

            List<Order> pendingOrders = orderMapper.selectList(queryWrapper);
            log.info("查询到 {} 个待支付订单,开始预热...", pendingOrders.size());

            int count = 0;
            for (Order order : pendingOrders) {
                try {
                    cache.put(order.getId(), order);
                    count++;
                } catch (Exception e) {
                    log.warn("预热待支付订单 {} 失败: {}", order.getId(), e.getMessage());
                }
            }

            log.info("待支付订单预热完成: {} 个", count);
            return count;

        } catch (Exception e) {
            log.error("预热待支付订单失败", e);
            return 0;
        }
    }

    /**
     * 预热处理中订单 (最近7天,前100条)
     */
    private int warmupProcessingOrders(Cache cache) {
        try {
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

            LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(Order::getStatus, 1, 2, 3)  // 已支付、配送中、已完成
                    .ge(Order::getUpdatedAt, sevenDaysAgo)  // 最近7天
                    .orderByDesc(Order::getUpdatedAt)
                    .last("LIMIT 100");

            List<Order> processingOrders = orderMapper.selectList(queryWrapper);
            log.info("查询到 {} 个处理中订单,开始预热...", processingOrders.size());

            int count = 0;
            for (Order order : processingOrders) {
                try {
                    // 避免重复预热(待支付订单可能也在处理中)
                    if (cache.get(order.getId()) == null) {
                        cache.put(order.getId(), order);
                        count++;
                    }
                } catch (Exception e) {
                    log.warn("预热处理中订单 {} 失败: {}", order.getId(), e.getMessage());
                }
            }

            log.info("处理中订单预热完成: {} 个", count);
            return count;

        } catch (Exception e) {
            log.error("预热处理中订单失败", e);
            return 0;
        }
    }

    @Override
    public String getStrategyName() {
        return "OrderCacheWarmupStrategy";
    }
}
