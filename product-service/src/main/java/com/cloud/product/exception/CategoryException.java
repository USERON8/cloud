package com.cloud.product.exception;

import com.cloud.common.exception.BusinessException;
import lombok.Getter;

/**
 * 分类服务异常类
 * 用于处理商品分类业务相关的异常情况
 *
 * @author what's up
 * @since 1.0.0
 */
@Getter
public class CategoryException extends BusinessException {

    public CategoryException(int code, String message) {
        super(code, message);
    }

    public CategoryException(String message) {
        super(message);
    }

    public CategoryException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }

    /**
     * 分类不存在异常
     */
    public static class CategoryNotFoundException extends CategoryException {
        public CategoryNotFoundException(Long categoryId) {
            super(404, "分类不存在: " + categoryId);
        }

        public CategoryNotFoundException(String categoryName) {
            super(404, "分类不存在: " + categoryName);
        }
    }

    /**
     * 分类已存在异常
     */
    public static class CategoryAlreadyExistsException extends CategoryException {
        public CategoryAlreadyExistsException(String categoryName) {
            super(409, "分类已存在: " + categoryName);
        }
    }

    /**
     * 分类状态异常
     */
    public static class CategoryStatusException extends CategoryException {
        public CategoryStatusException(String message) {
            super(400, message);
        }

        public CategoryStatusException(Long categoryId, String status) {
            super(400, "分类状态异常, ID: " + categoryId + ", 状态: " + status);
        }
    }

    /**
     * 分类层级异常
     */
    public static class CategoryHierarchyException extends CategoryException {
        public CategoryHierarchyException(String message) {
            super(400, message);
        }
    }

    /**
     * 分类包含子分类异常
     */
    public static class CategoryHasChildrenException extends CategoryException {
        public CategoryHasChildrenException(Long categoryId) {
            super(400, "分类包含子分类，无法删除: " + categoryId);
        }
    }

    /**
     * 分类包含商品异常
     */
    public static class CategoryHasProductsException extends CategoryException {
        public CategoryHasProductsException(Long categoryId) {
            super(400, "分类下存在商品，无法删除: " + categoryId);
        }
    }
}
