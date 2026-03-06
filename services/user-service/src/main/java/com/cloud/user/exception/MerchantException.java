package com.cloud.user.exception;

import com.cloud.common.exception.BusinessException;
import lombok.Getter;

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

    public static class MerchantNotFoundException extends MerchantException {
        public MerchantNotFoundException(Long merchantId) {
            super(404, "Merchant not found: " + merchantId);
        }

        public MerchantNotFoundException(String merchantName) {
            super(404, "Merchant not found: " + merchantName);
        }
    }

    public static class MerchantAlreadyExistsException extends MerchantException {
        public MerchantAlreadyExistsException(String merchantName) {
            super(409, "Merchant already exists: " + merchantName);
        }
    }

    public static class MerchantStatusException extends MerchantException {
        public MerchantStatusException(String message) {
            super(400, message);
        }

        public MerchantStatusException(Long merchantId, String status) {
            super(400, "Invalid merchant status. ID: " + merchantId + ", status: " + status);
        }
    }

    public static class MerchantAuditException extends MerchantException {
        public MerchantAuditException(String message) {
            super(400, message);
        }

        public MerchantAuditException(Long merchantId, String reason) {
            super(400, "Merchant audit failed. ID: " + merchantId + ", reason: " + reason);
        }
    }

    public static class MerchantPermissionException extends MerchantException {
        public MerchantPermissionException(String message) {
            super(403, message);
        }
    }

    public static class MerchantCreateFailedException extends MerchantException {
        public MerchantCreateFailedException(String message) {
            super(500, "Merchant creation failed: " + message);
        }
    }

    public static class MerchantUpdateFailedException extends MerchantException {
        public MerchantUpdateFailedException(String message) {
            super(500, "Merchant update failed: " + message);
        }
    }

    public static class MerchantDeleteFailedException extends MerchantException {
        public MerchantDeleteFailedException(String message) {
            super(500, "Merchant deletion failed: " + message);
        }
    }

    public static class MerchantStatusErrorException extends MerchantException {
        public MerchantStatusErrorException(String message) {
            super(400, "Merchant status error: " + message);
        }
    }

    public static class MerchantQueryFailedException extends MerchantException {
        public MerchantQueryFailedException(String message) {
            super(500, "Merchant query failed: " + message);
        }
    }

    public static class UserNotMerchantException extends MerchantException {
        public UserNotMerchantException(String message) {
            super(400, "User is not a merchant: " + message);
        }
    }
}