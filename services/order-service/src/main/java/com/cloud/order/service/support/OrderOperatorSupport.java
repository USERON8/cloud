package com.cloud.order.service.support;

import com.cloud.api.user.UserDubboApi;
import com.cloud.common.exception.BizException;
import com.cloud.common.security.SecurityPermissionUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderOperatorSupport {

  @org.apache.dubbo.config.annotation.DubboReference(check = false, timeout = 5000, retries = 0)
  private UserDubboApi userDubboApi;

  public Long requireCurrentUserId(Authentication authentication) {
    return SecurityPermissionUtils.requireCurrentUserIdAsLong(authentication);
  }

  public Long requireCurrentMerchantId(Authentication authentication) {
    Long currentMerchantId =
        userDubboApi.findMerchantIdByOwnerUserId(requireCurrentUserId(authentication));
    if (currentMerchantId == null) {
      throw new BizException("current merchant not found");
    }
    return currentMerchantId;
  }
}
