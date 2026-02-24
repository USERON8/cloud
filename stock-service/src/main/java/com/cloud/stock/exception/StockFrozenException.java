package com.cloud.stock.exception;

import com.cloud.common.exception.BusinessException;

public class StockFrozenException extends BusinessException {

    private static final int STOCK_FROZEN_ERROR_CODE = 3005;

    public StockFrozenException(String message) {
        super(STOCK_FROZEN_ERROR_CODE, message);
    }

    public StockFrozenException(String operation, Long productId) {
        super(
                STOCK_FROZEN_ERROR_CODE,
                String.format("Stock freeze operation [%s] failed, productId=%d", operation, productId)
        );
    }

    public StockFrozenException(String operation, Long productId, Integer quantity) {
        super(
                STOCK_FROZEN_ERROR_CODE,
                String.format("Stock freeze operation [%s] failed, productId=%d, quantity=%d", operation, productId, quantity)
        );
    }

    public StockFrozenException(String message, Throwable cause) {
        super(STOCK_FROZEN_ERROR_CODE, message, cause);
    }
}
