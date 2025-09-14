package com.cloud.user.utils;

import com.cloud.common.enums.ResultCode;
import com.cloud.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 统一响应处理工具类
 * 标准化API响应格式和错误消息
 *
 * @author what's up
 */
@Component
@Slf4j
public class ResponseHelper {

    /**
     * 操作成功响应
     */
    public static <T> Result<T> success(T data) {
        return Result.success(data);
    }

    /**
     * 操作成功响应（带消息）
     */
    public static <T> Result<T> success(String message, T data) {
        return Result.success(message, data);
    }

    /**
     * 操作成功响应（仅消息）
     */
    public static Result<String> success(String message) {
        return Result.success(message, null);
    }

    /**
     * 操作失败响应
     */
    public static <T> Result<T> error(String message) {
        return Result.error(message);
    }

    /**
     * 操作失败响应（带错误代码）
     */
    public static <T> Result<T> error(int code, String message) {
        return Result.error(code, message);
    }

    /**
     * 权限不足响应
     */
    public static <T> Result<T> forbidden(String message) {
        String errorMessage = message != null ? message : "无权限执行此操作";
        log.warn("权限验证失败: {}", errorMessage);
        return Result.error(ResultCode.FORBIDDEN.getCode(), errorMessage);
    }

    /**
     * 权限不足响应（默认消息）
     */
    public static <T> Result<T> forbidden() {
        return forbidden("您没有权限执行此操作");
    }

    /**
     * 未授权响应
     */
    public static <T> Result<T> unauthorized(String message) {
        String errorMessage = message != null ? message : "认证失败或凭据无效";
        log.warn("认证失败: {}", errorMessage);
        return Result.error(401, errorMessage);
    }

    /**
     * 未授权响应（默认消息）
     */
    public static <T> Result<T> unauthorized() {
        return unauthorized("您需要登录才能访问此资源");
    }

    /**
     * 参数错误响应
     */
    public static <T> Result<T> badRequest(String message) {
        String errorMessage = message != null ? message : "请求参数错误";
        log.warn("参数验证失败: {}", errorMessage);
        return Result.error(ResultCode.PARAM_ERROR.getCode(), errorMessage);
    }

    /**
     * 资源未找到响应
     */
    public static <T> Result<T> notFound(String message) {
        String errorMessage = message != null ? message : "请求的资源不存在";
        log.warn("资源未找到: {}", errorMessage);
        return Result.error(404, errorMessage);
    }

    /**
     * 系统错误响应
     */
    public static <T> Result<T> systemError(String message) {
        String errorMessage = message != null ? message : "系统内部错误";
        log.error("系统错误: {}", errorMessage);
        return Result.error(ResultCode.SYSTEM_ERROR.getCode(), errorMessage);
    }

    /**
     * 业务错误响应
     */
    public static <T> Result<T> businessError(String message) {
        String errorMessage = message != null ? message : "业务处理失败";
        log.warn("业务错误: {}", errorMessage);
        return Result.error(ResultCode.BUSINESS_ERROR.getCode(), errorMessage);
    }

    /**
     * 数据验证失败响应
     */
    public static <T> Result<T> validationError(String message) {
        String errorMessage = message != null ? message : "数据验证失败";
        log.warn("数据验证失败: {}", errorMessage);
        return Result.error(ResultCode.PARAM_ERROR.getCode(), errorMessage);
    }

    /**
     * 用户相关错误响应
     */
    public static class UserResponse {

        /**
         * 用户不存在
         */
        public static <T> Result<T> userNotFound(Long userId) {
            String message = userId != null ?
                    String.format("用户不存在，用户ID: %d", userId) : "用户不存在";
            return notFound(message);
        }

        /**
         * 用户不存在（按用户名）
         */
        public static <T> Result<T> userNotFound(String username) {
            String message = username != null ?
                    String.format("用户不存在，用户名: %s", username) : "用户不存在";
            return notFound(message);
        }

        /**
         * 用户已存在
         */
        public static <T> Result<T> userAlreadyExists(String username) {
            String message = username != null ?
                    String.format("用户已存在，用户名: %s", username) : "用户已存在";
            return businessError(message);
        }

        /**
         * 用户操作成功
         */
        public static <T> Result<T> operationSuccess(String operation, T data) {
            String message = String.format("用户%s成功", operation);
            return success(message, data);
        }

        /**
         * 批量用户操作成功
         */
        public static <T> Result<T> batchOperationSuccess(String operation, int count, T data) {
            String message = String.format("成功%s%d个用户", operation, count);
            return success(message, data);
        }
    }

    /**
     * 地址相关错误响应
     */
    public static class AddressResponse {

        /**
         * 地址不存在
         */
        public static <T> Result<T> addressNotFound(Long addressId) {
            String message = addressId != null ?
                    String.format("地址不存在，地址ID: %d", addressId) : "地址不存在";
            return notFound(message);
        }

        /**
         * 地址权限错误
         */
        public static <T> Result<T> addressPermissionDenied(Long userId, Long addressId) {
            String message = String.format("用户 %d 无权访问地址 %d", userId, addressId);
            return forbidden(message);
        }

        /**
         * 地址操作成功
         */
        public static <T> Result<T> operationSuccess(String operation, T data) {
            String message = String.format("地址%s成功", operation);
            return success(message, data);
        }
    }

    /**
     * 文件上传相关响应
     */
    public static class FileResponse {

        /**
         * 文件上传成功
         */
        public static <T> Result<T> uploadSuccess(T data) {
            return success("文件上传成功", data);
        }

        /**
         * 文件上传失败
         */
        public static <T> Result<T> uploadFailed(String message) {
            String errorMessage = message != null ? message : "文件上传失败";
            return businessError(errorMessage);
        }

        /**
         * 文件大小超限
         */
        public static <T> Result<T> fileSizeExceeded(String maxSize) {
            String message = String.format("文件大小超出限制，最大允许: %s", maxSize);
            return badRequest(message);
        }

        /**
         * 文件类型不支持
         */
        public static <T> Result<T> unsupportedFileType(String allowedTypes) {
            String message = String.format("不支持的文件类型，允许的类型: %s", allowedTypes);
            return badRequest(message);
        }
    }

    /**
     * 分页响应辅助方法
     */
    public static class PageResponse {

        /**
         * 分页查询成功
         */
        public static <T> Result<T> success(T pageData) {
            return Result.success("查询成功", pageData);
        }

        /**
         * 分页查询成功（自定义消息）
         */
        public static <T> Result<T> success(String message, T pageData) {
            return Result.success(message, pageData);
        }

        /**
         * 分页查询失败
         */
        public static <T> Result<T> failed(String message) {
            return businessError(message != null ? message : "分页查询失败");
        }
    }
}
