package com.cloud.product.exception;

import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BusinessException;

public class ProductServiceException extends BusinessException {

    public ProductServiceException(int code, String message) {
        super(code, message);
    }

    public ProductServiceException(String message) {
        super(message);
    }

    public ProductServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProductServiceException(ResultCode resultCode) {
        super(resultCode);
    }

    public ProductServiceException(ResultCode resultCode, String message) {
        super(resultCode, message);
    }

    public ProductServiceException(ResultCode resultCode, Throwable cause) {
        super(resultCode, cause);
    }

    public ProductServiceException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }

    public static class ProductNotFoundException extends ProductServiceException {
        public ProductNotFoundException(String message) {
            super(ResultCode.PRODUCT_NOT_FOUND, message);
        }

        public ProductNotFoundException(Long productId) {
            super(ResultCode.PRODUCT_NOT_FOUND, "Product not found, productId: " + productId);
        }
    }

    public static class ProductAlreadyExistsException extends ProductServiceException {
        public ProductAlreadyExistsException(String message) {
            super(ResultCode.PRODUCT_ALREADY_EXISTS, message);
        }
    }

    public static class ProductStatusException extends ProductServiceException {
        public ProductStatusException(String message) {
            super(ResultCode.PRODUCT_STATUS_ERROR, message);
        }

        public ProductStatusException(Long productId, String currentStatus, String targetStatus) {
            super(ResultCode.PRODUCT_STATUS_ERROR,
                    String.format(
                            "Invalid product status transition, productId: %d, currentStatus: %s, targetStatus: %s",
                            productId, currentStatus, targetStatus
                    ));
        }
    }

    public static class CategoryNotFoundException extends ProductServiceException {
        public CategoryNotFoundException(String message) {
            super(ResultCode.CATEGORY_NOT_FOUND, message);
        }

        public CategoryNotFoundException(Long categoryId) {
            super(ResultCode.CATEGORY_NOT_FOUND, "Category not found, categoryId: " + categoryId);
        }
    }

    public static class StockInsufficientException extends ProductServiceException {
        public StockInsufficientException(String message) {
            super(ResultCode.STOCK_INSUFFICIENT, message);
        }

        public StockInsufficientException(Long productId, Integer required, Integer available) {
            super(ResultCode.STOCK_INSUFFICIENT,
                    String.format(
                            "Insufficient stock, productId: %d, required: %d, available: %d",
                            productId, required, available
                    ));
        }
    }

    public static class ProductValidationException extends ProductServiceException {
        public ProductValidationException(String message) {
            super(ResultCode.PARAM_ERROR, message);
        }

        public ProductValidationException(String field, String message) {
            super(ResultCode.PARAM_ERROR, String.format("Field %s %s", field, message));
        }
    }

    public static class ProductPermissionException extends ProductServiceException {
        public ProductPermissionException(String message) {
            super(ResultCode.FORBIDDEN.getCode(), message);
        }

        public ProductPermissionException(Long userId, Long productId) {
            super(ResultCode.FORBIDDEN.getCode(),
                    String.format("User %d has no permission to access product %d", userId, productId));
        }
    }
}
