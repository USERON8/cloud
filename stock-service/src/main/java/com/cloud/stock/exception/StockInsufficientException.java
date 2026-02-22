package com.cloud.stock.exception;

import com.cloud.common.exception.BusinessException;






public class StockInsufficientException extends BusinessException {

    private static final int STOCK_INSUFFICIENT_CODE = 3001;

    public StockInsufficientException(String message) {
        super(STOCK_INSUFFICIENT_CODE, message);
    }

    public StockInsufficientException(Long productId, Integer required, Integer available) {
        super(STOCK_INSUFFICIENT_CODE,
                String.format("鍟嗗搧ID[%d]搴撳瓨涓嶈冻锛岄渶瑕侊細%d锛屽彲鐢細%d", productId, required, available));
    }

    public StockInsufficientException(String productName, Integer required, Integer available) {
        super(STOCK_INSUFFICIENT_CODE,
                String.format("鍟嗗搧[%s]搴撳瓨涓嶈冻锛岄渶瑕侊細%d锛屽彲鐢細%d", productName, required, available));
    }

    public StockInsufficientException(String message, Throwable cause) {
        super(STOCK_INSUFFICIENT_CODE, message, cause);
    }
}
