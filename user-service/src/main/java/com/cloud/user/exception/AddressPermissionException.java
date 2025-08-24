package com.cloud.user.exception;

/**
 * 地址权限异常
 * 当用户尝试操作不属于自己地址时抛出此异常
 */
public class AddressPermissionException extends UserServiceException {
    
    public AddressPermissionException(String message) {
        super(40301, "地址权限错误: " + message);
    }
}