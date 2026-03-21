package com.cloud.product.controller;

import com.cloud.common.domain.dto.product.SpuCreateRequestDTO;
import com.cloud.common.domain.vo.product.SkuDetailVO;
import com.cloud.common.domain.vo.product.SpuDetailVO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.common.result.Result;
import com.cloud.common.security.SecurityPermissionUtils;
import com.cloud.product.controller.support.ProductMerchantGuard;
import com.cloud.product.service.ProductCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
@Tag(name = "Product Catalog API", description = "SPU/SKU catalog management APIs")
public class ProductCatalogController {

  private final ProductCatalogService productCatalogService;
  private final ProductMerchantGuard productMerchantGuard;

  @PostMapping("/spu")
  @PreAuthorize("hasAuthority('product:create')")
  @Operation(summary = "Create SPU")
  public Result<Long> createSpu(
      @Valid @RequestBody SpuCreateRequestDTO request, Authentication authentication) {
    productMerchantGuard.assertCanWriteMerchant(authentication, request.getSpu().getMerchantId());
    return Result.success(productCatalogService.createSpu(request));
  }

  @PutMapping("/spu/{spuId}")
  @PreAuthorize("hasAuthority('product:edit')")
  @Operation(summary = "Update SPU")
  public Result<Boolean> updateSpu(
      @PathVariable Long spuId,
      @Valid @RequestBody SpuCreateRequestDTO request,
      Authentication authentication) {
    SpuDetailVO existing = productMerchantGuard.requireWritableSpu(authentication, spuId);
    if (!SecurityPermissionUtils.isAdmin(authentication)) {
      request.getSpu().setMerchantId(existing.getMerchantId());
    }
    return Result.success(productCatalogService.updateSpu(spuId, request));
  }

  @GetMapping("/spu/{spuId}")
  @Operation(summary = "Get SPU detail")
  public Result<SpuDetailVO> getSpu(@PathVariable Long spuId) {
    SpuDetailVO detail = toPublicSpu(productCatalogService.getSpuById(spuId));
    if (detail == null) {
      throw new BizException(ResultCode.NOT_FOUND, "spu not found");
    }
    return Result.success(detail);
  }

  @GetMapping("/spu/category/{categoryId}")
  @Operation(summary = "List SPU by category")
  public Result<List<SpuDetailVO>> listByCategory(
      @PathVariable Long categoryId, @RequestParam(required = false) Integer status) {
    Integer effectiveStatus = status != null ? status : 1;
    return Result.success(
        productCatalogService.listSpuByCategory(categoryId, effectiveStatus).stream()
            .map(this::toPublicSpu)
            .filter(Objects::nonNull)
            .toList());
  }

  @GetMapping("/sku/batch")
  @Operation(summary = "Batch query SKU details")
  public Result<List<SkuDetailVO>> listSkuByIds(@RequestParam List<Long> skuIds) {
    return Result.success(
        productCatalogService.listSkuByIds(skuIds).stream().filter(this::isActiveSku).toList());
  }

  @PatchMapping("/spu/{spuId}/status")
  @PreAuthorize("hasAuthority('product:edit')")
  @Operation(summary = "Update SPU status")
  public Result<Boolean> updateSpuStatus(
      @PathVariable Long spuId, @RequestParam Integer status, Authentication authentication) {
    productMerchantGuard.requireWritableSpu(authentication, spuId);
    return Result.success(productCatalogService.updateSpuStatus(spuId, status));
  }

  private SpuDetailVO toPublicSpu(SpuDetailVO detail) {
    if (detail == null || !Integer.valueOf(1).equals(detail.getStatus())) {
      return null;
    }
    List<SkuDetailVO> activeSkus =
        detail.getSkus() == null
            ? List.of()
            : detail.getSkus().stream().filter(this::isActiveSku).toList();
    if (activeSkus.isEmpty()) {
      return null;
    }
    detail.setSkus(activeSkus);
    return detail;
  }

  private boolean isActiveSku(SkuDetailVO detail) {
    return detail != null && Integer.valueOf(1).equals(detail.getStatus());
  }
}
