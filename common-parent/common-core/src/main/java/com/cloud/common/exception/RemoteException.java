package com.cloud.common.exception;

import com.cloud.common.enums.ResultCode;

public class RemoteException extends BaseException {

  private final RemoteFailType failType;

  public enum RemoteFailType {
    TIMEOUT,
    UNAVAILABLE,
    REJECTED,
    PROVIDER_ERR
  }

  public RemoteException(ResultCode resultCode) {
    this(resultCode, resultCode == null ? null : resultCode.getMessage(), null);
  }

  public RemoteException(ResultCode resultCode, String message) {
    this(resultCode, message, null);
  }

  public RemoteException(ResultCode resultCode, String message, Throwable cause) {
    this(resultCode, message, cause, RemoteFailType.UNAVAILABLE);
  }

  public RemoteException(String message, Throwable cause) {
    this(ResultCode.REMOTE_SERVICE_UNAVAILABLE, message, cause);
  }

  public static RemoteException timeout(
      ResultCode resultCode, String target, long elapsedMs, Throwable cause) {
    String message = target + " timed out after " + elapsedMs + "ms";
    return new RemoteException(resultCode, message, cause, RemoteFailType.TIMEOUT);
  }

  public static RemoteException unavailable(ResultCode resultCode, String target, Throwable cause) {
    return new RemoteException(
        resultCode, target + " unavailable", cause, RemoteFailType.UNAVAILABLE);
  }

  public static RemoteException rejected(ResultCode resultCode, String target, Throwable cause) {
    return new RemoteException(resultCode, target + " rejected", cause, RemoteFailType.REJECTED);
  }

  public static RemoteException providerError(
      ResultCode resultCode, String target, Throwable cause) {
    return new RemoteException(
        resultCode, target + " provider error", cause, RemoteFailType.PROVIDER_ERR);
  }

  public boolean isTimeout() {
    return failType == RemoteFailType.TIMEOUT;
  }

  public boolean isUnavailable() {
    return failType == RemoteFailType.UNAVAILABLE;
  }

  public boolean isRejected() {
    return failType == RemoteFailType.REJECTED;
  }

  public boolean isProviderError() {
    return failType == RemoteFailType.PROVIDER_ERR;
  }

  public RemoteFailType getFailType() {
    return failType;
  }

  private RemoteException(
      ResultCode resultCode, String message, Throwable cause, RemoteFailType failType) {
    super(resultCode, resolveMessage(resultCode, message), cause, 503, ErrorCategory.REMOTE, true);
    this.failType = failType == null ? RemoteFailType.UNAVAILABLE : failType;
  }

  private static String resolveMessage(ResultCode resultCode, String message) {
    if (message != null && !message.isBlank()) {
      return message;
    }
    return resultCode == null
        ? ResultCode.REMOTE_SERVICE_UNAVAILABLE.getMessage()
        : resultCode.getMessage();
  }
}
