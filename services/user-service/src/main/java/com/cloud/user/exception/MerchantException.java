package com.cloud.user.exception;

import com.cloud.common.exception.BizException;
import lombok.Getter;

@Getter
public class MerchantException extends BizException {

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
}
