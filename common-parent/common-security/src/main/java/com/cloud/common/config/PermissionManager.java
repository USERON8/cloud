package com.cloud.common.config;

import com.cloud.common.security.SecurityPermissionUtils;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("permissionManager")
public class PermissionManager {

  private boolean hasAdminAccess(Authentication authentication) {
    return SecurityPermissionUtils.hasRole(authentication, "ADMIN");
  }

  public boolean isMerchantOwner(Long merchantId, Authentication authentication) {
    if (hasAdminAccess(authentication)) {
      return true;
    }
    return SecurityPermissionUtils.isMerchantOwner(authentication, merchantId);
  }
}
