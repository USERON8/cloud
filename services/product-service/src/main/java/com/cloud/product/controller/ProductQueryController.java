package com.cloud.product.controller;

import com.cloud.api.user.UserDubboApi;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.common.result.PageResult;
import com.cloud.common.result.Result;
import com.cloud.common.security.SecurityPermissionUtils;
import com.cloud.product.controller.support.ProductMerchantGuard;
import com.cloud.product.dto.ProductItemDTO;
import com.cloud.product.service.ProductQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Product Query API", description = "Product list and search APIs")
public class ProductQueryController {

  private final ProductQueryService productQueryService;
  private final ProductMerchantGuard productMerchantGuard;

  @DubboReference(check = false, timeout = 5000, retries = 0)
  private UserDubboApi userDubboApi;

  @GetMapping("/products")
  @Operation(summary = "List products")
  public Result<PageResult<ProductItemDTO>> listProducts(
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) Long categoryId,
      @RequestParam(required = false) Long brandId,
      @RequestParam(required = false) Integer status) {
    Integer effectiveStatus = normalizePublicStatus(status);
    return Result.success(
        productQueryService.listProducts(
            page, size, name, categoryId, brandId, null, effectiveStatus));
  }

  @GetMapping("/spus")
  @PreAuthorize("hasAuthority('product:edit')")
  @Operation(summary = "List products for product management")
  public Result<PageResult<ProductItemDTO>> listManageProducts(
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) Long categoryId,
      @RequestParam(required = false) Long brandId,
      @RequestParam(required = false) Long merchantId,
      @RequestParam(required = false) Integer status,
      Authentication authentication) {
    Long effectiveMerchantId = resolveManageMerchantId(authentication, merchantId);
    return Result.success(
        productQueryService.listProducts(
            page, size, name, categoryId, brandId, effectiveMerchantId, status));
  }

  private Integer normalizePublicStatus(Integer status) {
    if (status == null) {
      return 1;
    }
    if (!Integer.valueOf(1).equals(status)) {
      throw new BizException(
          ResultCode.BAD_REQUEST, "public product queries only support active status");
    }
    return status;
  }

  private Long resolveManageMerchantId(Authentication authentication, Long merchantId) {
    if (SecurityPermissionUtils.isAdmin(authentication)) {
      return merchantId;
    }
    String currentUserId = SecurityPermissionUtils.getCurrentUserId(authentication);
    if (currentUserId == null || currentUserId.isBlank()) {
      throw new BizException(ResultCode.UNAUTHORIZED, "current merchant is not available");
    }
    Long currentUserIdValue;
    try {
      currentUserIdValue = Long.parseLong(currentUserId);
    } catch (NumberFormatException ex) {
      throw new BizException(ResultCode.UNAUTHORIZED, "invalid user id in token");
    }
    Long effectiveMerchantId = merchantId;
    if (effectiveMerchantId == null) {
      effectiveMerchantId = userDubboApi.findMerchantIdByOwnerUserId(currentUserIdValue);
    }
    if (effectiveMerchantId == null) {
      throw new BizException(ResultCode.NOT_FOUND, "merchant not found for current user");
    }
    productMerchantGuard.assertCanWriteMerchant(authentication, effectiveMerchantId);
    return effectiveMerchantId;
  }
}
