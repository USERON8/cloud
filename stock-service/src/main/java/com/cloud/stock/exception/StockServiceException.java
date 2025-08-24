package com.cloud.stock.exception;

import com.cloud.common.exception.BusinessException;

/**
 * 库存服务业务异常基类
 * 所有库存服务特定的业务异常都应该继承此类
 * 
 * @author cloud
 * @since 1.0.0
 */
public class StockServiceException extends BusinessException {
    
    public StockServiceException(int code, String message) {
        super(code, message);
    }
    
    public StockServiceException(String message) {
        super(message);
    }
}