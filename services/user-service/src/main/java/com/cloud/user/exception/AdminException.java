package com.cloud.user.exception;

import com.cloud.common.exception.BizException;
import lombok.Getter;

@Getter
public class AdminException extends BizException {

  public AdminException(int code, String message) {
    super(code, message);
  }

  public AdminException(String message) {
    super(message);
  }

  public AdminException(int code, String message, Throwable cause) {
    super(code, message, cause);
  }

  public static class AdminNotFoundException extends AdminException {
    public AdminNotFoundException(Long adminId) {
      super(404, "Admin not found: " + adminId);
    }

    public AdminNotFoundException(String username) {
      super(404, "Admin not found: " + username);
    }
  }

  public static class AdminAlreadyExistsException extends AdminException {
    public AdminAlreadyExistsException(String username) {
      super(409, "Admin already exists: " + username);
    }
  }

  public static class AdminStatusException extends AdminException {
    public AdminStatusException(String message) {
      super(400, message);
    }

    public AdminStatusException(Long adminId, String status) {
      super(400, "Invalid admin status. ID: " + adminId + ", status: " + status);
    }
  }

  public static class AdminPasswordException extends AdminException {
    public AdminPasswordException(String message) {
      super(400, message);
    }
  }
}
