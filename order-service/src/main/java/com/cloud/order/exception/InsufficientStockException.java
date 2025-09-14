package com.cloud.order.exception;

/**
 * 库存不足异常
 *
 * @deprecated 推荐使用通用异常类 InsufficientException.stock(productId, required, available)
 */
@Deprecated
public class InsufficientStockException extends OrderServiceException {

    public InsufficientStockException(String message) {
        super(40003, "库存不足: " + message);
    }
}
