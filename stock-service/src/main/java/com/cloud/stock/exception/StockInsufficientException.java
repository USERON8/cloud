package com.cloud.stock.exception;

import com.cloud.common.exception.BusinessException;

public class StockInsufficientException extends BusinessException {

    private static final int STOCK_INSUFFICIENT_CODE = 3001;

    public StockInsufficientException(String message) {
        super(STOCK_INSUFFICIENT_CODE, message);
    }

    public StockInsufficientException(Long productId, Integer required, Integer available) {
        super(
                STOCK_INSUFFICIENT_CODE,
                String.format("Insufficient stock, productId=%d, required=%d, available=%d", productId, required, available)
        );
    }

    public StockInsufficientException(String productName, Integer required, Integer available) {
        super(
                STOCK_INSUFFICIENT_CODE,
                String.format("Insufficient stock, productName=%s, required=%d, available=%d", productName, required, available)
        );
    }

    public StockInsufficientException(String message, Throwable cause) {
        super(STOCK_INSUFFICIENT_CODE, message, cause);
    }
}
