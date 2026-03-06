package com.cloud.user.exception;

import com.cloud.common.exception.BusinessException;
import lombok.Getter;

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

    public static class AdminPermissionException extends AdminException {
        public AdminPermissionException(String message) {
            super(403, message);
        }
    }

    public static class AdminPasswordException extends AdminException {
        public AdminPasswordException(String message) {
            super(400, message);
        }
    }

    public static class AdminCreateFailedException extends AdminException {
        public AdminCreateFailedException(String message) {
            super(500, "Admin creation failed: " + message);
        }
    }

    public static class AdminUpdateFailedException extends AdminException {
        public AdminUpdateFailedException(String message) {
            super(500, "Admin update failed: " + message);
        }
    }

    public static class AdminDeleteFailedException extends AdminException {
        public AdminDeleteFailedException(String message) {
            super(500, "Admin deletion failed: " + message);
        }
    }

    public static class AdminStatusErrorException extends AdminException {
        public AdminStatusErrorException(String message) {
            super(400, "Admin status error: " + message);
        }
    }

    public static class AdminQueryFailedException extends AdminException {
        public AdminQueryFailedException(String message) {
            super(500, "Admin query failed: " + message);
        }
    }
}