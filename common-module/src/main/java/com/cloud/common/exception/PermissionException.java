package com.cloud.common.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 权限异常类
 * 用于处理用户权限不足的情况
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PermissionException extends BusinessException {
    private String permission;

    public PermissionException(String message) {
        super(message);
    }

    public PermissionException(int code, String message) {
        super(code, message);
    }

    public PermissionException(String permission, String message) {
        super(message);
        this.permission = permission;
    }

    public PermissionException(int code, String permission, String message) {
        super(code, message);
        this.permission = permission;
    }
}