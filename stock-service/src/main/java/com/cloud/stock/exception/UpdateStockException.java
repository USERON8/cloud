package com.cloud.stock.exception;

/**
 * 库存更新异常
 * 当库存更新操作失败时抛出此异常
 */
public class UpdateStockException extends StockServiceException {
    
    public UpdateStockException(String message) {
        super(50002, "库存更新失败: " + message);
    }
}