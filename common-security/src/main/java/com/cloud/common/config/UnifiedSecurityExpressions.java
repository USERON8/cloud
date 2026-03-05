package com.cloud.common.config;

import com.cloud.common.security.SecurityPermissionUtils;

public class UnifiedSecurityExpressions {

    public boolean isAdmin() {
        return SecurityPermissionUtils.isAdmin();
    }

    public boolean isMerchant() {
        return SecurityPermissionUtils.isMerchant();
    }

    public boolean isUser() {
        return SecurityPermissionUtils.isUser();
    }

    public boolean isAdminOrOwner(Long resourceUserId) {
        return SecurityPermissionUtils.isAdminOrOwner(resourceUserId);
    }

    public boolean isAdminOrMerchantOwner(Long merchantId) {
        return SecurityPermissionUtils.isAdminOrMerchantOwner(merchantId);
    }

    public boolean hasPermission(String permission) {
        return SecurityPermissionUtils.hasAuthority(permission);
    }

    public boolean hasAnyPermission(String... permissions) {
        return SecurityPermissionUtils.hasAnyAuthority(permissions);
    }

    public boolean hasUserType(String userType) {
        return SecurityPermissionUtils.hasUserType(userType);
    }

    public boolean isSameUser(String userId) {
        return SecurityPermissionUtils.isSameUser(userId);
    }

    public boolean canAccessResource(Long resourceUserId) {
        return SecurityPermissionUtils.canAccessResource(resourceUserId);
    }

    public boolean isAdminOrSelf(String userId) {
        return SecurityPermissionUtils.isAdmin() || SecurityPermissionUtils.isSameUser(userId);
    }

    public boolean isAdminOrMerchantData(Long merchantId) {
        return SecurityPermissionUtils.isAdmin()
                || (SecurityPermissionUtils.isMerchant() && SecurityPermissionUtils.isMerchantOwner(merchantId));
    }

    public boolean hasUserRole(String role) {
        return SecurityPermissionUtils.hasRole(role);
    }

    public boolean hasAnyUserRole(String... roles) {
        return SecurityPermissionUtils.hasAnyRole(roles);
    }

    public String getCurrentUserSummary() {
        return SecurityPermissionUtils.getUserSummary();
    }
}
