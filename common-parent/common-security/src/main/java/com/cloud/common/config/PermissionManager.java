package com.cloud.common.config;

import com.cloud.common.security.MerchantOwnershipResolver;
import com.cloud.common.security.SecurityPermissionUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("permissionManager")
public class PermissionManager {

  private final MerchantOwnershipResolver merchantOwnershipResolver;

  public PermissionManager(ObjectProvider<MerchantOwnershipResolver> merchantOwnershipResolver) {
    this.merchantOwnershipResolver = merchantOwnershipResolver.getIfAvailable();
  }

  private boolean hasAdminAccess(Authentication authentication) {
    return SecurityPermissionUtils.hasRole(authentication, "ADMIN");
  }

  public boolean isMerchantOwner(Long merchantId, Authentication authentication) {
    if (hasAdminAccess(authentication)) {
      return true;
    }
    if (merchantOwnershipResolver == null) {
      return SecurityPermissionUtils.isMerchantOwner(authentication, merchantId);
    }
    return merchantOwnershipResolver.isMerchantOwner(authentication, merchantId);
  }
}
