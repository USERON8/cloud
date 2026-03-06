package com.cloud.order.exception;

import com.cloud.common.exception.BusinessException;







public class InsufficientStockException extends BusinessException {

    private static final int INSUFFICIENT_STOCK_ERROR_CODE = 40002;

    private Long productId;

    public InsufficientStockException(String message) {
        super(INSUFFICIENT_STOCK_ERROR_CODE, message);
    }

    public InsufficientStockException(Long productId, Integer required, Integer available) {
        super(INSUFFICIENT_STOCK_ERROR_CODE,
                String.format("鍟嗗搧[ID:%d]搴撳瓨涓嶈冻锛岄渶瑕侊細%d锛屽彲鐢細%d", productId, required, available));
        this.productId = productId;
    }

    public InsufficientStockException(String productName, Integer required, Integer available) {
        super(INSUFFICIENT_STOCK_ERROR_CODE,
                String.format("鍟嗗搧[%s]搴撳瓨涓嶈冻锛岄渶瑕侊細%d锛屽彲鐢細%d", productName, required, available));
    }

    public InsufficientStockException(String message, Throwable cause) {
        super(INSUFFICIENT_STOCK_ERROR_CODE, message, cause);
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }
}
