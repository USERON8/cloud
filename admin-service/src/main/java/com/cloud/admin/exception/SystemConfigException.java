package com.cloud.admin.exception;

/**
 * 系统配置异常
 * 当系统配置不正确或缺失时抛出此异常
 */
public class SystemConfigException extends AdminServiceException {
    
    public SystemConfigException(String message) {
        super(50002, "系统配置错误: " + message);
    }
}