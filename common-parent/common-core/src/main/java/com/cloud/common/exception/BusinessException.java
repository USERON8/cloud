package com.cloud.common.exception;

import com.cloud.common.enums.ResultCode;

@Deprecated
public class BusinessException extends BizException {

  public BusinessException(int code, String message) {
    super(code, message);
  }

  public BusinessException(String message) {
    super(message);
  }

  public BusinessException(ResultCode resultCode) {
    super(resultCode);
  }

  public BusinessException(ResultCode resultCode, String message) {
    super(resultCode, message);
  }

  public BusinessException(String message, Throwable cause) {
    super(message, cause);
  }

  public BusinessException(int code, String message, Throwable cause) {
    super(code, message, cause);
  }

  public BusinessException(ResultCode resultCode, Throwable cause) {
    super(resultCode, null, cause);
  }
}
