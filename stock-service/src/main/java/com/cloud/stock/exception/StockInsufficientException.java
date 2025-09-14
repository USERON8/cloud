package com.cloud.stock.exception;

import com.cloud.common.exception.BusinessException;

/**
 * 库存不足异常
 *
 * @author what's up
 */
public class StockInsufficientException extends BusinessException {

    private static final int STOCK_INSUFFICIENT_CODE = 3001;

    public StockInsufficientException(String message) {
        super(STOCK_INSUFFICIENT_CODE, message);
    }

    public StockInsufficientException(Long productId, Integer required, Integer available) {
        super(STOCK_INSUFFICIENT_CODE,
                String.format("商品ID[%d]库存不足，需要：%d，可用：%d", productId, required, available));
    }

    public StockInsufficientException(String productName, Integer required, Integer available) {
        super(STOCK_INSUFFICIENT_CODE,
                String.format("商品[%s]库存不足，需要：%d，可用：%d", productName, required, available));
    }

    public StockInsufficientException(String message, Throwable cause) {
        super(STOCK_INSUFFICIENT_CODE, message, cause);
    }
}
