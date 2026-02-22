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
            super(404, "鍟嗗涓嶅瓨鍦? " + merchantId);
        }

        public MerchantNotFoundException(String merchantName) {
            super(404, "鍟嗗涓嶅瓨鍦? " + merchantName);
        }
    }

    


    public static class MerchantAlreadyExistsException extends MerchantException {
        public MerchantAlreadyExistsException(String merchantName) {
            super(409, "鍟嗗宸插瓨鍦? " + merchantName);
        }
    }

    


    public static class MerchantStatusException extends MerchantException {
        public MerchantStatusException(String message) {
            super(400, message);
        }

        public MerchantStatusException(Long merchantId, String status) {
            super(400, "鍟嗗鐘舵€佸紓甯? ID: " + merchantId + ", 鐘舵€? " + status);
        }
    }

    


    public static class MerchantAuditException extends MerchantException {
        public MerchantAuditException(String message) {
            super(400, message);
        }

        public MerchantAuditException(Long merchantId, String reason) {
            super(400, "鍟嗗瀹℃牳澶辫触, ID: " + merchantId + ", 鍘熷洜: " + reason);
        }
    }

    


    public static class MerchantPermissionException extends MerchantException {
        public MerchantPermissionException(String message) {
            super(403, message);
        }
    }

    


    public static class MerchantCreateFailedException extends MerchantException {
        public MerchantCreateFailedException(String message) {
            super(500, "鍟嗗鍒涘缓澶辫触: " + message);
        }
    }

    


    public static class MerchantUpdateFailedException extends MerchantException {
        public MerchantUpdateFailedException(String message) {
            super(500, "鍟嗗鏇存柊澶辫触: " + message);
        }
    }

    


    public static class MerchantDeleteFailedException extends MerchantException {
        public MerchantDeleteFailedException(String message) {
            super(500, "鍟嗗鍒犻櫎澶辫触: " + message);
        }
    }

    


    public static class MerchantStatusErrorException extends MerchantException {
        public MerchantStatusErrorException(String message) {
            super(400, "鍟嗗鐘舵€侀敊璇? " + message);
        }
    }

    


    public static class MerchantQueryFailedException extends MerchantException {
        public MerchantQueryFailedException(String message) {
            super(500, "鍟嗗鏌ヨ澶辫触: " + message);
        }
    }

    


    public static class UserNotMerchantException extends MerchantException {
        public UserNotMerchantException(String message) {
            super(400, "鐢ㄦ埛闈炲晢瀹? " + message);
        }
    }
}
