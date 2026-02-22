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








@Slf4j
public class UserContextUtils {

    
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_NAME = "X-User-Name";
    public static final String HEADER_USER_TYPE = "X-User-Type";
    public static final String HEADER_USER_NICKNAME = "X-User-Nickname";
    public static final String HEADER_USER_STATUS = "X-User-Status";
    public static final String HEADER_CLIENT_ID = "X-Client-Id";
    public static final String HEADER_TOKEN_VERSION = "X-Token-Version";
    public static final String HEADER_USER_SCOPES = "X-User-Scopes";

    





    public static String getCurrentUserId() {
        
        String userIdFromJwt = getClaimFromJwt("user_id");
        if (StringUtils.hasText(userIdFromJwt)) {
            return userIdFromJwt;
        }

        
        return getHeaderValue(HEADER_USER_ID);
    }

    




    public static String getCurrentUsername() {
        
        String usernameFromJwt = getClaimFromJwt("username");
        if (StringUtils.hasText(usernameFromJwt)) {
            return usernameFromJwt;
        }

        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && StringUtils.hasText(authentication.getName())) {
            return authentication.getName();
        }

        
        return getHeaderValue(HEADER_USER_NAME);
    }

    




    public static String getCurrentUserType() {
        
        String userTypeFromJwt = getClaimFromJwt("user_type");
        if (StringUtils.hasText(userTypeFromJwt)) {
            return userTypeFromJwt;
        }

        
        return getHeaderValue(HEADER_USER_TYPE);
    }

    




    public static String getCurrentUserNickname() {
        
        String nicknameFromJwt = getClaimFromJwt("nickname");
        if (StringUtils.hasText(nicknameFromJwt)) {
            return nicknameFromJwt;
        }

        
        return getHeaderValue(HEADER_USER_NICKNAME);
    }

    




    public static String getCurrentUserStatus() {
        
        String statusFromJwt = getClaimFromJwt("status");
        if (StringUtils.hasText(statusFromJwt)) {
            return statusFromJwt;
        }

        
        return getHeaderValue(HEADER_USER_STATUS);
    }

    





    public static String getCurrentUserPhone() {
        
        return getClaimFromJwt("phone");
    }

    




    public static String getClientId() {
        
        String clientIdFromJwt = getClaimFromJwt("client_id");
        if (StringUtils.hasText(clientIdFromJwt)) {
            return clientIdFromJwt;
        }

        
        return getHeaderValue(HEADER_CLIENT_ID);
    }

    




    public static Set<String> getCurrentUserScopes() {
        
        String scopesFromJwt = getClaimFromJwt("scope");
        if (StringUtils.hasText(scopesFromJwt)) {
            return Stream.of(scopesFromJwt.split("\\s+"))
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toSet());
        }

        
        String scopesFromHeader = getHeaderValue(HEADER_USER_SCOPES);
        if (StringUtils.hasText(scopesFromHeader)) {
            return Stream.of(scopesFromHeader.split("\\s+"))
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toSet());
        }

        return Collections.emptySet();
    }

    





    public static boolean hasScope(String scope) {
        Set<String> userScopes = getCurrentUserScopes();
        return userScopes.contains(scope);
    }

    





    public static boolean hasAnyScope(String... scopes) {
        Set<String> userScopes = getCurrentUserScopes();
        for (String scope : scopes) {
            if (userScopes.contains(scope)) {
                return true;
            }
        }
        return false;
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
                return StringUtils.hasText(value) && !"null".equals(value) ? value : null;
            }
        } catch (Exception e) {
            log.debug("鑾峰彇HTTP璇锋眰澶?{} 鏃跺彂鐢熷紓甯? {}", headerName, e.getMessage());
        }
        return null;
    }

    




    public static String getCurrentUserInfo() {
        return String.format("User[id=%s, username=%s, type=%s, nickname=%s, status=%s, scopes=%s]",
                getCurrentUserId(),
                getCurrentUsername(),
                getCurrentUserType(),
                getCurrentUserNickname(),
                getCurrentUserStatus(),
                getCurrentUserScopes());
    }

    





    public static boolean isUserType(String userType) {
        String currentUserType = getCurrentUserType();
        return userType.equals(currentUserType);
    }

    




    public static boolean isRegularUser() {
        return isUserType("USER");
    }

    




    public static boolean isMerchant() {
        return isUserType("MERCHANT");
    }

    




    public static boolean isAdmin() {
        return isUserType("ADMIN");
    }

    




    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null &&
                authentication.isAuthenticated() &&
                authentication instanceof JwtAuthenticationToken;
    }

    




    public static String getTokenVersion() {
        
        String versionFromJwt = getClaimFromJwt("token_version");
        if (StringUtils.hasText(versionFromJwt)) {
            return versionFromJwt;
        }

        
        return getHeaderValue(HEADER_TOKEN_VERSION);
    }
}
