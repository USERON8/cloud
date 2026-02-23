package com.cloud.common.config;

import com.cloud.common.security.SecurityPermissionUtils;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("permissionManager")
public class PermissionManager {

    public boolean hasUserAccess(Authentication authentication) {
        return SecurityPermissionUtils.hasRole(authentication, "USER")
                || SecurityPermissionUtils.hasUserType(authentication, "USER");
    }

    public boolean hasAdminAccess(Authentication authentication) {
        return SecurityPermissionUtils.hasRole(authentication, "ADMIN")
                || SecurityPermissionUtils.hasUserType(authentication, "ADMIN");
    }

    public boolean hasMerchantAccess(Authentication authentication) {
        return SecurityPermissionUtils.hasRole(authentication, "MERCHANT")
                || SecurityPermissionUtils.hasUserType(authentication, "MERCHANT");
    }

    public boolean isMerchantOwner(Long merchantId, Authentication authentication) {
        if (hasAdminAccess(authentication)) {
            return true;
        }
        return SecurityPermissionUtils.isMerchantOwner(authentication, merchantId);
    }
}
