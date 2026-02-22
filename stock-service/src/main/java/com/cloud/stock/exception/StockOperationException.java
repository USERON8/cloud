package com.cloud.stock.exception;

import com.cloud.common.exception.BusinessException;






public class StockOperationException extends BusinessException {

    private static final int STOCK_OPERATION_ERROR_CODE = 3004;

    public StockOperationException(String message) {
        super(STOCK_OPERATION_ERROR_CODE, message);
    }

    public StockOperationException(String operation, Long productId, String reason) {
        super(STOCK_OPERATION_ERROR_CODE,
                String.format("搴撳瓨[%s]鎿嶄綔澶辫触锛屽晢鍝両D锛?d锛屽師鍥狅細%s", operation, productId, reason));
    }

    public StockOperationException(String operation, String reason) {
        super(STOCK_OPERATION_ERROR_CODE,
                String.format("搴撳瓨[%s]鎿嶄綔澶辫触锛屽師鍥狅細%s", operation, reason));
    }

    public StockOperationException(String message, Throwable cause) {
        super(STOCK_OPERATION_ERROR_CODE, message, cause);
    }
}
