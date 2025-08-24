package com.cloud.stock.exception;

/**
 * 库存冻结异常
 * 当库存冻结操作失败时抛出此异常
 */
public class FreezeStockException extends StockServiceException {
    
    public FreezeStockException(String message) {
        super(40008, "库存冻结失败: " + message);
    }
}