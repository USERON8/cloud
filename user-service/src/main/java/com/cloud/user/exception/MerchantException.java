package com.cloud.user.exception;

import com.cloud.common.exception.BusinessException;
import lombok.Getter;

/**
 * 商家服务异常类
 * 用于处理商家业务相关的异常情况
 *
 * @author what's up
 * @since 1.0.0
 */
@Getter
public class MerchantException extends BusinessException {

    public MerchantException(int code, String message) {
        super(code, message);
    }

    public MerchantException(String message) {
        super(message);
    }

    public MerchantException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }

    /**
     * 商家不存在异常
     */
    public static class MerchantNotFoundException extends MerchantException {
        public MerchantNotFoundException(Long merchantId) {
            super(404, "商家不存在: " + merchantId);
        }

        public MerchantNotFoundException(String merchantName) {
            super(404, "商家不存在: " + merchantName);
        }
    }

    /**
     * 商家已存在异常
     */
    public static class MerchantAlreadyExistsException extends MerchantException {
        public MerchantAlreadyExistsException(String merchantName) {
            super(409, "商家已存在: " + merchantName);
        }
    }

    /**
     * 商家状态异常
     */
    public static class MerchantStatusException extends MerchantException {
        public MerchantStatusException(String message) {
            super(400, message);
        }

        public MerchantStatusException(Long merchantId, String status) {
            super(400, "商家状态异常, ID: " + merchantId + ", 状态: " + status);
        }
    }

    /**
     * 商家审核异常
     */
    public static class MerchantAuditException extends MerchantException {
        public MerchantAuditException(String message) {
            super(400, message);
        }

        public MerchantAuditException(Long merchantId, String reason) {
            super(400, "商家审核失败, ID: " + merchantId + ", 原因: " + reason);
        }
    }

    /**
     * 商家权限异常
     */
    public static class MerchantPermissionException extends MerchantException {
        public MerchantPermissionException(String message) {
            super(403, message);
        }
    }
}
