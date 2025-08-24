package com.cloud.product.exception;

/**
 * 商品状态异常
 * 当商品状态不满足操作条件时抛出此异常
 */
public class ProductStatusException extends ProductServiceException {
    
    public ProductStatusException(String message) {
        super(40006, "商品状态错误: " + message);
    }
}