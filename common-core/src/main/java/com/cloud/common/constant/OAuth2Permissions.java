package com.cloud.common.constant;








public final class OAuth2Permissions {

    
    public static final String READ = "read";
    public static final String WRITE = "write";
    
    public static final String USER_READ = "user:read";
    public static final String USER_WRITE = "user:write";
    public static final String USER_DELETE = "user:delete";
    
    public static final String ADMIN_READ = "admin:read";
    public static final String ADMIN_WRITE = "admin:write";
    public static final String ADMIN_DELETE = "admin:delete";
    
    public static final String MERCHANT_READ = "merchant:read";
    public static final String MERCHANT_WRITE = "merchant:write";
    public static final String MERCHANT_MANAGE = "merchant:manage";
    public static final String MERCHANT_AUTH = "merchant:auth";
    
    public static final String SYSTEM_READ = "system:read";
    public static final String SYSTEM_WRITE = "system:write";
    public static final String SYSTEM_MANAGE = "system:manage";
    
    public static final String HAS_ROLE_ADMIN = "hasRole('ADMIN')";
    public static final String HAS_ROLE_USER = "hasRole('USER')";
    public static final String HAS_ROLE_MERCHANT = "hasRole('MERCHANT')";

    private OAuth2Permissions() {
        
    }
}
