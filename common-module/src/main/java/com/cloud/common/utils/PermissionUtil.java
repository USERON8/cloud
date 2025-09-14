package com.cloud.common.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;

/**
 * 权限检查工具类
 * 提供通用的权限检查方法，供各业务服务使用
 */
public class PermissionUtil {

    /**
     * 检查用户是否具有指定角色
     *
     * @param authentication 认证信息
     * @param role           角色名称（如 ROLE_ADMIN）
     * @return 是否具有角色
     */
    public static boolean hasRole(Authentication authentication, String role) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        return authorities.stream()
                .anyMatch(authority -> authority.getAuthority().equals(role));
    }

    /**
     * 检查用户是否具有指定权限
     *
     * @param authentication 认证信息
     * @param authority      权限名称（如 user:read）
     * @return 是否具有权限
     */
    public static boolean hasAuthority(Authentication authentication, String authority) {
        return hasRole(authentication, authority);
    }

    /**
     * 检查JWT令牌是否具有指定范围
     *
     * @param jwt   JWT令牌
     * @param scope 范围名称（如 read）
     * @return 是否具有范围
     */
    public static boolean hasScope(Jwt jwt, String scope) {
        if (jwt == null) {
            return false;
        }

        List<String> scopes = jwt.getClaimAsStringList("scope");
        return scopes != null && scopes.contains(scope);
    }

    /**
     * 获取JWT中的用户ID
     *
     * @param jwt JWT令牌
     * @return 用户ID
     */
    public static Long getUserIdFromJwt(Jwt jwt) {
        if (jwt == null) {
            return null;
        }

        try {
            return jwt.getClaim("user_id");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取JWT中的用户类型
     *
     * @param jwt JWT令牌
     * @return 用户类型
     */
    public static String getUserTypeFromJwt(Jwt jwt) {
        if (jwt == null) {
            return null;
        }

        return jwt.getClaimAsString("user_type");
    }

    /**
     * 获取JWT中的用户昵称
     *
     * @param jwt JWT令牌
     * @return 用户昵称
     */
    public static String getNicknameFromJwt(Jwt jwt) {
        if (jwt == null) {
            return null;
        }

        return jwt.getClaimAsString("nickname");
    }

    /**
     * 获取JWT中的部门ID
     *
     * @param jwt JWT令牌
     * @return 部门ID
     */
    public static Long getDepartmentIdFromJwt(Jwt jwt) {
        if (jwt == null) {
            return null;
        }

        try {
            return jwt.getClaim("department_id");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取JWT中的权限列表
     *
     * @param jwt JWT令牌
     * @return 权限列表
     */
    public static List<String> getPermissionsFromJwt(Jwt jwt) {
        if (jwt == null) {
            return null;
        }

        try {
            return jwt.getClaim("permissions");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 检查用户是否可以访问指定资源（基于声明的访问控制）
     *
     * @param jwt        JWT令牌
     * @param resource   资源名称
     * @param action     操作类型
     * @param resourceId 资源ID
     * @return 是否可以访问
     */
    public static boolean canAccessResource(Jwt jwt, String resource, String action, Object resourceId) {
        if (jwt == null) {
            return false;
        }

        // 管理员可以访问所有资源
        String userType = getUserTypeFromJwt(jwt);
        if ("ADMIN".equals(userType)) {
            return true;
        }

        Long userId = getUserIdFromJwt(jwt);
        if (userId == null) {
            return false;
        }

        // 根据资源类型进行不同的检查
        return switch (resource) {
            case "order" ->
                // 检查订单是否属于用户
                    checkOrderOwnership(jwt, resourceId);
            case "address" ->
                // 检查地址是否属于用户
                    checkAddressOwnership(jwt, resourceId);
            default ->
                // 默认情况下，用户只能访问自己的资源
                    true;
        };
    }

    /**
     * 检查订单是否属于用户
     *
     * @param jwt     JWT令牌
     * @param orderId 订单ID
     * @return 是否属于用户
     */
    private static boolean checkOrderOwnership(Jwt jwt, Object orderId) {
        // 这里应该调用订单服务检查订单是否属于该用户
        // 为简化示例，我们假设检查通过
        return true;
    }

    /**
     * 检查地址是否属于用户
     *
     * @param jwt       JWT令牌
     * @param addressId 地址ID
     * @return 是否属于用户
     */
    private static boolean checkAddressOwnership(Jwt jwt, Object addressId) {
        // 这里应该调用用户服务检查地址是否属于该用户
        // 为简化示例，我们假设检查通过
        return true;
    }
}