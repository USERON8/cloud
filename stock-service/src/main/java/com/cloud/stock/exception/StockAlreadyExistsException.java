package com.cloud.stock.exception;

import com.cloud.common.exception.BusinessException;

/**
 * 库存已存在异常
 *
 * @author what's up
 */
public class StockAlreadyExistsException extends BusinessException {

    private static final int STOCK_ALREADY_EXISTS_CODE = 3003;

    public StockAlreadyExistsException(String message) {
        super(STOCK_ALREADY_EXISTS_CODE, message);
    }

    public StockAlreadyExistsException(Long productId) {
        super(STOCK_ALREADY_EXISTS_CODE, String.format("商品ID[%d]的库存信息已存在", productId));
    }


    public StockAlreadyExistsException(String message, Throwable cause) {
        super(STOCK_ALREADY_EXISTS_CODE, message, cause);
    }
}
