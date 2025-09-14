package com.cloud.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 安全上下文工具类
 * 提供统一的JWT Token解析、用户信息获取、权限检查等功能
 *
 * @author what's up
 */
@Slf4j
public class SecurityContextUtils {

    /**
     * 获取当前认证的用户JWT Token
     *
     * @return Jwt Token对象，如果未认证则返回null
     */
    public static Jwt getCurrentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt;
        }

        return null;
    }

    /**
     * 获取当前用户ID
     *
     * @return 用户ID字符串，如果未找到则返回null
     */
    public static String getCurrentUserId() {
        Jwt jwt = getCurrentJwt();
        if (jwt == null) {
            return null;
        }

        // 尝试从不同的claim中获取用户ID
        String userId = jwt.getClaimAsString("user_id");
        if (userId == null) {
            userId = jwt.getClaimAsString("sub");
        }
        if (userId == null) {
            userId = jwt.getClaimAsString("uid");
        }

        return userId;
    }

    /**
     * 获取当前用户名
     *
     * @return 用户名字符串，如果未找到则返回null
     */
    public static String getCurrentUsername() {
        Jwt jwt = getCurrentJwt();
        if (jwt == null) {
            return null;
        }

        String username = jwt.getClaimAsString("username");
        if (username == null) {
            username = jwt.getClaimAsString("preferred_username");
        }
        if (username == null) {
            username = jwt.getClaimAsString("name");
        }

        return username;
    }

    /**
     * 获取当前用户类型
     *
     * @return 用户类型字符串 (USER, MERCHANT, ADMIN)，如果未找到则返回null
     */
    public static String getCurrentUserType() {
        Jwt jwt = getCurrentJwt();
        if (jwt == null) {
            return null;
        }

        return jwt.getClaimAsString("user_type");
    }

    /**
     * 获取当前用户的权限列表
     *
     * @return 权限字符串列表，如果未找到则返回空列表
     */
    public static List<String> getCurrentPermissions() {
        Jwt jwt = getCurrentJwt();
        if (jwt == null) {
            return List.of();
        }

        List<String> permissions = jwt.getClaimAsStringList("permissions");
        return permissions != null ? permissions : List.of();
    }

    /**
     * 获取当前用户的角色列表
     *
     * @return 角色字符串列表，如果未找到则返回空列表
     */
    public static List<String> getCurrentRoles() {
        Jwt jwt = getCurrentJwt();
        if (jwt == null) {
            return List.of();
        }

        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null ? roles : List.of();
    }

    /**
     * 获取当前用户的作用域(scope)列表
     *
     * @return 作用域字符串列表，如果未找到则返回空列表
     */
    public static List<String> getCurrentScopes() {
        Jwt jwt = getCurrentJwt();
        if (jwt == null) {
            return List.of();
        }

        String scopeString = jwt.getClaimAsString("scope");
        if (scopeString != null && !scopeString.isEmpty()) {
            return List.of(scopeString.split(" "));
        }

        List<String> scopes = jwt.getClaimAsStringList("scopes");
        return scopes != null ? scopes : List.of();
    }

    /**
     * 获取当前用户的所有权限（包括角色转换的权限和直接权限）
     *
     * @return 所有权限字符串集合
     */
    public static Set<String> getAllAuthorities() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Set.of();
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    /**
     * 检查当前用户是否具有指定权限
     *
     * @param permission 权限标识
     * @return 是否具有该权限
     */
    public static boolean hasPermission(String permission) {
        if (StringUtils.isEmpty(permission)) {
            return false;
        }

        return getCurrentPermissions().contains(permission);
    }

    /**
     * 检查当前用户是否具有任意一个指定权限
     *
     * @param permissions 权限标识数组
     * @return 是否具有任意一个权限
     */
    public static boolean hasAnyPermission(String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return false;
        }

        List<String> userPermissions = getCurrentPermissions();
        for (String permission : permissions) {
            if (userPermissions.contains(permission)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 检查当前用户是否具有指定角色
     *
     * @param role 角色标识
     * @return 是否具有该角色
     */
    public static boolean hasRole(String role) {
        if (StringUtils.isEmpty(role)) {
            return false;
        }

        // 检查JWT中的roles claim
        if (getCurrentRoles().contains(role)) {
            return true;
        }

        // 检查Spring Security的GrantedAuthority
        Set<String> authorities = getAllAuthorities();
        return authorities.contains(role) || authorities.contains("ROLE_" + role);
    }

    /**
     * 检查当前用户是否具有任意一个指定角色
     *
     * @param roles 角色标识数组
     * @return 是否具有任意一个角色
     */
    public static boolean hasAnyRole(String... roles) {
        if (roles == null || roles.length == 0) {
            return false;
        }

        for (String role : roles) {
            if (hasRole(role)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 检查当前用户是否具有指定的用户类型
     *
     * @param userType 用户类型 (USER, MERCHANT, ADMIN)
     * @return 是否匹配该用户类型
     */
    public static boolean hasUserType(String userType) {
        if (StringUtils.isEmpty(userType)) {
            return false;
        }

        String currentUserType = getCurrentUserType();
        return userType.equalsIgnoreCase(currentUserType);
    }

    /**
     * 检查当前用户是否具有任意一个指定用户类型
     *
     * @param userTypes 用户类型数组
     * @return 是否匹配任意一个用户类型
     */
    public static boolean hasAnyUserType(String... userTypes) {
        if (userTypes == null || userTypes.length == 0) {
            return false;
        }

        String currentUserType = getCurrentUserType();
        if (currentUserType == null) {
            return false;
        }

        for (String userType : userTypes) {
            if (currentUserType.equalsIgnoreCase(userType)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 检查当前用户是否具有指定作用域
     *
     * @param scope 作用域标识
     * @return 是否具有该作用域
     */
    public static boolean hasScope(String scope) {
        if (StringUtils.isEmpty(scope)) {
            return false;
        }

        return getCurrentScopes().contains(scope) ||
                getAllAuthorities().contains("SCOPE_" + scope);
    }

    /**
     * 检查当前用户是否具有任意一个指定作用域
     *
     * @param scopes 作用域标识数组
     * @return 是否具有任意一个作用域
     */
    public static boolean hasAnyScope(String... scopes) {
        if (scopes == null || scopes.length == 0) {
            return false;
        }

        for (String scope : scopes) {
            if (hasScope(scope)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 检查当前用户是否为同一用户（用于资源权限检查）
     *
     * @param userId 要检查的用户ID
     * @return 是否为同一用户
     */
    public static boolean isSameUser(String userId) {
        if (StringUtils.isEmpty(userId)) {
            return false;
        }

        String currentUserId = getCurrentUserId();
        return userId.equals(currentUserId);
    }

    /**
     * 获取JWT中的自定义claim
     *
     * @param claimName claim名称
     * @param clazz     返回值类型
     * @param <T>       泛型类型
     * @return claim值，如果未找到则返回null
     */
    public static <T> T getClaim(String claimName, Class<T> clazz) {
        Jwt jwt = getCurrentJwt();
        if (jwt == null || StringUtils.isEmpty(claimName)) {
            return null;
        }

        return jwt.getClaimAsString(claimName) != null ? jwt.getClaim(claimName) : null;
    }

    /**
     * 获取当前用户的客户端ID（OAuth2 Client）
     *
     * @return 客户端ID，如果未找到则返回null
     */
    public static String getCurrentClientId() {
        Jwt jwt = getCurrentJwt();
        if (jwt == null) {
            return null;
        }

        String clientId = jwt.getClaimAsString("client_id");
        if (clientId == null) {
            clientId = jwt.getClaimAsString("aud"); // audience
        }

        return clientId;
    }

    /**
     * 检查当前是否为已认证状态
     *
     * @return 是否已认证
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() &&
                !"anonymousUser".equals(authentication.getPrincipal());
    }
}
