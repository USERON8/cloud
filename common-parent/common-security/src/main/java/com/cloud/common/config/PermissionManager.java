package com.cloud.common.config;

import com.cloud.common.security.SecurityPermissionUtils;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("permissionManager")
public class PermissionManager {

  public boolean hasUserAccess(Authentication authentication) {
    return SecurityPermissionUtils.hasRole(authentication, "USER");
  }

  public boolean hasAdminAccess(Authentication authentication) {
    return SecurityPermissionUtils.hasRole(authentication, "ADMIN");
  }

  public boolean hasMerchantAccess(Authentication authentication) {
    return SecurityPermissionUtils.hasRole(authentication, "MERCHANT");
  }

  public boolean isMerchantOwner(Long merchantId, Authentication authentication) {
    if (hasAdminAccess(authentication)) {
      return true;
    }
    return SecurityPermissionUtils.isMerchantOwner(authentication, merchantId);
  }
}
