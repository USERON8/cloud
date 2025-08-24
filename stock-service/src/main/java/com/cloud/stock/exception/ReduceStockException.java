package com.cloud.stock.exception;

/**
 * 库存扣减异常
 * 当库存扣减操作失败时抛出此异常
 * 
 * @author cloud
 * @since 1.0.0
 */
public class ReduceStockException extends StockServiceException {
    
    public ReduceStockException(String message) {
        super(40010, "库存扣减失败: " + message);
    }
}