package com.cloud.stock.exception;

import com.cloud.common.exception.BusinessException;






public class StockFrozenException extends BusinessException {

    private static final int STOCK_FROZEN_ERROR_CODE = 3005;

    public StockFrozenException(String message) {
        super(STOCK_FROZEN_ERROR_CODE, message);
    }

    public StockFrozenException(String operation, Long productId) {
        super(STOCK_FROZEN_ERROR_CODE,
                String.format("鍐荤粨搴撳瓨[%s]鎿嶄綔澶辫触锛屽晢鍝両D锛?d", operation, productId));
    }

    public StockFrozenException(String operation, Long productId, Integer quantity) {
        super(STOCK_FROZEN_ERROR_CODE,
                String.format("鍐荤粨搴撳瓨[%s]鎿嶄綔澶辫触锛屽晢鍝両D锛?d锛屾暟閲忥細%d", operation, productId, quantity));
    }

    public StockFrozenException(String message, Throwable cause) {
        super(STOCK_FROZEN_ERROR_CODE, message, cause);
    }
}
