package com.cloud.auth.service;

import com.cloud.api.user.UserFeignClient;
import com.cloud.common.domain.dto.user.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * 权限检查服务
 * 用于处理复杂的权限检查逻辑，避免在业务代码中直接编写复杂的权限检查
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionCheckService {

    private final UserFeignClient userFeignClient;

    /**
     * 检查用户是否具有指定角色
     *
     * @param authentication 认证信息
     * @param role           角色名称（如 ROLE_ADMIN）
     * @return 是否具有角色
     */
    public boolean hasRole(Authentication authentication, String role) {
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
    public boolean hasAuthority(Authentication authentication, String authority) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        return authorities.stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(authority));
    }

    /**
     * 检查用户是否是管理员
     *
     * @param authentication 认证信息
     * @return 是否是管理员
     */
    public boolean isAdmin(Authentication authentication) {
        return hasRole(authentication, "ROLE_ADMIN");
    }

    /**
     * 检查用户是否是商家
     *
     * @param authentication 认证信息
     * @return 是否是商家
     */
    public boolean isMerchant(Authentication authentication) {
        return hasRole(authentication, "ROLE_MERCHANT");
    }

    /**
     * 检查用户是否是普通用户
     *
     * @param authentication 认证信息
     * @return 是否是普通用户
     */
    public boolean isUser(Authentication authentication) {
        return hasRole(authentication, "ROLE_USER");
    }

    /**
     * 检查用户是否可以访问指定资源
     *
     * @param authentication 认证信息
     * @param resource       资源名称
     * @param action         操作类型（read, write, delete等）
     * @return 是否可以访问
     */
    public boolean canAccessResource(Authentication authentication, String resource, String action) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // 管理员可以访问所有资源
        if (isAdmin(authentication)) {
            return true;
        }

        // 从JWT中获取用户信息
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt) {
            Jwt jwt = (Jwt) principal;
            String userType = jwt.getClaimAsString("user_type");
            Long userId = jwt.getClaim("user_id");

            // 根据用户类型和资源类型进行判断
            switch (userType) {
                case "ADMIN":
                    return true;
                case "MERCHANT":
                    // 商家只能访问与自己相关的资源
                    return isMerchantResource(resource, userId);
                case "USER":
                    // 普通用户只能访问自己的资源
                    return isUserResource(resource, userId);
                default:
                    return false;
            }
        }

        return false;
    }

    /**
     * 检查资源是否属于商家
     *
     * @param resource   资源名称
     * @param merchantId 商家ID
     * @return 是否属于商家
     */
    private boolean isMerchantResource(String resource, Long merchantId) {
        // 这里可以根据具体业务逻辑实现
        // 例如：检查商品是否属于指定商家、订单是否来自该商家等
        return true;
    }

    /**
     * 检查资源是否属于用户
     *
     * @param resource 资源名称
     * @param userId   用户ID
     * @return 是否属于用户
     */
    private boolean isUserResource(String resource, Long userId) {
        // 这里可以根据具体业务逻辑实现
        // 例如：检查订单是否属于指定用户、地址是否是用户的等
        return true;
    }

    /**
     * 获取当前用户ID
     *
     * @param authentication 认证信息
     * @return 用户ID
     */
    public Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt) {
            Jwt jwt = (Jwt) principal;
            return jwt.getClaim("user_id");
        }

        return null;
    }

    /**
     * 获取当前用户类型
     *
     * @param authentication 认证信息
     * @return 用户类型
     */
    public String getCurrentUserType(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt) {
            Jwt jwt = (Jwt) principal;
            return jwt.getClaimAsString("user_type");
        }

        return null;
    }

    /**
     * 获取完整的用户信息
     *
     * @param authentication 认证信息
     * @return 用户DTO
     */
    public UserDTO getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt) {
            Jwt jwt = (Jwt) principal;
            String username = jwt.getSubject();
            try {
                return userFeignClient.findByUsername(username);
            } catch (Exception e) {
                log.error("获取用户信息失败: username={}", username, e);
                return null;
            }
        }

        return null;
    }
}