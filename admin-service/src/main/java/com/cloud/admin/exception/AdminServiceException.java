package com.cloud.admin.exception;

import com.cloud.common.exception.BusinessException;

/**
 * 管理服务业务异常基类
 * 所有管理服务特定的业务异常都应该继承此类
 */
public class AdminServiceException extends BusinessException {
    
    public AdminServiceException(int code, String message) {
        super(code, message);
    }
    
    public AdminServiceException(String message) {
        super(message);
    }
}