package com.cloud.common.config;

import com.cloud.common.enums.UserType;
import com.cloud.common.exception.PermissionException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * 权限检查器
 *
 * @author cloud
 * @date 2024-01-20
 */
@Component
public class PermissionChecker {

    /**
     * 检查用户类型权限
     *
     * @param requiredType 要求的用户类型
     * @param currentType  当前用户类型
     * @return 是否有权限
     */
    public boolean checkUserType(UserType requiredType, UserType currentType) {
        if (requiredType == null || currentType == null) {
            return false;
        }

        return requiredType.equals(currentType);
    }

    /**
     * 检查管理员权限
     *
     * @param currentType 当前用户类型
     * @return 是否是管理员
     */
    public boolean checkAdminPermission(UserType currentType) {
        return UserType.ADMIN.equals(currentType);
    }

    /**
     * 检查商家权限
     *
     * @param currentType 当前用户类型
     * @return 是否是商家
     */
    public boolean checkMerchantPermission(UserType currentType) {
        return UserType.ADMIN.equals(currentType);
    }

    /**
     * 检查用户权限
     *
     * @param currentType 当前用户类型
     * @return 是否是普通用户
     */
    public boolean checkUserPermission(UserType currentType) {
        return UserType.USER.equals(currentType);
    }

    /**
     * 断言用户类型，如果不符合则抛出异常
     *
     * @param requiredType 要求的用户类型字符串
     * @throws PermissionException 权限不足异常
     */
    public void assertUserType(String requiredType) {
        UserType currentType = getCurrentUserType();
        UserType required;

        try {
            required = UserType.valueOf(requiredType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new PermissionException("INVALID_USER_TYPE", "无效的用户类型: " + requiredType);
        }

        if (!checkUserType(required, currentType)) {
            throw new PermissionException("ACCESS_DENIED",
                    "当前用户类型[" + (currentType != null ? currentType.name() : "未知") + "]无权限访问，需要用户类型: " + requiredType);
        }
    }

    /**
     * 获取当前用户类型
     *
     * @return 当前用户类型
     */
    private UserType getCurrentUserType() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        if (authentication instanceof JwtAuthenticationToken jwtToken) {
            String userType = jwtToken.getToken().getClaimAsString("user_type");
            if (userType != null) {
                try {
                    return UserType.valueOf(userType.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
        }

        // 从authorities中获取用户类型
        return authentication.getAuthorities().stream()
                .filter(auth -> auth.getAuthority().startsWith("USER_TYPE_"))
                .findFirst()
                .map(auth -> {
                    String type = auth.getAuthority().substring("USER_TYPE_".length());
                    try {
                        return UserType.valueOf(type);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .orElse(null);
    }
}
