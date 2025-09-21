package com.cloud.common.utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 用户上下文工具类
 * 用于从JWT token和HTTP请求头中提取用户信息
 * 支持Gateway转发的用户信息获取
 *
 * @author what's up
 */
@Slf4j
public class UserContextUtils {

    // HTTP头名称常量 - 对应Gateway的JwtTokenForwardFilter
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_NAME = "X-User-Name";
    public static final String HEADER_USER_TYPE = "X-User-Type";
    public static final String HEADER_USER_NICKNAME = "X-User-Nickname";
    public static final String HEADER_USER_STATUS = "X-User-Status";
    public static final String HEADER_CLIENT_ID = "X-Client-Id";
    public static final String HEADER_TOKEN_VERSION = "X-Token-Version";
    public static final String HEADER_USER_SCOPES = "X-User-Scopes";

    /**
     * 获取当前用户ID
     * 优先从JWT token中获取，其次从HTTP头中获取
     *
     * @return 用户ID，如果不存在返回null
     */
    public static String getCurrentUserId() {
        // 优先从JWT token中获取
        String userIdFromJwt = getClaimFromJwt("user_id");
        if (StringUtils.hasText(userIdFromJwt)) {
            return userIdFromJwt;
        }

        // 从HTTP头中获取（Gateway转发的信息）
        return getHeaderValue(HEADER_USER_ID);
    }

    /**
     * 获取当前用户名
     *
     * @return 用户名，如果不存在返回null
     */
    public static String getCurrentUsername() {
        // 优先从JWT token中获取
        String usernameFromJwt = getClaimFromJwt("username");
        if (StringUtils.hasText(usernameFromJwt)) {
            return usernameFromJwt;
        }

        // 从认证对象中获取
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && StringUtils.hasText(authentication.getName())) {
            return authentication.getName();
        }

        // 从HTTP头中获取
        return getHeaderValue(HEADER_USER_NAME);
    }

    /**
     * 获取当前用户类型
     *
     * @return 用户类型，如果不存在返回null
     */
    public static String getCurrentUserType() {
        // 优先从JWT token中获取
        String userTypeFromJwt = getClaimFromJwt("user_type");
        if (StringUtils.hasText(userTypeFromJwt)) {
            return userTypeFromJwt;
        }

        // 从HTTP头中获取
        return getHeaderValue(HEADER_USER_TYPE);
    }

    /**
     * 获取当前用户昵称
     *
     * @return 用户昵称，如果不存在返回null
     */
    public static String getCurrentUserNickname() {
        // 优先从JWT token中获取
        String nicknameFromJwt = getClaimFromJwt("nickname");
        if (StringUtils.hasText(nicknameFromJwt)) {
            return nicknameFromJwt;
        }

        // 从HTTP头中获取
        return getHeaderValue(HEADER_USER_NICKNAME);
    }

    /**
     * 获取当前用户状态
     *
     * @return 用户状态，如果不存在返回null
     */
    public static String getCurrentUserStatus() {
        // 优先从JWT token中获取
        String statusFromJwt = getClaimFromJwt("status");
        if (StringUtils.hasText(statusFromJwt)) {
            return statusFromJwt;
        }

        // 从HTTP头中获取
        return getHeaderValue(HEADER_USER_STATUS);
    }

    /**
     * 获取当前用户手机号
     * 注意：手机号作为敏感信息，仅从JWT token中获取，不会通过HTTP头传递
     *
     * @return 用户手机号，如果不存在返回null
     */
    public static String getCurrentUserPhone() {
        // 仅从JWT token中获取敏感信息，不会通过HTTP头传递
        return getClaimFromJwt("phone");
    }

    /**
     * 获取客户端ID
     *
     * @return 客户端ID，如果不存在返回null
     */
    public static String getClientId() {
        // 优先从JWT token中获取
        String clientIdFromJwt = getClaimFromJwt("client_id");
        if (StringUtils.hasText(clientIdFromJwt)) {
            return clientIdFromJwt;
        }

        // 从HTTP头中获取
        return getHeaderValue(HEADER_CLIENT_ID);
    }

    /**
     * 获取用户权限范围
     *
     * @return 权限范围集合，如果不存在返回空集合
     */
    public static Set<String> getCurrentUserScopes() {
        // 优先从JWT token中获取
        String scopesFromJwt = getClaimFromJwt("scope");
        if (StringUtils.hasText(scopesFromJwt)) {
            return Stream.of(scopesFromJwt.split("\\s+"))
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toSet());
        }

        // 从HTTP头中获取
        String scopesFromHeader = getHeaderValue(HEADER_USER_SCOPES);
        if (StringUtils.hasText(scopesFromHeader)) {
            return Stream.of(scopesFromHeader.split("\\s+"))
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toSet());
        }

        return Collections.emptySet();
    }

    /**
     * 检查当前用户是否拥有指定权限
     *
     * @param scope 权限范围
     * @return 如果拥有权限返回true，否则返回false
     */
    public static boolean hasScope(String scope) {
        Set<String> userScopes = getCurrentUserScopes();
        return userScopes.contains(scope);
    }

    /**
     * 检查当前用户是否拥有任意一个指定权限
     *
     * @param scopes 权限范围列表
     * @return 如果拥有任意一个权限返回true，否则返回false
     */
    public static boolean hasAnyScope(String... scopes) {
        Set<String> userScopes = getCurrentUserScopes();
        for (String scope : scopes) {
            if (userScopes.contains(scope)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取当前JWT token
     *
     * @return JWT token字符串，如果不存在返回null
     */
    public static String getCurrentToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getTokenValue();
        }
        return null;
    }

    /**
     * 获取完整的JWT对象
     *
     * @return JWT对象，如果不存在返回null
     */
    public static Jwt getCurrentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken();
        }
        return null;
    }

    /**
     * 从JWT token中获取特定的claim
     *
     * @param claimName claim名称
     * @return claim值，如果不存在返回null
     */
    public static String getClaimFromJwt(String claimName) {
        Jwt jwt = getCurrentJwt();
        if (jwt != null) {
            Object claim = jwt.getClaim(claimName);
            return claim != null ? claim.toString() : null;
        }
        return null;
    }

    /**
     * 从HTTP请求头中获取值
     *
     * @param headerName 请求头名称
     * @return 请求头值，如果不存在返回null
     */
    public static String getHeaderValue(String headerName) {
        try {
            ServletRequestAttributes requestAttributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {
                HttpServletRequest request = requestAttributes.getRequest();
                String value = request.getHeader(headerName);
                return StringUtils.hasText(value) && !"null".equals(value) ? value : null;
            }
        } catch (Exception e) {
            log.debug("获取HTTP请求头 {} 时发生异常: {}", headerName, e.getMessage());
        }
        return null;
    }

    /**
     * 获取当前用户的所有信息（用于调试和日志）
     *
     * @return 用户信息字符串
     */
    public static String getCurrentUserInfo() {
        return String.format("User[id=%s, username=%s, type=%s, nickname=%s, status=%s, scopes=%s]",
                getCurrentUserId(),
                getCurrentUsername(),
                getCurrentUserType(),
                getCurrentUserNickname(),
                getCurrentUserStatus(),
                getCurrentUserScopes());
    }

    /**
     * 判断当前用户是否为指定类型
     *
     * @param userType 用户类型
     * @return 如果是指定类型返回true，否则返回false
     */
    public static boolean isUserType(String userType) {
        String currentUserType = getCurrentUserType();
        return userType.equals(currentUserType);
    }

    /**
     * 判断当前用户是否为普通用户
     *
     * @return 如果是普通用户返回true，否则返回false
     */
    public static boolean isRegularUser() {
        return isUserType("USER");
    }

    /**
     * 判断当前用户是否为商户
     *
     * @return 如果是商户返回true，否则返回false
     */
    public static boolean isMerchant() {
        return isUserType("MERCHANT");
    }

    /**
     * 判断当前用户是否为管理员
     *
     * @return 如果是管理员返回true，否则返回false
     */
    public static boolean isAdmin() {
        return isUserType("ADMIN");
    }

    /**
     * 检查当前请求是否包含有效的JWT认证信息
     *
     * @return 如果有效返回true，否则返回false
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null &&
                authentication.isAuthenticated() &&
                authentication instanceof JwtAuthenticationToken;
    }

    /**
     * 获取Token版本
     *
     * @return Token版本，如果不存在返回null
     */
    public static String getTokenVersion() {
        // 优先从JWT token中获取
        String versionFromJwt = getClaimFromJwt("token_version");
        if (StringUtils.hasText(versionFromJwt)) {
            return versionFromJwt;
        }

        // 从HTTP头中获取
        return getHeaderValue(HEADER_TOKEN_VERSION);
    }
}
