package com.cloud.user.exception;

import com.cloud.common.enums.ResultCode;
import com.cloud.common.result.Result;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@Hidden
@RestControllerAdvice(basePackages = "com.cloud")
public class UserGlobalExceptionHandler extends com.cloud.common.exception.GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public Result<String> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        log.warn("Access denied - uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.forbidden("You do not have permission to perform this operation");
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public Result<String> handleDuplicateKeyException(DuplicateKeyException e, HttpServletRequest request) {
        log.warn("Duplicate key conflict - uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.error(ResultCode.DB_DUPLICATE_KEY);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result<String> handleDataIntegrityViolationException(DataIntegrityViolationException e, HttpServletRequest request) {
        log.warn("Data integrity violation - uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.error(ResultCode.DB_CONSTRAINT_VIOLATION);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<String> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e, HttpServletRequest request) {
        log.warn("File size exceeded - uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.error(ResultCode.FILE_SIZE_EXCEEDED);
    }

    @ExceptionHandler(UserServiceException.class)
    public Result<String> handleUserServiceException(UserServiceException e, HttpServletRequest request) {
        log.warn("User service exception - uri: {}, type: {}, message: {}", request.getRequestURI(), e.getClass().getSimpleName(), e.getMessage());

        if (e instanceof UserServiceException.UserNotFoundException) {
            return Result.error(ResultCode.USER_NOT_FOUND, e.getMessage());
        } else if (e instanceof UserServiceException.UserAlreadyExistsException) {
            return Result.error(ResultCode.USER_ALREADY_EXISTS, e.getMessage());
        } else if (e instanceof UserServiceException.UserCreateFailedException) {
            return Result.error(ResultCode.USER_CREATE_FAILED, e.getMessage());
        } else if (e instanceof UserServiceException.UserUpdateFailedException) {
            return Result.error(ResultCode.USER_UPDATE_FAILED, e.getMessage());
        } else if (e instanceof UserServiceException.UserDeleteFailedException) {
            return Result.error(ResultCode.USER_DELETE_FAILED, e.getMessage());
        } else if (e instanceof UserServiceException.UserQueryFailedException) {
            return Result.error(ResultCode.USER_QUERY_FAILED, e.getMessage());
        } else if (e instanceof UserServiceException.UserTypeMismatchException) {
            return Result.error(ResultCode.USER_TYPE_MISMATCH, e.getMessage());
        } else if (e instanceof UserServiceException.UserDisabledException) {
            return Result.error(ResultCode.USER_DISABLED, e.getMessage());
        } else if (e instanceof UserServiceException.PasswordErrorException) {
            return Result.error(ResultCode.PASSWORD_ERROR, e.getMessage());
        } else if (e instanceof UserServiceException.ParamValidationFailedException) {
            return Result.error(ResultCode.PARAM_VALIDATION_FAILED, e.getMessage());
        } else if (e instanceof UserServiceException.AddressPermissionException) {
            return Result.forbidden(e.getMessage());
        } else if (e instanceof UserServiceException.FileUploadException) {
            return Result.error(ResultCode.UPLOAD_FAILED, e.getMessage());
        } else if (e instanceof UserServiceException.FileSizeExceededException) {
            return Result.error(ResultCode.FILE_SIZE_EXCEEDED, e.getMessage());
        }

        return Result.error(ResultCode.BUSINESS_ERROR, e.getMessage());
    }

    @ExceptionHandler(AdminException.class)
    public Result<String> handleAdminException(AdminException e, HttpServletRequest request) {
        log.warn("Admin service exception - uri: {}, type: {}, message: {}", request.getRequestURI(), e.getClass().getSimpleName(), e.getMessage());

        if (e instanceof AdminException.AdminNotFoundException) {
            return Result.error(ResultCode.ADMIN_NOT_FOUND, e.getMessage());
        } else if (e instanceof AdminException.AdminCreateFailedException) {
            return Result.error(ResultCode.ADMIN_CREATE_FAILED, e.getMessage());
        } else if (e instanceof AdminException.AdminUpdateFailedException) {
            return Result.error(ResultCode.ADMIN_UPDATE_FAILED, e.getMessage());
        } else if (e instanceof AdminException.AdminDeleteFailedException) {
            return Result.error(ResultCode.ADMIN_DELETE_FAILED, e.getMessage());
        } else if (e instanceof AdminException.AdminStatusErrorException) {
            return Result.error(ResultCode.ADMIN_STATUS_ERROR, e.getMessage());
        } else if (e instanceof AdminException.AdminQueryFailedException) {
            return Result.error(ResultCode.ADMIN_QUERY_FAILED, e.getMessage());
        }

        return Result.error(ResultCode.BUSINESS_ERROR, e.getMessage());
    }

    @ExceptionHandler(MerchantException.class)
    public Result<String> handleMerchantException(MerchantException e, HttpServletRequest request) {
        log.warn("Merchant service exception - uri: {}, type: {}, message: {}", request.getRequestURI(), e.getClass().getSimpleName(), e.getMessage());

        if (e instanceof MerchantException.MerchantNotFoundException) {
            return Result.error(ResultCode.MERCHANT_NOT_FOUND, e.getMessage());
        } else if (e instanceof MerchantException.MerchantCreateFailedException) {
            return Result.error(ResultCode.MERCHANT_CREATE_FAILED, e.getMessage());
        } else if (e instanceof MerchantException.MerchantUpdateFailedException) {
            return Result.error(ResultCode.MERCHANT_UPDATE_FAILED, e.getMessage());
        } else if (e instanceof MerchantException.MerchantDeleteFailedException) {
            return Result.error(ResultCode.MERCHANT_DELETE_FAILED, e.getMessage());
        } else if (e instanceof MerchantException.MerchantStatusErrorException) {
            return Result.error(ResultCode.MERCHANT_STATUS_ERROR, e.getMessage());
        } else if (e instanceof MerchantException.MerchantQueryFailedException) {
            return Result.error(ResultCode.MERCHANT_QUERY_FAILED, e.getMessage());
        } else if (e instanceof MerchantException.UserNotMerchantException) {
            return Result.error(ResultCode.USER_NOT_MERCHANT, e.getMessage());
        }

        return Result.error(ResultCode.BUSINESS_ERROR, e.getMessage());
    }
}