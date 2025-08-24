package com.cloud.user.exception;

/**
 * 用户已存在异常
 * 当尝试创建一个已存在的用户时抛出此异常
 */
public class UserAlreadyExistsException extends UserServiceException {
    
    public UserAlreadyExistsException(String username) {
        super(40901, "用户已存在: " + username);
    }
}