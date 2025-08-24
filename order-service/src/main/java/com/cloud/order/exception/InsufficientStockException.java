package com.cloud.order.exception;

/**
 * 库存不足异常
 * 当订单中商品库存不足时抛出此异常
 */
public class InsufficientStockException extends OrderServiceException {
    
    public InsufficientStockException(String message) {
        super(40003, "库存不足: " + message);
    }
}