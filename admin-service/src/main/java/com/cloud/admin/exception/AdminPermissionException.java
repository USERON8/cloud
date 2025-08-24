package com.cloud.admin.exception;

/**
 * 管理员权限异常
 * 当管理员尝试执行无权限的操作时抛出此异常
 */
public class AdminPermissionException extends AdminServiceException {
    
    public AdminPermissionException(String message) {
        super(40303, "管理员权限不足: " + message);
    }
}