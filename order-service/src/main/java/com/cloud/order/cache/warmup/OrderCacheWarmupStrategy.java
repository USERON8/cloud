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


@Component
@Slf4j
@RequiredArgsConstructor
public class OrderCacheWarmupStrategy implements com.cloud.common.cache.warmup.CacheWarmupStrategy {

    private final OrderMapper orderMapper;

    @Override
    public int warmup(CacheManager cacheManager) {
        int warmedUpCount = 0;

        try {


            return warmedUpCount;

        } catch (Exception e) {
            log.error("璁㈠崟缂撳瓨棰勭儹澶辫触", e);
            return warmedUpCount;
        }
    }


    private int warmupPendingOrders(Cache cache) {

        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);

        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getStatus, 0)
                .ge(Order::getCreatedAt, oneDayAgo)
                .orderByDesc(Order::getCreatedAt);

        List<Order> pendingOrders = orderMapper.selectList(queryWrapper);


        int count = 0;
        for (Order order : pendingOrders) {
            try {
                cache.put(order.getId(), order);
                count++;
            } catch (Exception e) {
                log.warn("棰勭儹寰呮敮浠樿鍗?{} 澶辫触: {}", order.getId(), e.getMessage());
            }
        }


        return 0;

    }


    private int warmupProcessingOrders(Cache cache) {

        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Order::getStatus, 1, 2, 3)
                .ge(Order::getUpdatedAt, sevenDaysAgo)
                .orderByDesc(Order::getUpdatedAt)
                .last("LIMIT 100");

        List<Order> processingOrders = orderMapper.selectList(queryWrapper);


        int count = 0;
        for (Order order : processingOrders) {
            try {

                if (cache.get(order.getId()) == null) {
                    cache.put(order.getId(), order);
                    count++;
                }
            } catch (Exception e) {
                log.warn("棰勭儹澶勭悊涓鍗?{} 澶辫触: {}", order.getId(), e.getMessage());
            }
        }


        return 0;

    }

    @Override
    public String getStrategyName() {
        return "OrderCacheWarmupStrategy";
    }
}
