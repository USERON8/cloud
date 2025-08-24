package com.cloud.product.exception;

import com.cloud.common.exception.BusinessException;

/**
 * 商品服务业务异常基类
 * 所有商品服务特定的业务异常都应该继承此类
 */
public class ProductServiceException extends BusinessException {
    
    public ProductServiceException(int code, String message) {
        super(code, message);
    }
    
    public ProductServiceException(String message) {
        super(message);
    }
}