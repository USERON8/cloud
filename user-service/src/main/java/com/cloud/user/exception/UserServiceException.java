package com.cloud.user.exception;

import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BusinessException;

/**
 * 用户服务业务异常基类
 * 所有用户服务特定的业务异常都应该继承此类
 */
public class UserServiceException extends BusinessException {

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
        super(resultCode, cause);
    }

    public UserServiceException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }

    // 用户相关特定异常

    public static class UserNotFoundException extends UserServiceException {
        public UserNotFoundException(String message) {
            super(ResultCode.USER_NOT_FOUND, message);
        }

        public UserNotFoundException(Long userId) {
            super(ResultCode.USER_NOT_FOUND, String.format("用户不存在，用户ID: %d", userId));
        }

        public UserNotFoundException(String username, boolean isEmail) {
            super(ResultCode.USER_NOT_FOUND, String.format("用户不存在，%s: %s", isEmail ? "邮箱" : "用户名", username));
        }
    }

    public static class UserAlreadyExistsException extends UserServiceException {
        public UserAlreadyExistsException(String message) {
            super(ResultCode.USER_ALREADY_EXISTS, message);
        }

        public UserAlreadyExistsException(String username, String email) {
            super(ResultCode.USER_ALREADY_EXISTS, String.format("用户已存在，用户名: %s，邮箱: %s", username, email));
        }
    }

    public static class AddressPermissionException extends UserServiceException {
        public AddressPermissionException(String message) {
            super(ResultCode.FORBIDDEN.getCode(), message);
        }

        public AddressPermissionException(Long userId, Long addressId) {
            super(ResultCode.FORBIDDEN.getCode(), String.format("用户 %d 无权访问地址 %d", userId, addressId));
        }
    }

    public static class FileUploadException extends UserServiceException {
        public FileUploadException(String message) {
            super(ResultCode.UPLOAD_FAILED, message);
        }

        public FileUploadException(String message, Throwable cause) {
            super(ResultCode.UPLOAD_FAILED.getCode(), message, cause);
        }
    }

    public static class FileSizeExceededException extends UserServiceException {
        public FileSizeExceededException(String message) {
            super(ResultCode.FILE_SIZE_EXCEEDED, message);
        }
    }

    public static class UserCreateFailedException extends UserServiceException {
        public UserCreateFailedException(String message) {
            super(ResultCode.USER_CREATE_FAILED, message);
        }
    }

    public static class UserUpdateFailedException extends UserServiceException {
        public UserUpdateFailedException(String message) {
            super(ResultCode.USER_UPDATE_FAILED, message);
        }
    }

    public static class UserDeleteFailedException extends UserServiceException {
        public UserDeleteFailedException(String message) {
            super(ResultCode.USER_DELETE_FAILED, message);
        }
    }

    public static class UserQueryFailedException extends UserServiceException {
        public UserQueryFailedException(String message) {
            super(ResultCode.USER_QUERY_FAILED, message);
        }
    }

    public static class UserTypeMismatchException extends UserServiceException {
        public UserTypeMismatchException(String message) {
            super(ResultCode.USER_TYPE_MISMATCH, message);
        }
    }

    public static class UserDisabledException extends UserServiceException {
        public UserDisabledException(String message) {
            super(ResultCode.USER_DISABLED, message);
        }
    }

    public static class PasswordErrorException extends UserServiceException {
        public PasswordErrorException(String message) {
            super(ResultCode.PASSWORD_ERROR, message);
        }
    }

    public static class ParamValidationFailedException extends UserServiceException {
        public ParamValidationFailedException(String message) {
            super(ResultCode.PARAM_VALIDATION_FAILED, message);
        }
    }
}