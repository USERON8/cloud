package com.cloud.common.config;

import com.cloud.common.enums.UserType;
import com.cloud.common.exception.PermissionException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;







@Component
public class PermissionChecker {

    






    public boolean checkUserType(UserType requiredType, UserType currentType) {
        if (requiredType == null || currentType == null) {
            return false;
        }

        return requiredType.equals(currentType);
    }

    





    public boolean checkAdminPermission(UserType currentType) {
        return UserType.ADMIN.equals(currentType);
    }

    





    public boolean checkMerchantPermission(UserType currentType) {
        return UserType.ADMIN.equals(currentType);
    }

    





    public boolean checkUserPermission(UserType currentType) {
        return UserType.USER.equals(currentType);
    }

    





    public void assertUserType(String requiredType) {
        UserType currentType = getCurrentUserType();
        UserType required;

        try {
            required = UserType.valueOf(requiredType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new PermissionException("INVALID_USER_TYPE", "鏃犳晥鐨勭敤鎴风被鍨? " + requiredType);
        }

        if (!checkUserType(required, currentType)) {
            throw new PermissionException("ACCESS_DENIED",
                    "褰撳墠鐢ㄦ埛绫诲瀷[" + (currentType != null ? currentType.name() : "鏈煡") + "]鏃犳潈闄愯闂紝闇€瑕佺敤鎴风被鍨? " + requiredType);
        }
    }

    




    private UserType getCurrentUserType() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        if (authentication instanceof JwtAuthenticationToken jwtToken) {
            String userType = jwtToken.getToken().getClaimAsString("user_type");
            if (userType != null) {
                try {
                    return UserType.valueOf(userType.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
        }

        
        return authentication.getAuthorities().stream()
                .filter(auth -> auth.getAuthority().startsWith("USER_TYPE_"))
                .findFirst()
                .map(auth -> {
                    String type = auth.getAuthority().substring("USER_TYPE_".length());
                    try {
                        return UserType.valueOf(type);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .orElse(null);
    }
}
