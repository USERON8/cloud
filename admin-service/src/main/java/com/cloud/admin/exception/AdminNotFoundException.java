package com.cloud.admin.exception;

/**
 * 管理员不存在异常
 * 当尝试操作一个不存在的管理员时抛出此异常
 */
public class AdminNotFoundException extends AdminServiceException {
    
    public AdminNotFoundException(Long adminId) {
        super(40408, "管理员不存在: " + adminId);
    }
    
    public AdminNotFoundException(String adminName) {
        super(40408, "管理员不存在: " + adminName);
    }
}