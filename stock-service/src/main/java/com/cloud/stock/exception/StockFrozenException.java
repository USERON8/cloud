package com.cloud.stock.exception;

import com.cloud.common.exception.BusinessException;

/**
 * 冻结库存操作异常
 *
 * @author what's up
 */
public class StockFrozenException extends BusinessException {

    private static final int STOCK_FROZEN_ERROR_CODE = 3005;

    public StockFrozenException(String message) {
        super(STOCK_FROZEN_ERROR_CODE, message);
    }

    public StockFrozenException(String operation, Long productId) {
        super(STOCK_FROZEN_ERROR_CODE,
                String.format("冻结库存[%s]操作失败，商品ID：%d", operation, productId));
    }

    public StockFrozenException(String operation, Long productId, Integer quantity) {
        super(STOCK_FROZEN_ERROR_CODE,
                String.format("冻结库存[%s]操作失败，商品ID：%d，数量：%d", operation, productId, quantity));
    }

    public StockFrozenException(String message, Throwable cause) {
        super(STOCK_FROZEN_ERROR_CODE, message, cause);
    }
}
