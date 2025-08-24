package com.cloud.product.exception;

/**
 * 商品不存在异常
 * 当尝试操作一个不存在的商品时抛出此异常
 */
public class ProductNotFoundException extends ProductServiceException {
    
    public ProductNotFoundException(Long productId) {
        super(40404, "商品不存在: " + productId);
    }
    
    public ProductNotFoundException(String productName) {
        super(40404, "商品不存在: " + productName);
    }
}