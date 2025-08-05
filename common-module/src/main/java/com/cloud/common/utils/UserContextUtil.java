package com.cloud.common.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 用户上下文工具类
 * 提供获取当前登录用户信息的方法
 */
public class UserContextUtil {

    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();

    /**
     * 从Spring Security上下文中获取当前用户ID
     *
     * @return 当前用户ID
     */
    public static Long getCurrentUserId() {
        // 首先尝试从ThreadLocal获取
        Long userId = USER_ID_HOLDER.get();
        if (userId != null) {
            return userId;
        }

        // 如果ThreadLocal中没有，则从SecurityContext中获取
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof Long) {
                    return (Long) principal;
                }
                // 如果principal是UserDetails的实现类，可以根据实际情况从中获取用户ID
                // 这里假设在JWT中或者认证信息中可以获取到用户ID
            }
        } catch (Exception e) {
            // 安全上下文可能不可用，忽略异常
        }

        return null;
    }

    /**
     * 将用户ID存储到ThreadLocal中
     *
     * @param userId 用户ID
     */
    public static void setCurrentUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    /**
     * 清除ThreadLocal中的用户ID
     */
    public static void clear() {
        USER_ID_HOLDER.remove();
    }
}
