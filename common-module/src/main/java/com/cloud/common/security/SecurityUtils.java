package com.cloud.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * 通用安全工具类
 * 提供基础的用户信息获取功能，供MyBatis Plus元数据处理器使用
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
public class SecurityUtils {

    /**
     * 获取当前用户ID（Long类型）
     * 用于MyBatis Plus自动填充
     *
     * @return 当前用户ID，如果获取失败返回0L
     */
    public static Long getCurrentUserId() {
        try {
            String userIdStr = getCurrentUserIdAsString();
            if (userIdStr != null && !userIdStr.isEmpty()) {
                return Long.parseLong(userIdStr);
            }
        } catch (Exception e) {
            // 忽略异常，返回默认值
        }
        return 0L; // 系统用户ID
    }

    /**
     * 获取当前用户ID（String类型）
     *
     * @return 当前用户ID字符串
     */
    public static String getCurrentUserIdAsString() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken) {
            Jwt jwt = ((JwtAuthenticationToken) authentication).getToken();
            return jwt.getClaimAsString("user_id");
        }
        return null;
    }

    /**
     * 获取当前用户名
     *
     * @return 当前用户名
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : null;
    }

    /**
     * 检查是否已认证
     *
     * @return 是否已认证
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName());
    }

    /**
     * 获取当前认证对象
     *
     * @return 当前认证对象
     */
    public static Authentication getCurrentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
