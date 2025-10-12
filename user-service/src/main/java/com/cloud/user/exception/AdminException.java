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

    public AdminException(int code, String message) {
        super(code, message);
    }

    public AdminException(String message) {
        super(message);
    }

    public AdminException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }

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

    /**
     * 管理员创建失败异常
     */
    public static class AdminCreateFailedException extends AdminException {
        public AdminCreateFailedException(String message) {
            super(500, "管理员创建失败: " + message);
        }
    }

    /**
     * 管理员更新失败异常
     */
    public static class AdminUpdateFailedException extends AdminException {
        public AdminUpdateFailedException(String message) {
            super(500, "管理员更新失败: " + message);
        }
    }

    /**
     * 管理员删除失败异常
     */
    public static class AdminDeleteFailedException extends AdminException {
        public AdminDeleteFailedException(String message) {
            super(500, "管理员删除失败: " + message);
        }
    }

    /**
     * 管理员状态错误异常
     */
    public static class AdminStatusErrorException extends AdminException {
        public AdminStatusErrorException(String message) {
            super(400, "管理员状态错误: " + message);
        }
    }

    /**
     * 管理员查询失败异常
     */
    public static class AdminQueryFailedException extends AdminException {
        public AdminQueryFailedException(String message) {
            super(500, "管理员查询失败: " + message);
        }
    }
}
