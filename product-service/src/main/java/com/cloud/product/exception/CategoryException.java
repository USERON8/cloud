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
            super(404, "鍒嗙被涓嶅瓨鍦? " + categoryId);
        }

        public CategoryNotFoundException(String categoryName) {
            super(404, "鍒嗙被涓嶅瓨鍦? " + categoryName);
        }
    }

    


    public static class CategoryAlreadyExistsException extends CategoryException {
        public CategoryAlreadyExistsException(String categoryName) {
            super(409, "鍒嗙被宸插瓨鍦? " + categoryName);
        }
    }

    


    public static class CategoryStatusException extends CategoryException {
        public CategoryStatusException(String message) {
            super(400, message);
        }

        public CategoryStatusException(Long categoryId, String status) {
            super(400, "鍒嗙被鐘舵€佸紓甯? ID: " + categoryId + ", 鐘舵€? " + status);
        }
    }

    


    public static class CategoryHierarchyException extends CategoryException {
        public CategoryHierarchyException(String message) {
            super(400, message);
        }
    }

    


    public static class CategoryHasChildrenException extends CategoryException {
        public CategoryHasChildrenException(Long categoryId) {
            super(400, "鍒嗙被鍖呭惈瀛愬垎绫伙紝鏃犳硶鍒犻櫎: " + categoryId);
        }
    }

    


    public static class CategoryHasProductsException extends CategoryException {
        public CategoryHasProductsException(Long categoryId) {
            super(400, "鍒嗙被涓嬪瓨鍦ㄥ晢鍝侊紝鏃犳硶鍒犻櫎: " + categoryId);
        }
    }
}
