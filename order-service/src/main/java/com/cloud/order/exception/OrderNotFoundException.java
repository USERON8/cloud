package com.cloud.order.exception;

/**
 * 订单不存在异常
 * 当尝试操作一个不存在的订单时抛出此异常
 */
public class OrderNotFoundException extends OrderServiceException {
    
    public OrderNotFoundException(Long orderId) {
        super(40402, "订单不存在: " + orderId);
    }
    
    public OrderNotFoundException(String orderNo) {
        super(40402, "订单不存在: " + orderNo);
    }
}