package com.cloud.user.service.support;

import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.common.security.MerchantOwnershipResolver;
import com.cloud.common.security.SecurityPermissionUtils;
import com.cloud.user.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MerchantAuthorizationService implements MerchantOwnershipResolver {

  private final MerchantService merchantService;

  public void assertCanReadMerchant(Authentication authentication, Long merchantId) {
    if (!isMerchantOwner(authentication, merchantId)) {
      throw new BizException(ResultCode.FORBIDDEN, "no permission to access merchant");
    }
  }

  public void assertCanWriteMerchant(Authentication authentication, Long merchantId) {
    if (!isMerchantOwner(authentication, merchantId)) {
      throw new BizException(ResultCode.FORBIDDEN, "no permission to operate merchant");
    }
  }

  public void assertCanAuditMerchant(Authentication authentication) {
    if (!SecurityPermissionUtils.isAdmin(authentication)
        && !SecurityPermissionUtils.hasAuthority(authentication, "merchant:audit")) {
      throw new BizException(ResultCode.FORBIDDEN, "no permission to audit merchant");
    }
  }

  @Override
  public boolean isMerchantOwner(Authentication authentication, Long merchantId) {
    if (SecurityPermissionUtils.isAdmin(authentication)) {
      return true;
    }
    Long currentUserId = parseCurrentUserId(authentication);
    return merchantService.isMerchantOwner(merchantId, currentUserId);
  }

  public boolean isMerchantOwner(Long merchantId, Long userId) {
    return merchantService.isMerchantOwner(merchantId, userId);
  }

  private Long parseCurrentUserId(Authentication authentication) {
    String currentUserId = SecurityPermissionUtils.getCurrentUserId(authentication);
    if (currentUserId == null || currentUserId.isBlank()) {
      return null;
    }
    try {
      return Long.parseLong(currentUserId);
    } catch (NumberFormatException ex) {
      return null;
    }
  }
}
