package com.cloud.user.constants;

/**
 * OAuth2.0权限常量类
 * 定义标准的OAuth2.0权限、角色和作用域
 *
 * @author what's up
 */
public final class OAuth2Permissions {

    /**
     * 系统管理员角色
     * 拥有系统最高权限
     */
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    // ===================== OAuth2.0 标准角色 =====================
    /**
     * 普通用户角色
     * 基础用户权限
     */
    public static final String ROLE_USER = "ROLE_USER";
    /**
     * 商户角色
     * 商户相关操作权限
     */
    public static final String ROLE_MERCHANT = "ROLE_MERCHANT";
    /**
     * 客服角色
     * 客服相关操作权限
     */
    public static final String ROLE_SUPPORT = "ROLE_SUPPORT";
    /**
     * 管理员权限检查表达式
     */
    public static final String HAS_ROLE_ADMIN = "hasRole('ADMIN')";

    // ===================== OAuth2.0 标准权限表达式 =====================
    /**
     * 用户权限检查表达式
     */
    public static final String HAS_ROLE_USER = "hasRole('USER')";
    /**
     * 商户权限检查表达式
     */
    public static final String HAS_ROLE_MERCHANT = "hasRole('MERCHANT')";
    /**
     * 客服权限检查表达式
     */
    public static final String HAS_ROLE_SUPPORT = "hasRole('SUPPORT')";
    /**
     * 管理员或客服权限检查表达式
     */
    public static final String HAS_ROLE_ADMIN_OR_SUPPORT = "hasRole('ADMIN') or hasRole('SUPPORT')";
    /**
     * 用户或商户权限检查表达式
     */
    public static final String HAS_ROLE_USER_OR_MERCHANT = "hasRole('USER') or hasRole('MERCHANT')";
    /**
     * 读取作用域
     */
    public static final String SCOPE_READ = "SCOPE_read";

    // ===================== OAuth2.0 标准作用域 (Scopes) =====================
    /**
     * 写入作用域
     */
    public static final String SCOPE_WRITE = "SCOPE_write";
    /**
     * 内部API作用域
     */
    public static final String SCOPE_INTERNAL_API = "SCOPE_internal_api";
    /**
     * 用户管理作用域
     */
    public static final String SCOPE_USER_MANAGE = "SCOPE_user_manage";
    /**
     * 商户管理作用域
     */
    public static final String SCOPE_MERCHANT_MANAGE = "SCOPE_merchant_manage";
    /**
     * 内部API权限检查表达式
     */
    public static final String HAS_SCOPE_INTERNAL_API = "hasAuthority('SCOPE_internal_api')";

    // ===================== OAuth2.0 权限组合表达式 =====================
    /**
     * 用户管理权限检查表达式
     */
    public static final String HAS_SCOPE_USER_MANAGE = "hasAuthority('SCOPE_user_manage')";
    /**
     * 商户管理权限检查表达式
     */
    public static final String HAS_SCOPE_MERCHANT_MANAGE = "hasAuthority('SCOPE_merchant_manage')";
    /**
     * 读取权限检查表达式
     */
    public static final String HAS_SCOPE_READ = "hasAuthority('SCOPE_read')";
    /**
     * 写入权限检查表达式
     */
    public static final String HAS_SCOPE_WRITE = "hasAuthority('SCOPE_write')";
    /**
     * 管理员且有写入权限
     */
    public static final String ADMIN_WITH_WRITE = "hasRole('ADMIN') and hasAuthority('SCOPE_write')";

    // ===================== 复合权限表达式 =====================
    /**
     * 用户管理权限（管理员或有用户管理作用域）
     */
    public static final String USER_MANAGEMENT = "hasRole('ADMIN') or hasAuthority('SCOPE_user_manage')";
    /**
     * 商户管理权限（管理员或有商户管理作用域）
     */
    public static final String MERCHANT_MANAGEMENT = "hasRole('ADMIN') or hasAuthority('SCOPE_merchant_manage')";
    /**
     * 个人数据访问权限（本人或管理员）
     */
    public static final String PERSONAL_DATA_ACCESS = "hasRole('ADMIN') or @userPermissionHelper.isOwner(authentication, #userId)";
    /**
     * 商户数据访问权限（商户本人或管理员）
     */
    public static final String MERCHANT_DATA_ACCESS = "hasRole('ADMIN') or @userPermissionHelper.isMerchantOwner(authentication, #merchantId)";
    /**
     * 检查用户是否为管理员
     */
    public static final String IS_ADMIN = "@userPermissionHelper.isAdmin(authentication)";

    // ===================== 方法级权限检查 =====================
    /**
     * 检查用户是否为商户
     */
    public static final String IS_MERCHANT = "@userPermissionHelper.isMerchant(authentication)";
    /**
     * 检查是否为数据所有者
     */
    public static final String IS_OWNER = "@userPermissionHelper.isOwner(authentication, #id)";
    /**
     * 检查是否有特定资源的访问权限
     */
    public static final String HAS_RESOURCE_ACCESS = "@userPermissionHelper.hasResourceAccess(authentication, #resourceId, #resourceType)";

    private OAuth2Permissions() {
        // 工具类，禁止实例化
    }
}
