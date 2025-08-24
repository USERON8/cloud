package com.cloud.merchant.exception;

/**
 * 商家不存在异常
 * 当尝试操作一个不存在的商家时抛出此异常
 */
public class MerchantNotFoundException extends MerchantServiceException {
    
    public MerchantNotFoundException(Long merchantId) {
        super(40407, "商家不存在: " + merchantId);
    }
    
    public MerchantNotFoundException(String merchantName) {
        super(40407, "商家不存在: " + merchantName);
    }
}