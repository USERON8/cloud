package com.cloud.product.utils;

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
        return Result.success(message);
    }

    /**
     * 操作失败响应
     */
    public static <T> Result<T> error(String message) {
        return Result.error("BUSINESS_ERROR", message);
    }

    /**
     * 操作失败响应（带错误代码）
     */
    public static <T> Result<T> error(String code, String message) {
        return Result.error(code, message);
    }

    /**
     * 权限不足响应
     */
    public static <T> Result<T> forbidden(String message) {
        String errorMessage = message != null ? message : "无权限执行此操作";
        log.warn("权限验证失败: {}", errorMessage);
        return Result.error("FORBIDDEN", errorMessage);
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
        return Result.error("UNAUTHORIZED", errorMessage);
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
        return Result.error("BAD_REQUEST", errorMessage);
    }

    /**
     * 资源未找到响应
     */
    public static <T> Result<T> notFound(String message) {
        String errorMessage = message != null ? message : "请求的资源不存在";
        log.warn("资源未找到: {}", errorMessage);
        return Result.error("NOT_FOUND", errorMessage);
    }

    /**
     * 系统错误响应
     */
    public static <T> Result<T> systemError(String message) {
        String errorMessage = message != null ? message : "系统内部错误";
        log.error("系统错误: {}", errorMessage);
        return Result.error("SYSTEM_ERROR", errorMessage);
    }

    /**
     * 业务错误响应
     */
    public static <T> Result<T> businessError(String message) {
        String errorMessage = message != null ? message : "业务处理失败";
        log.warn("业务错误: {}", errorMessage);
        return Result.error("BUSINESS_ERROR", errorMessage);
    }

    /**
     * 数据验证失败响应
     */
    public static <T> Result<T> validationError(String message) {
        String errorMessage = message != null ? message : "数据验证失败";
        log.warn("数据验证失败: {}", errorMessage);
        return Result.error("VALIDATION_ERROR", errorMessage);
    }

    /**
     * 商品相关错误响应
     */
    public static class ProductResponse {

        /**
         * 商品不存在
         */
        public static <T> Result<T> productNotFound(Long productId) {
            String message = productId != null ?
                    String.format("商品不存在，商品ID: %d", productId) : "商品不存在";
            return notFound(message);
        }

        /**
         * 商品不存在（按商品名称）
         */
        public static <T> Result<T> productNotFound(String productName) {
            String message = productName != null ?
                    String.format("商品不存在，商品名称: %s", productName) : "商品不存在";
            return notFound(message);
        }

        /**
         * 商品已存在
         */
        public static <T> Result<T> productAlreadyExists(String productName) {
            String message = productName != null ?
                    String.format("商品已存在，商品名称: %s", productName) : "商品已存在";
            return businessError(message);
        }

        /**
         * 商品状态错误
         */
        public static <T> Result<T> productStatusError(Long productId, String operation) {
            String message = String.format("商品状态不允许执行操作: %s，商品ID: %d", operation, productId);
            return businessError(message);
        }

        /**
         * 库存不足
         */
        public static <T> Result<T> stockInsufficient(Long productId, Integer required, Integer available) {
            String message = String.format("库存不足，商品ID: %d，需要: %d，可用: %d", productId, required, available);
            return businessError(message);
        }

        /**
         * 商品操作成功
         */
        public static <T> Result<T> operationSuccess(String operation, T data) {
            String message = String.format("商品%s成功", operation);
            return success(message, data);
        }

        /**
         * 批量商品操作成功
         */
        public static <T> Result<T> batchOperationSuccess(String operation, int count, T data) {
            String message = String.format("成功%s%d个商品", operation, count);
            return success(message, data);
        }
    }

    /**
     * 分类相关错误响应
     */
    public static class CategoryResponse {

        /**
         * 分类不存在
         */
        public static <T> Result<T> categoryNotFound(Long categoryId) {
            String message = categoryId != null ?
                    String.format("商品分类不存在，分类ID: %d", categoryId) : "商品分类不存在";
            return notFound(message);
        }

        /**
         * 分类不存在（按分类名称）
         */
        public static <T> Result<T> categoryNotFound(String categoryName) {
            String message = categoryName != null ?
                    String.format("商品分类不存在，分类名称: %s", categoryName) : "商品分类不存在";
            return notFound(message);
        }

        /**
         * 分类已存在
         */
        public static <T> Result<T> categoryAlreadyExists(String categoryName) {
            String message = categoryName != null ?
                    String.format("商品分类已存在，分类名称: %s", categoryName) : "商品分类已存在";
            return businessError(message);
        }
    }

    /**
     * 店铺相关错误响应
     */
    public static class ShopResponse {

        /**
         * 店铺不存在
         */
        public static <T> Result<T> shopNotFound(Long shopId) {
            String message = shopId != null ?
                    String.format("店铺不存在，店铺ID: %d", shopId) : "店铺不存在";
            return notFound(message);
        }

        /**
         * 店铺不存在（按店铺名称）
         */
        public static <T> Result<T> shopNotFound(String shopName) {
            String message = shopName != null ?
                    String.format("店铺不存在，店铺名称: %s", shopName) : "店铺不存在";
            return notFound(message);
        }

        /**
         * 店铺已存在
         */
        public static <T> Result<T> shopAlreadyExists(String shopName) {
            String message = shopName != null ?
                    String.format("店铺已存在，店铺名称: %s", shopName) : "店铺已存在";
            return businessError(message);
        }

        /**
         * 店铺状态错误
         */
        public static <T> Result<T> shopStatusError(Long shopId, String operation) {
            String message = String.format("店铺状态不允许执行操作: %s，店铺ID: %d", operation, shopId);
            return businessError(message);
        }

        /**
         * 店铺权限错误
         */
        public static <T> Result<T> shopPermissionDenied(Long merchantId, Long shopId) {
            String message = String.format("商家 %d 无权访问店铺 %d", merchantId, shopId);
            return forbidden(message);
        }

        /**
         * 店铺操作成功
         */
        public static <T> Result<T> operationSuccess(String operation, T data) {
            String message = String.format("店铺%s成功", operation);
            return success(message, data);
        }

        /**
         * 批量店铺操作成功
         */
        public static <T> Result<T> batchOperationSuccess(String operation, int count, T data) {
            String message = String.format("成功%s%d个店铺", operation, count);
            return success(message, data);
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
