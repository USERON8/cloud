package com.cloud.stock.exception;

/**
 * 库存记录不存在异常
 * 当尝试操作一个不存在的库存记录时抛出此异常
 * 
 * @author cloud
 * @since 1.0.0
 */
public class StockNotFoundException extends StockServiceException {
    
    public StockNotFoundException(Long stockId) {
        super(40406, "库存记录不存在: " + stockId);
    }
    
    public StockNotFoundException(String productNo) {
        super(40406, "库存记录不存在: " + productNo);
    }
}