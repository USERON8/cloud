package com.cloud.stock.exception;

/**
 * 库存查询异常
 * 当库存查询操作失败时抛出此异常
 */
public class QueryStockException extends StockServiceException {
    
    public QueryStockException(String message) {
        super(50003, "库存查询失败: " + message);
    }
}