package com.cloud.order.exception;

import com.cloud.common.exception.BusinessException;

/**
 * 库存不足异常
 * 当商品库存不足时抛出此异常
 *
 * @author cloud
 */
public class InsufficientStockException extends BusinessException {

    private static final int INSUFFICIENT_STOCK_ERROR_CODE = 40002;

    public InsufficientStockException(String message) {
        super(INSUFFICIENT_STOCK_ERROR_CODE, message);
    }

    public InsufficientStockException(Long productId, Integer required, Integer available) {
        super(INSUFFICIENT_STOCK_ERROR_CODE,
                String.format("商品[ID:%d]库存不足，需要：%d，可用：%d", productId, required, available));
    }

    public InsufficientStockException(String productName, Integer required, Integer available) {
        super(INSUFFICIENT_STOCK_ERROR_CODE,
                String.format("商品[%s]库存不足，需要：%d，可用：%d", productName, required, available));
    }

    public InsufficientStockException(String message, Throwable cause) {
        super(INSUFFICIENT_STOCK_ERROR_CODE, message, cause);
    }
}
