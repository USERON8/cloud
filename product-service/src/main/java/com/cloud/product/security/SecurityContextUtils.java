package com.cloud.product.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 商品服务安全上下文工具类
 * 提供当前用户信息获取和权限检查功能
 *
 * @author what's up
 */
@Slf4j
public class SecurityContextUtils {

    /**
     * 获取当前认证对象
     */
    public static Authentication getCurrentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 获取当前用户ID
     */
    public static String getCurrentUserId() {
        Authentication authentication = getCurrentAuthentication();
        if (authentication instanceof JwtAuthenticationToken) {
            Jwt jwt = ((JwtAuthenticationToken) authentication).getToken();
            return jwt.getClaimAsString("user_id");
        }
        return null;
    }

    /**
     * 获取当前用户名
     */
    public static String getCurrentUsername() {
        Authentication authentication = getCurrentAuthentication();
        if (authentication instanceof JwtAuthenticationToken) {
            Jwt jwt = ((JwtAuthenticationToken) authentication).getToken();
            return jwt.getClaimAsString("user_name");
        }
        return authentication != null ? authentication.getName() : null;
    }

    /**
     * 获取当前用户类型
     */
    public static String getCurrentUserType() {
        Authentication authentication = getCurrentAuthentication();
        if (authentication instanceof JwtAuthenticationToken) {
            Jwt jwt = ((JwtAuthenticationToken) authentication).getToken();
            return jwt.getClaimAsString("user_type");
        }
        return null;
    }

    /**
     * 获取当前商户ID（商品服务特有）
     */
    public static String getCurrentMerchantId() {
        Authentication authentication = getCurrentAuthentication();
        if (authentication instanceof JwtAuthenticationToken) {
            Jwt jwt = ((JwtAuthenticationToken) authentication).getToken();
            return jwt.getClaimAsString("merchant_id");
        }
        return null;
    }

    /**
     * 获取当前用户的所有权限
     */
    public static Set<String> getCurrentUserAuthorities() {
        Authentication authentication = getCurrentAuthentication();
        if (authentication == null) {
            return Collections.emptySet();
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    /**
     * 检查当前用户是否具有指定权限
     */
    public static boolean hasAuthority(String authority) {
        return getCurrentUserAuthorities().contains(authority);
    }

    /**
     * 检查当前用户是否具有指定角色
     */
    public static boolean hasRole(String role) {
        String roleAuthority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return hasAuthority(roleAuthority);
    }

    /**
     * 检查当前用户是否具有任意一个指定权限
     */
    public static boolean hasAnyAuthority(String... authorities) {
        Set<String> userAuthorities = getCurrentUserAuthorities();
        for (String authority : authorities) {
            if (userAuthorities.contains(authority)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查是否为指定用户
     */
    public static boolean isSameUser(String userId) {
        String currentUserId = getCurrentUserId();
        return currentUserId != null && currentUserId.equals(userId);
    }

    /**
     * 检查是否为指定商户
     */
    public static boolean isSameMerchant(String merchantId) {
        String currentMerchantId = getCurrentMerchantId();
        return currentMerchantId != null && currentMerchantId.equals(merchantId);
    }

    /**
     * 检查是否为指定用户类型
     */
    public static boolean hasUserType(String userType) {
        String currentUserType = getCurrentUserType();
        return currentUserType != null && currentUserType.equalsIgnoreCase(userType);
    }

    /**
     * 检查当前用户是否为管理员
     */
    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }

    /**
     * 检查当前用户是否为商户
     */
    public static boolean isMerchant() {
        return hasRole("MERCHANT");
    }

    /**
     * 检查是否已认证
     */
    public static boolean isAuthenticated() {
        Authentication authentication = getCurrentAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName());
    }

    /**
     * 获取JWT Token
     */
    public static Jwt getCurrentJwt() {
        Authentication authentication = getCurrentAuthentication();
        if (authentication instanceof JwtAuthenticationToken) {
            return ((JwtAuthenticationToken) authentication).getToken();
        }
        return null;
    }

    /**
     * 从JWT中获取Claim值
     */
    public static Object getClaim(String claimName) {
        Jwt jwt = getCurrentJwt();
        return jwt != null ? jwt.getClaim(claimName) : null;
    }

    /**
     * 从JWT中获取字符串类型的Claim值
     */
    public static String getClaimAsString(String claimName) {
        Jwt jwt = getCurrentJwt();
        return jwt != null ? jwt.getClaimAsString(claimName) : null;
    }
}
