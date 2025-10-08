package com.cloud.common.constant;

/**
 * OAuth2权限常量
 * 定义系统中使用的OAuth2权限范围
 *
 * @author what's up
 * @since 2025-01-28
 */
public final class OAuth2Permissions {

    // 基础权限
    public static final String READ = "read";
    public static final String WRITE = "write";
    // 用户权限
    public static final String USER_READ = "user:read";
    public static final String USER_WRITE = "user:write";
    public static final String USER_DELETE = "user:delete";
    // 管理员权限
    public static final String ADMIN_READ = "admin:read";
    public static final String ADMIN_WRITE = "admin:write";
    public static final String ADMIN_DELETE = "admin:delete";
    // 商户权限
    public static final String MERCHANT_READ = "merchant:read";
    public static final String MERCHANT_WRITE = "merchant:write";
    public static final String MERCHANT_MANAGE = "merchant:manage";
    public static final String MERCHANT_AUTH = "merchant:auth";
    // 系统权限
    public static final String SYSTEM_READ = "system:read";
    public static final String SYSTEM_WRITE = "system:write";
    public static final String SYSTEM_MANAGE = "system:manage";
    // SpEL 表达式常量
    public static final String HAS_ROLE_ADMIN = "hasRole('ADMIN')";
    public static final String HAS_ROLE_USER = "hasRole('USER')";
    public static final String HAS_ROLE_MERCHANT = "hasRole('MERCHANT')";

    private OAuth2Permissions() {
        // 工具类，禁止实例化
    }
}
