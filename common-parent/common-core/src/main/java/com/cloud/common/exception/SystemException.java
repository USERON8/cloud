package com.cloud.common.exception;

import com.cloud.common.enums.ResultCode;

public class SystemException extends BaseException {

  private final boolean retryable;

  public SystemException(ResultCode resultCode) {
    this(resultCode, resultCode == null ? null : resultCode.getMessage(), null, true);
  }

  public SystemException(ResultCode resultCode, String message) {
    this(resultCode, message, null, true);
  }

  public SystemException(ResultCode resultCode, String message, Throwable cause) {
    this(resultCode, message, cause, true);
  }

  public SystemException(String message) {
    this(ResultCode.SYSTEM_ERROR, message, null, true);
  }

  public SystemException(String message, Throwable cause) {
    this(ResultCode.SYSTEM_ERROR, message, cause, true);
  }

  public SystemException(int code, String message) {
    this(code, message, null, true);
  }

  public SystemException(int code, String message, Throwable cause) {
    this(code, message, cause, true);
  }

  public static SystemException retryable(ResultCode resultCode, String message, Throwable cause) {
    return new SystemException(resultCode, message, cause, true);
  }

  public static SystemException nonRetryable(
      ResultCode resultCode, String message, Throwable cause) {
    return new SystemException(resultCode, message, cause, false);
  }

  public boolean isRetryable() {
    return retryable;
  }

  private SystemException(
      ResultCode resultCode, String message, Throwable cause, boolean retryable) {
    super(resultCode, resolveMessage(resultCode, message), cause, 500, ErrorCategory.SYSTEM, true);
    this.retryable = retryable;
  }

  private SystemException(int code, String message, Throwable cause, boolean retryable) {
    super(code, message, cause, 500, ErrorCategory.SYSTEM, true);
    this.retryable = retryable;
  }

  private static String resolveMessage(ResultCode resultCode, String message) {
    if (message != null && !message.isBlank()) {
      return message;
    }
    return resultCode == null ? ResultCode.SYSTEM_ERROR.getMessage() : resultCode.getMessage();
  }
}
