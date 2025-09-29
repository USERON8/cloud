package com.cloud.common.security;

/**
 * OAuth2权限常量类
 * 
 * @author CloudDevAgent
 * @since 2025-09-28
 */
public final class OAuth2Permissions {
    
    private OAuth2Permissions() {
        // 私有构造函数，防止实例化
    }
    
    /**
     * 管理员角色权限
     */
    public static final String HAS_ROLE_ADMIN = "ROLE_ADMIN";
    
    /**
     * 用户角色权限
     */
    public static final String HAS_ROLE_USER = "ROLE_USER";
    
    /**
     * 商家角色权限
     */
    public static final String HAS_ROLE_MERCHANT = "ROLE_MERCHANT";
    
    /**
     * 内部API权限
     */
    public static final String HAS_SCOPE_INTERNAL = "SCOPE_internal_api";
    
    /**
     * 读权限
     */
    public static final String HAS_SCOPE_READ = "SCOPE_read";
    
    /**
     * 写权限
     */
    public static final String HAS_SCOPE_WRITE = "SCOPE_write";
}
