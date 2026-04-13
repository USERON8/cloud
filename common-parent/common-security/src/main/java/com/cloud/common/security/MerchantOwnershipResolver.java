package com.cloud.common.security;

import org.springframework.security.core.Authentication;

public interface MerchantOwnershipResolver {

  boolean isMerchantOwner(Authentication authentication, Long merchantId);
}
