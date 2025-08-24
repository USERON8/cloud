package com.cloud.merchant.exception;

/**
 * 商家权限异常
 * 当商家尝试执行无权限的操作时抛出此异常
 */
public class MerchantPermissionException extends MerchantServiceException {
    
    public MerchantPermissionException(String message) {
        super(40302, "商家权限不足: " + message);
    }
}