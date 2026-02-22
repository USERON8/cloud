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






@Slf4j
@Service
@RequiredArgsConstructor
public class OrderTimeoutServiceImpl implements OrderTimeoutService {

    private final OrderService orderService;

    
    @Value("${order.timeout.minutes:30}")
    private Integer timeoutMinutes;

    @Override
    public int checkAndHandleTimeoutOrders() {
        

        try {
            
            List<Order> timeoutOrders = getTimeoutOrders(timeoutMinutes);

            if (timeoutOrders.isEmpty()) {
                

            throw new BusinessException("妫€鏌ヨ秴鏃惰鍗曞け璐?, e);
        }
    }

    @Override
    public List<Order> getTimeoutOrders(Integer timeoutMinutes) {
        if (timeoutMinutes == null || timeoutMinutes <= 0) {
            timeoutMinutes = this.timeoutMinutes;
        }

        
        LocalDateTime timeoutTime = LocalDateTime.now().minusMinutes(timeoutMinutes);

        

        
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getStatus, 0)  
                .lt(Order::getCreatedAt, timeoutTime)      
                .orderByAsc(Order::getCreatedAt);

        List<Order> timeoutOrders = orderService.list(queryWrapper);

        
        return timeoutOrders;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelTimeoutOrder(Long orderId) {
        

        try {
            
            boolean success = orderService.cancelOrder(orderId);

            if (success) {
                

                
                
                
            } else {
                log.warn("瓒呮椂璁㈠崟鍙栨秷澶辫触, orderId: {}", orderId);
            }

            return success;

        } catch (Exception e) {
            log.error("鍙栨秷瓒呮椂璁㈠崟寮傚父, orderId: {}", orderId, e);
            throw new BusinessException("鍙栨秷瓒呮椂璁㈠崟澶辫触", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchCancelTimeoutOrders(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return 0;
        }

        

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
                log.error("鍙栨秷璁㈠崟澶辫触, orderId: {}", orderId, e);
                failedOrderIds.add(orderId);
            }
        }

        if (!failedOrderIds.isEmpty()) {
            log.warn("閮ㄥ垎璁㈠崟鍙栨秷澶辫触, 澶辫触璁㈠崟ID: {}", failedOrderIds);
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
            throw new BusinessException("瓒呮椂鏃堕棿蹇呴』澶т簬0");
        }

        this.timeoutMinutes = timeoutMinutes;
        

        
        return true;
    }
}
