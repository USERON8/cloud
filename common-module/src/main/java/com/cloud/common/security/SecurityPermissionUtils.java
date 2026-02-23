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









@Slf4j
public final class SecurityPermissionUtils {

    private SecurityPermissionUtils() {
        
    }

    

    


    public static Authentication getCurrentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    


    public static String getCurrentUserId() {
        return getCurrentUserId(getCurrentAuthentication());
    }

    


    public static String getCurrentUserId(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            return jwt.getClaimAsString("user_id");
        }
        return null;
    }

    


    public static String getCurrentUsername() {
        return getCurrentUsername(getCurrentAuthentication());
    }

    


    public static String getCurrentUsername(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String username = jwt.getClaimAsString("username");
            if (username != null && !username.isBlank()) {
                return username;
            }
            return jwt.getClaimAsString("user_name");
        }
        return authentication != null ? authentication.getName() : null;
    }

    


    public static String getCurrentUserType() {
        return getCurrentUserType(getCurrentAuthentication());
    }

    


    public static String getCurrentUserType(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            return jwt.getClaimAsString("user_type");
        }
        return null;
    }

    


    public static Set<String> getCurrentUserAuthorities() {
        return getCurrentUserAuthorities(getCurrentAuthentication());
    }

    


    public static Set<String> getCurrentUserAuthorities(Authentication authentication) {
        if (authentication == null) {
            return Collections.emptySet();
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    

    


    public static boolean isAuthenticated() {
        return isAuthenticated(getCurrentAuthentication());
    }

    


    public static boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName());
    }

    

    


    public static boolean hasAuthority(String authority) {
        return hasAuthority(getCurrentAuthentication(), authority);
    }

    


    public static boolean hasAuthority(Authentication authentication, String authority) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> auth.equals(authority));
    }

    


    public static boolean hasAnyAuthority(String... authorities) {
        return hasAnyAuthority(getCurrentAuthentication(), authorities);
    }

    


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

    


    public static boolean hasRole(String role) {
        return hasRole(getCurrentAuthentication(), role);
    }

    


    public static boolean hasRole(Authentication authentication, String role) {
        String roleAuthority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return hasAuthority(authentication, roleAuthority);
    }

    


    public static boolean hasAnyRole(String... roles) {
        return hasAnyRole(getCurrentAuthentication(), roles);
    }

    


    public static boolean hasAnyRole(Authentication authentication, String... roles) {
        String[] roleAuthorities = new String[roles.length];
        for (int i = 0; i < roles.length; i++) {
            roleAuthorities[i] = roles[i].startsWith("ROLE_") ? roles[i] : "ROLE_" + roles[i];
        }
        return hasAnyAuthority(authentication, roleAuthorities);
    }

    

    


    public static boolean hasUserType(String userType) {
        return hasUserType(getCurrentAuthentication(), userType);
    }

    


    public static boolean hasUserType(Authentication authentication, String userType) {
        String currentUserType = getCurrentUserType(authentication);
        return currentUserType != null && currentUserType.equalsIgnoreCase(userType);
    }

    


    public static boolean isAdmin() {
        return isAdmin(getCurrentAuthentication());
    }

    


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

    


    public static boolean isMerchant() {
        return isMerchant(getCurrentAuthentication());
    }

    


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

    


    public static boolean isUser() {
        return isUser(getCurrentAuthentication());
    }

    


    public static boolean isUser(Authentication authentication) {
        return hasRole(authentication, "USER");
    }

    

    


    public static boolean isSameUser(String userId) {
        return isSameUser(getCurrentAuthentication(), userId);
    }

    


    public static boolean isSameUser(Authentication authentication, String userId) {
        String currentUserId = getCurrentUserId(authentication);
        return currentUserId != null && currentUserId.equals(userId);
    }

    


    public static boolean isOwner(Long resourceUserId) {
        return isOwner(getCurrentAuthentication(), resourceUserId);
    }

    


    public static boolean isOwner(Authentication authentication, Long resourceUserId) {
        if (authentication == null || resourceUserId == null) {
            return false;
        }

        String currentUserId = getCurrentUserId(authentication);
        return Objects.equals(currentUserId, resourceUserId.toString());
    }

    


    public static boolean isMerchantOwner(Long merchantId) {
        return isMerchantOwner(getCurrentAuthentication(), merchantId);
    }

    


    public static boolean isMerchantOwner(Authentication authentication, Long merchantId) {
        if (authentication == null || merchantId == null) {
            return false;
        }

        
        if (!isMerchant(authentication)) {
            return false;
        }

        
        String currentUserId = getCurrentUserId(authentication);
        return Objects.equals(currentUserId, merchantId.toString());
    }

    

    


    public static Jwt getCurrentJwt() {
        return getCurrentJwt(getCurrentAuthentication());
    }

    


    public static Jwt getCurrentJwt(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken();
        }
        return null;
    }

    


    public static Object getClaim(String claimName) {
        return getClaim(getCurrentAuthentication(), claimName);
    }

    


    public static Object getClaim(Authentication authentication, String claimName) {
        Jwt jwt = getCurrentJwt(authentication);
        return jwt != null ? jwt.getClaim(claimName) : null;
    }

    


    public static String getClaimAsString(String claimName) {
        return getClaimAsString(getCurrentAuthentication(), claimName);
    }

    


    public static String getClaimAsString(Authentication authentication, String claimName) {
        Jwt jwt = getCurrentJwt(authentication);
        return jwt != null ? jwt.getClaimAsString(claimName) : null;
    }

    

    


    public static boolean isAdminOrOwner(Long resourceUserId) {
        return isAdminOrOwner(getCurrentAuthentication(), resourceUserId);
    }

    


    public static boolean isAdminOrOwner(Authentication authentication, Long resourceUserId) {
        return isAdmin(authentication) || isOwner(authentication, resourceUserId);
    }

    


    public static boolean isAdminOrMerchantOwner(Long merchantId) {
        return isAdminOrMerchantOwner(getCurrentAuthentication(), merchantId);
    }

    


    public static boolean isAdminOrMerchantOwner(Authentication authentication, Long merchantId) {
        return isAdmin(authentication) || isMerchantOwner(authentication, merchantId);
    }

    

    


    public static boolean canAccessResource(Long resourceUserId) {
        return canAccessResource(getCurrentAuthentication(), resourceUserId);
    }

    


    public static boolean canAccessResource(Authentication authentication, Long resourceUserId) {
        
        if (isAdmin(authentication)) {
            return true;
        }

        
        String currentUserId = getCurrentUserId(authentication);
        return currentUserId != null && currentUserId.equals(resourceUserId.toString());
    }

    


    public static String getUserSummary() {
        return getUserSummary(getCurrentAuthentication());
    }

    


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
