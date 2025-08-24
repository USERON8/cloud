package com.cloud.stock.exception;

/**
 * 库存不足异常
 * 当库存数量不足时抛出此异常
 * 
 * @author cloud
 * @since 1.0.0
 */
public class InsufficientStockException extends StockServiceException {
    
    public InsufficientStockException(String message) {
        super(40007, "库存不足: " + message);
    }
}