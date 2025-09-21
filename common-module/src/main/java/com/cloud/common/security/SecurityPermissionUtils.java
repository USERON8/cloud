package com.cloud.common.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 统一的安全权限工具类
 * 整合认证、授权、权限检查等所有安全相关功能
 * 提供静态方法和基于Authentication参数的方法两种使用方式
 *
 * @author what's up
 * @since 2025-09-21
 */
@Slf4j
public final class SecurityPermissionUtils {

    private SecurityPermissionUtils() {
        // 工具类，防止实例化
    }

    // ================================ 认证信息获取 ================================

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
        return getCurrentUserId(getCurrentAuthentication());
    }

    /**
     * 获取指定认证对象的用户ID
     */
    public static String getCurrentUserId(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            return jwt.getClaimAsString("user_id");
        }
        return null;
    }

    /**
     * 获取当前用户名
     */
    public static String getCurrentUsername() {
        return getCurrentUsername(getCurrentAuthentication());
    }

    /**
     * 获取指定认证对象的用户名
     */
    public static String getCurrentUsername(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            return jwt.getClaimAsString("user_name");
        }
        return authentication != null ? authentication.getName() : null;
    }

    /**
     * 获取当前用户类型
     */
    public static String getCurrentUserType() {
        return getCurrentUserType(getCurrentAuthentication());
    }

    /**
     * 获取指定认证对象的用户类型
     */
    public static String getCurrentUserType(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            return jwt.getClaimAsString("user_type");
        }
        return null;
    }

    /**
     * 获取当前用户的所有权限
     */
    public static Set<String> getCurrentUserAuthorities() {
        return getCurrentUserAuthorities(getCurrentAuthentication());
    }

    /**
     * 获取指定认证对象的所有权限
     */
    public static Set<String> getCurrentUserAuthorities(Authentication authentication) {
        if (authentication == null) {
            return Collections.emptySet();
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    // ================================ 认证状态检查 ================================

    /**
     * 检查是否已认证
     */
    public static boolean isAuthenticated() {
        return isAuthenticated(getCurrentAuthentication());
    }

    /**
     * 检查指定认证对象是否已认证
     */
    public static boolean isAuthenticated(Authentication authentication) {
        return authentication != null 
               && authentication.isAuthenticated()
               && !"anonymousUser".equals(authentication.getName());
    }

    // ================================ 权限检查 ================================

    /**
     * 检查当前用户是否具有指定权限
     */
    public static boolean hasAuthority(String authority) {
        return hasAuthority(getCurrentAuthentication(), authority);
    }

    /**
     * 检查指定认证对象是否具有指定权限
     */
    public static boolean hasAuthority(Authentication authentication, String authority) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> auth.equals(authority));
    }

    /**
     * 检查当前用户是否具有任意一个指定权限
     */
    public static boolean hasAnyAuthority(String... authorities) {
        return hasAnyAuthority(getCurrentAuthentication(), authorities);
    }

    /**
     * 检查指定认证对象是否具有任意一个指定权限
     */
    public static boolean hasAnyAuthority(Authentication authentication, String... authorities) {
        if (authentication == null || authorities == null) {
            return false;
        }
        
        for (String authority : authorities) {
            if (hasAuthority(authentication, authority)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查当前用户是否具有指定角色
     */
    public static boolean hasRole(String role) {
        return hasRole(getCurrentAuthentication(), role);
    }

    /**
     * 检查指定认证对象是否具有指定角色
     */
    public static boolean hasRole(Authentication authentication, String role) {
        String roleAuthority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return hasAuthority(authentication, roleAuthority);
    }

    /**
     * 检查当前用户是否具有任意一个指定角色
     */
    public static boolean hasAnyRole(String... roles) {
        return hasAnyRole(getCurrentAuthentication(), roles);
    }

    /**
     * 检查指定认证对象是否具有任意一个指定角色
     */
    public static boolean hasAnyRole(Authentication authentication, String... roles) {
        String[] roleAuthorities = new String[roles.length];
        for (int i = 0; i < roles.length; i++) {
            roleAuthorities[i] = roles[i].startsWith("ROLE_") ? roles[i] : "ROLE_" + roles[i];
        }
        return hasAnyAuthority(authentication, roleAuthorities);
    }

    // ================================ 用户类型检查 ================================

    /**
     * 检查当前用户是否为指定用户类型
     */
    public static boolean hasUserType(String userType) {
        return hasUserType(getCurrentAuthentication(), userType);
    }

    /**
     * 检查指定认证对象是否为指定用户类型
     */
    public static boolean hasUserType(Authentication authentication, String userType) {
        String currentUserType = getCurrentUserType(authentication);
        return currentUserType != null && currentUserType.equalsIgnoreCase(userType);
    }

    /**
     * 检查当前用户是否为管理员
     */
    public static boolean isAdmin() {
        return isAdmin(getCurrentAuthentication());
    }

    /**
     * 检查指定认证对象是否为管理员
     */
    public static boolean isAdmin(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_ADMIN") 
                          || authority.contains("admin") 
                          || hasRole(authentication, "ADMIN"));
    }

    /**
     * 检查当前用户是否为商户
     */
    public static boolean isMerchant() {
        return isMerchant(getCurrentAuthentication());
    }

    /**
     * 检查指定认证对象是否为商户
     */
    public static boolean isMerchant(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_MERCHANT") 
                          || authority.contains("merchant") 
                          || hasRole(authentication, "MERCHANT"));
    }

    /**
     * 检查当前用户是否为普通用户
     */
    public static boolean isUser() {
        return isUser(getCurrentAuthentication());
    }

    /**
     * 检查指定认证对象是否为普通用户
     */
    public static boolean isUser(Authentication authentication) {
        return hasRole(authentication, "USER");
    }

    // ================================ 身份验证检查 ================================

    /**
     * 检查是否为指定用户
     */
    public static boolean isSameUser(String userId) {
        return isSameUser(getCurrentAuthentication(), userId);
    }

    /**
     * 检查指定认证对象是否为指定用户
     */
    public static boolean isSameUser(Authentication authentication, String userId) {
        String currentUserId = getCurrentUserId(authentication);
        return currentUserId != null && currentUserId.equals(userId);
    }

    /**
     * 检查是否为资源所有者
     */
    public static boolean isOwner(Long resourceUserId) {
        return isOwner(getCurrentAuthentication(), resourceUserId);
    }

    /**
     * 检查指定认证对象是否为资源所有者
     */
    public static boolean isOwner(Authentication authentication, Long resourceUserId) {
        if (authentication == null || resourceUserId == null) {
            return false;
        }
        
        String currentUserId = getCurrentUserId(authentication);
        return Objects.equals(currentUserId, resourceUserId.toString());
    }

    /**
     * 检查当前用户是否为商户所有者
     */
    public static boolean isMerchantOwner(Long merchantId) {
        return isMerchantOwner(getCurrentAuthentication(), merchantId);
    }

    /**
     * 检查指定认证对象是否为商户所有者
     */
    public static boolean isMerchantOwner(Authentication authentication, Long merchantId) {
        if (authentication == null || merchantId == null) {
            return false;
        }
        
        // 检查是否为商户
        if (!isMerchant(authentication)) {
            return false;
        }
        
        // 获取当前用户ID，检查是否与商户ID一致
        String currentUserId = getCurrentUserId(authentication);
        return Objects.equals(currentUserId, merchantId.toString());
    }

    // ================================ JWT相关操作 ================================

    /**
     * 获取当前JWT Token
     */
    public static Jwt getCurrentJwt() {
        return getCurrentJwt(getCurrentAuthentication());
    }

    /**
     * 获取指定认证对象的JWT Token
     */
    public static Jwt getCurrentJwt(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken();
        }
        return null;
    }

    /**
     * 从当前JWT中获取Claim值
     */
    public static Object getClaim(String claimName) {
        return getClaim(getCurrentAuthentication(), claimName);
    }

    /**
     * 从指定认证对象的JWT中获取Claim值
     */
    public static Object getClaim(Authentication authentication, String claimName) {
        Jwt jwt = getCurrentJwt(authentication);
        return jwt != null ? jwt.getClaim(claimName) : null;
    }

    /**
     * 从当前JWT中获取字符串类型的Claim值
     */
    public static String getClaimAsString(String claimName) {
        return getClaimAsString(getCurrentAuthentication(), claimName);
    }

    /**
     * 从指定认证对象的JWT中获取字符串类型的Claim值
     */
    public static String getClaimAsString(Authentication authentication, String claimName) {
        Jwt jwt = getCurrentJwt(authentication);
        return jwt != null ? jwt.getClaimAsString(claimName) : null;
    }

    // ================================ 组合权限检查 ================================

    /**
     * 检查是否为管理员或资源所有者
     */
    public static boolean isAdminOrOwner(Long resourceUserId) {
        return isAdminOrOwner(getCurrentAuthentication(), resourceUserId);
    }

    /**
     * 检查指定认证对象是否为管理员或资源所有者
     */
    public static boolean isAdminOrOwner(Authentication authentication, Long resourceUserId) {
        return isAdmin(authentication) || isOwner(authentication, resourceUserId);
    }

    /**
     * 检查是否为管理员或商户所有者
     */
    public static boolean isAdminOrMerchantOwner(Long merchantId) {
        return isAdminOrMerchantOwner(getCurrentAuthentication(), merchantId);
    }

    /**
     * 检查指定认证对象是否为管理员或商户所有者
     */
    public static boolean isAdminOrMerchantOwner(Authentication authentication, Long merchantId) {
        return isAdmin(authentication) || isMerchantOwner(authentication, merchantId);
    }

    // ================================ 业务相关便捷方法 ================================

    /**
     * 检查用户是否有权限访问指定资源
     */
    public static boolean canAccessResource(Long resourceUserId) {
        return canAccessResource(getCurrentAuthentication(), resourceUserId);
    }

    /**
     * 检查指定认证对象是否有权限访问指定资源
     */
    public static boolean canAccessResource(Authentication authentication, Long resourceUserId) {
        // 管理员可以访问所有资源
        if (isAdmin(authentication)) {
            return true;
        }
        
        // 检查是否为资源所有者
        String currentUserId = getCurrentUserId(authentication);
        return currentUserId != null && currentUserId.equals(resourceUserId.toString());
    }

    /**
     * 获取安全的用户信息摘要（用于日志等场景）
     */
    public static String getUserSummary() {
        return getUserSummary(getCurrentAuthentication());
    }

    /**
     * 获取指定认证对象的安全的用户信息摘要
     */
    public static String getUserSummary(Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            return "Anonymous";
        }
        
        String userId = getCurrentUserId(authentication);
        String userType = getCurrentUserType(authentication);
        return String.format("User[id=%s, type=%s]", 
            userId != null ? userId : "unknown", 
            userType != null ? userType : "unknown");
    }
}
