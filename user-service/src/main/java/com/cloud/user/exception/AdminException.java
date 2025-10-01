package com.cloud.user.exception;

import com.cloud.common.exception.BusinessException;
import lombok.Getter;

/**
 * 管理员服务异常类
 * 用于处理管理员业务相关的异常情况
 *
 * @author what's up
 * @since 1.0.0
 */
@Getter
public class AdminException extends BusinessException {

    /**
     * 管理员不存在异常
     */
    public static class AdminNotFoundException extends AdminException {
        public AdminNotFoundException(Long adminId) {
            super(404, "管理员不存在: " + adminId);
        }

        public AdminNotFoundException(String username) {
            super(404, "管理员不存在: " + username);
        }
    }

    /**
     * 管理员已存在异常
     */
    public static class AdminAlreadyExistsException extends AdminException {
        public AdminAlreadyExistsException(String username) {
            super(409, "管理员已存在: " + username);
        }
    }

    /**
     * 管理员状态异常
     */
    public static class AdminStatusException extends AdminException {
        public AdminStatusException(String message) {
            super(400, message);
        }

        public AdminStatusException(Long adminId, String status) {
            super(400, "管理员状态异常, ID: " + adminId + ", 状态: " + status);
        }
    }

    /**
     * 管理员权限异常
     */
    public static class AdminPermissionException extends AdminException {
        public AdminPermissionException(String message) {
            super(403, message);
        }
    }

    /**
     * 管理员密码异常
     */
    public static class AdminPasswordException extends AdminException {
        public AdminPasswordException(String message) {
            super(400, message);
        }
    }

    public AdminException(int code, String message) {
        super(code, message);
    }

    public AdminException(String message) {
        super(message);
    }

    public AdminException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
