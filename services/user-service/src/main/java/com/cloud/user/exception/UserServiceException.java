package com.cloud.user.exception;

import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;

public class UserServiceException extends BizException {

  public UserServiceException(int code, String message) {
    super(code, message);
  }

  public UserServiceException(String message) {
    super(message);
  }

  public UserServiceException(String message, Throwable cause) {
    super(message, cause);
  }

  public UserServiceException(ResultCode resultCode) {
    super(resultCode);
  }

  public UserServiceException(ResultCode resultCode, String message) {
    super(resultCode, message);
  }

  public UserServiceException(ResultCode resultCode, Throwable cause) {
    super(resultCode, null, cause);
  }

  public UserServiceException(int code, String message, Throwable cause) {
    super(code, message, cause);
  }
}
