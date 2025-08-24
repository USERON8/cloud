package com.cloud.stock.exception;

/**
 * 库存解冻异常
 * 当库存解冻操作失败时抛出此异常
 */
public class UnfreezeStockException extends StockServiceException {
    
    public UnfreezeStockException(String message) {
        super(40009, "库存解冻失败: " + message);
    }
}