package com.cloud.stock.exception;

import com.cloud.common.exception.BusinessException;

/**
 * 库存操作失败异常
 *
 * @author what's up
 */
public class StockOperationException extends BusinessException {

    private static final int STOCK_OPERATION_ERROR_CODE = 3004;

    public StockOperationException(String message) {
        super(STOCK_OPERATION_ERROR_CODE, message);
    }

    public StockOperationException(String operation, Long productId, String reason) {
        super(STOCK_OPERATION_ERROR_CODE,
                String.format("库存[%s]操作失败，商品ID：%d，原因：%s", operation, productId, reason));
    }

    public StockOperationException(String operation, String reason) {
        super(STOCK_OPERATION_ERROR_CODE,
                String.format("库存[%s]操作失败，原因：%s", operation, reason));
    }

    public StockOperationException(String message, Throwable cause) {
        super(STOCK_OPERATION_ERROR_CODE, message, cause);
    }
}
