package com.cloud.merchant.exception;

/**
 * 商家状态异常
 * 当商家状态不满足操作条件时抛出此异常
 */
public class MerchantStatusException extends MerchantServiceException {
    
    public MerchantStatusException(String message) {
        super(40008, "商家状态错误: " + message);
    }
}