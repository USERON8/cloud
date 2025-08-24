package com.cloud.user.exception;

import com.cloud.common.exception.BusinessException;

/**
 * 用户服务业务异常基类
 * 所有用户服务特定的业务异常都应该继承此类
 */
public class UserServiceException extends BusinessException {
    
    public UserServiceException(int code, String message) {
        super(code, message);
    }
    
    public UserServiceException(String message) {
        super(message);
    }
}