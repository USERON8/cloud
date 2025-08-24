package com.cloud.order.exception;

/**
 * 订单状态异常
 * 当订单状态不满足操作条件时抛出此异常
 */
public class OrderStatusException extends OrderServiceException {
    
    public OrderStatusException(String message) {
        super(40002, "订单状态错误: " + message);
    }
}