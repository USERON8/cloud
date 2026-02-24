package com.cloud.stock.exception;

import com.cloud.common.exception.BusinessException;

public class StockOperationException extends BusinessException {

    private static final int STOCK_OPERATION_ERROR_CODE = 3004;

    public StockOperationException(String message) {
        super(STOCK_OPERATION_ERROR_CODE, message);
    }

    public StockOperationException(String operation, Long productId, String reason) {
        super(
                STOCK_OPERATION_ERROR_CODE,
                String.format("Stock operation [%s] failed, productId=%d, reason=%s", operation, productId, reason)
        );
    }

    public StockOperationException(String operation, String reason) {
        super(STOCK_OPERATION_ERROR_CODE, String.format("Stock operation [%s] failed, reason=%s", operation, reason));
    }

    public StockOperationException(String message, Throwable cause) {
        super(STOCK_OPERATION_ERROR_CODE, message, cause);
    }
}
