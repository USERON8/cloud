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

/**
 * 用户服务全局异常处理器
 * 继承公共异常处理器，只处理用户服务特有的异常
 * 其他常见异常由父类 GlobalExceptionHandler 统一处理
 *
 * 核心原则：
 * 1. Controller层不允许抛异常，所有异常由全局异常处理器统一处理
 * 2. Service层抛出特定异常，由全局异常处理器转换为标准Result格式
 * 3. 所有异常响应统一使用Result包装，不使用ResponseEntity
 *
 * @author cloud
 */
@Slf4j
@Hidden
@RestControllerAdvice
public class UserGlobalExceptionHandler extends com.cloud.common.exception.GlobalExceptionHandler {

    /**
     * 处理权限拒绝异常 - Spring Security访问控制
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Result<String> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        log.warn("权限拒绝 - uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.forbidden("您没有权限执行此操作");
    }

    /**
     * 处理唯一约束冲突异常 - 数据库唯一键冲突
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<String> handleDuplicateKeyException(DuplicateKeyException e, HttpServletRequest request) {
        log.warn("数据重复冲突 - uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.error(ResultCode.DB_DUPLICATE_KEY);
    }

    /**
     * 处理数据库完整性违反异常 - 外键约束等
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result<String> handleDataIntegrityViolationException(DataIntegrityViolationException e, HttpServletRequest request) {
        log.warn("数据完整性违反 - uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.error(ResultCode.DB_CONSTRAINT_VIOLATION);
    }

    /**
     * 处理文件上传大小超限异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<String> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e, HttpServletRequest request) {
        log.warn("文件上传大小超限 - uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.error(ResultCode.FILE_SIZE_EXCEEDED);
    }

    /**
     * 处理用户服务特定异常 - UserServiceException及其子类
     */
    @ExceptionHandler(UserServiceException.class)
    public Result<String> handleUserServiceException(UserServiceException e, HttpServletRequest request) {
        log.warn("用户服务异常 - uri: {}, type: {}, message: {}", request.getRequestURI(), e.getClass().getSimpleName(), e.getMessage());

        // 根据具体的异常类型返回对应的错误码
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

        // 默认处理
        return Result.error(ResultCode.BUSINESS_ERROR, e.getMessage());
    }

    /**
     * 处理管理员服务异常 - AdminException及其子类
     */
    @ExceptionHandler(AdminException.class)
    public Result<String> handleAdminException(AdminException e, HttpServletRequest request) {
        log.warn("管理员服务异常 - uri: {}, type: {}, message: {}", request.getRequestURI(), e.getClass().getSimpleName(), e.getMessage());

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

    /**
     * 处理商家服务异常 - MerchantException及其子类
     */
    @ExceptionHandler(MerchantException.class)
    public Result<String> handleMerchantException(MerchantException e, HttpServletRequest request) {
        log.warn("商家服务异常 - uri: {}, type: {}, message: {}", request.getRequestURI(), e.getClass().getSimpleName(), e.getMessage());

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