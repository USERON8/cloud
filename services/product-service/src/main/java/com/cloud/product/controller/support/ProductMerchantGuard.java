package com.cloud.product.controller.support;

import com.cloud.api.user.UserDubboApi;
import com.cloud.common.domain.vo.product.SpuDetailVO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.common.security.SecurityPermissionUtils;
import com.cloud.product.service.ProductCatalogService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductMerchantGuard {

  private final ProductCatalogService productCatalogService;

  @DubboReference(check = false, timeout = 5000, retries = 0)
  private UserDubboApi userDubboApi;

  public void assertCanWriteMerchant(Authentication authentication, Long merchantId) {
    if (!canWriteMerchantData(authentication, merchantId)) {
      throw new BizException(
          ResultCode.FORBIDDEN, "forbidden to operate another merchant's product");
    }
  }

  public SpuDetailVO requireWritableSpu(Authentication authentication, Long spuId) {
    SpuDetailVO existing = productCatalogService.getSpuById(spuId);
    if (existing == null) {
      throw new BizException(ResultCode.NOT_FOUND, "spu not found");
    }
    assertCanWriteMerchant(authentication, existing.getMerchantId());
    return existing;
  }

  private boolean canWriteMerchantData(Authentication authentication, Long merchantId) {
    if (SecurityPermissionUtils.isAdmin(authentication)) {
      return true;
    }
    String currentUserId = SecurityPermissionUtils.getCurrentUserId(authentication);
    if (currentUserId == null || currentUserId.isBlank()) {
      return false;
    }
    Long userId;
    try {
      userId = Long.parseLong(currentUserId);
    } catch (NumberFormatException ex) {
      return false;
    }
    return Boolean.TRUE.equals(userDubboApi.isMerchantOwner(merchantId, userId));
  }
}
