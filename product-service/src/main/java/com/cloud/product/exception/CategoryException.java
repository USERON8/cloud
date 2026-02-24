package com.cloud.product.exception;

import com.cloud.common.exception.BusinessException;
import lombok.Getter;

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

    public static class CategoryNotFoundException extends CategoryException {
        public CategoryNotFoundException(Long categoryId) {
            super(404, "Category not found: " + categoryId);
        }

        public CategoryNotFoundException(String categoryName) {
            super(404, "Category not found: " + categoryName);
        }
    }

    public static class CategoryAlreadyExistsException extends CategoryException {
        public CategoryAlreadyExistsException(String categoryName) {
            super(409, "Category already exists: " + categoryName);
        }
    }

    public static class CategoryStatusException extends CategoryException {
        public CategoryStatusException(String message) {
            super(400, message);
        }

        public CategoryStatusException(Long categoryId, String status) {
            super(400, "Invalid category status. categoryId: " + categoryId + ", status: " + status);
        }
    }

    public static class CategoryHierarchyException extends CategoryException {
        public CategoryHierarchyException(String message) {
            super(400, message);
        }
    }

    public static class CategoryHasChildrenException extends CategoryException {
        public CategoryHasChildrenException(Long categoryId) {
            super(400, "Category has child categories and cannot be deleted: " + categoryId);
        }
    }

    public static class CategoryHasProductsException extends CategoryException {
        public CategoryHasProductsException(Long categoryId) {
            super(400, "Category contains products and cannot be deleted: " + categoryId);
        }
    }
}
