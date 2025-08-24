package com.cloud.user.exception;

/**
 * 用户不存在异常
 * 当尝试操作一个不存在的用户时抛出此异常
 */
public class UserNotFoundException extends UserServiceException {
    
    public UserNotFoundException(Long userId) {
        super(40401, "用户不存在: " + userId);
    }
    
    public UserNotFoundException(String username) {
        super(40401, "用户不存在: " + username);
    }
}