package com.cloud.common.utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import cn.hutool.core.util.StrUtil;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;








@Slf4j
public class UserContextUtils {

    
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_NAME = "X-User-Name";
    public static final String HEADER_USER_NICKNAME = "X-User-Nickname";
    public static final String HEADER_USER_STATUS = "X-User-Status";
    public static final String HEADER_CLIENT_ID = "X-Client-Id";
    public static final String HEADER_TOKEN_VERSION = "X-Token-Version";
    public static final String HEADER_USER_SCOPES = "X-User-Scopes";

    





    public static String getCurrentUserId() {
        return getClaimFromJwt("user_id");
    }

    




    public static String getCurrentUsername() {
        
        String usernameFromJwt = getClaimFromJwt("username");
        if (StrUtil.isNotBlank(usernameFromJwt)) {
            return usernameFromJwt;
        }

        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && StrUtil.isNotBlank(authentication.getName())) {
            return authentication.getName();
        }

        return null;
    }

    




    public static String getCurrentUserType() {
        if (hasRole("ADMIN")) {
            return "ADMIN";
        }
        if (hasRole("MERCHANT")) {
            return "MERCHANT";
        }
        if (hasRole("USER")) {
            return "USER";
        }
        return null;
    }

    




    public static String getCurrentUserNickname() {
        return getClaimFromJwt("nickname");
    }

    




    public static String getCurrentUserStatus() {
        return getClaimFromJwt("status");
    }

    





    public static String getCurrentUserPhone() {
        
        return getClaimFromJwt("phone");
    }

    




    public static String getClientId() {
        return getClaimFromJwt("client_id");
    }

    




    public static Set<String> getCurrentUserScopes() {
        String scopesFromJwt = getClaimFromJwt("scope");
        if (StrUtil.isNotBlank(scopesFromJwt)) {
            return Stream.of(scopesFromJwt.split("\\s+"))
                    .filter(StrUtil::isNotBlank)
                    .map(UserContextUtils::normalizeScope)
                    .collect(Collectors.toSet());
        }

        return Collections.emptySet();
    }

    





    public static boolean hasScope(String scope) {
        if (StrUtil.isBlank(scope)) {
            return false;
        }
        Set<String> userScopes = getCurrentUserScopes();
        return userScopes.contains(normalizeScope(scope));
    }

    





    public static boolean hasAnyScope(String... scopes) {
        Set<String> userScopes = getCurrentUserScopes();
        for (String scope : scopes) {
            if (StrUtil.isNotBlank(scope) && userScopes.contains(normalizeScope(scope))) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeScope(String scope) {
        return scope.replace('.', ':');
    }

    




    public static String getCurrentToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getTokenValue();
        }
        return null;
    }

    




    public static Jwt getCurrentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken();
        }
        return null;
    }

    





    public static String getClaimFromJwt(String claimName) {
        Jwt jwt = getCurrentJwt();
        if (jwt != null) {
            Object claim = jwt.getClaim(claimName);
            return claim != null ? claim.toString() : null;
        }
        return null;
    }

    





    public static String getHeaderValue(String headerName) {
        try {
            ServletRequestAttributes requestAttributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {
                HttpServletRequest request = requestAttributes.getRequest();
                String value = request.getHeader(headerName);
                return StrUtil.isNotBlank(value) && !"null".equals(value) ? value : null;
            }
        } catch (Exception e) {
            log.debug("HTTP?{} ? {}", headerName, e.getMessage());
        }
        return null;
    }

    




    public static String getCurrentUserInfo() {
        return String.format("User[id=%s, username=%s, role=%s, nickname=%s, status=%s, scopes=%s]",
                getCurrentUserId(),
                getCurrentUsername(),
                getCurrentUserType(),
                getCurrentUserNickname(),
                getCurrentUserStatus(),
                getCurrentUserScopes());
    }

    





    public static boolean isUserType(String userType) {
        return hasRole(userType);
    }

    




    public static boolean isRegularUser() {
        return isUserType("USER");
    }

    




    public static boolean isMerchant() {
        return hasRole("MERCHANT");
    }

    




    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }

    




    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null &&
                authentication.isAuthenticated() &&
                authentication instanceof JwtAuthenticationToken;
    }

    




    public static String getTokenVersion() {
        return getClaimFromJwt("token_version");
    }

    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || role == null || role.isBlank()) {
            return false;
        }
        String expected = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return authentication.getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                .anyMatch(expected::equals);
    }
}



