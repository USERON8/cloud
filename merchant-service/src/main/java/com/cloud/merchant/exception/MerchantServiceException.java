package com.cloud.merchant.exception;

import com.cloud.common.exception.BusinessException;

/**
 * 商家服务业务异常基类
 * 所有商家服务特定的业务异常都应该继承此类
 */
public class MerchantServiceException extends BusinessException {
    
    public MerchantServiceException(int code, String message) {
        super(code, message);
    }
    
    public MerchantServiceException(String message) {
        super(message);
    }
}