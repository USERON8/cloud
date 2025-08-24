package com.cloud.product.exception;

/**
 * 分类不存在异常
 * 当尝试操作一个不存在的商品分类时抛出此异常
 */
public class CategoryNotFoundException extends ProductServiceException {
    
    public CategoryNotFoundException(Long categoryId) {
        super(40405, "商品分类不存在: " + categoryId);
    }
    
    public CategoryNotFoundException(String categoryName) {
        super(40405, "商品分类不存在: " + categoryName);
    }
}