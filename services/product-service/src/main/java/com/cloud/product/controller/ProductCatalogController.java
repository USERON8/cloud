package com.cloud.product.controller;

import com.cloud.common.domain.dto.product.SpuCreateRequestDTO;
import com.cloud.common.domain.vo.product.SkuDetailVO;
import com.cloud.common.domain.vo.product.SpuDetailVO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.common.result.Result;
import com.cloud.common.security.SecurityPermissionUtils;
import com.cloud.product.controller.support.ProductMerchantGuard;
import com.cloud.product.controller.support.ProductPublicStatusSupport;
import com.cloud.product.service.ProductCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "商品目录接口", description = "SPU/SKU 商品目录管理接口")
public class ProductCatalogController {

  private final ProductCatalogService productCatalogService;
  private final ProductMerchantGuard productMerchantGuard;

  @PostMapping("/spus")
  @PreAuthorize("hasAuthority('product:create')")
  @Operation(summary = "创建 SPU")
  public Result<Long> createSpu(
      @Valid @RequestBody SpuCreateRequestDTO request, Authentication authentication) {
    productMerchantGuard.assertCanWriteMerchant(authentication, request.getSpu().getMerchantId());
    return Result.success(productCatalogService.createSpu(request));
  }

  @PutMapping("/spus/{spuId}")
  @PreAuthorize("hasAuthority('product:edit')")
  @Operation(summary = "更新 SPU")
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

  @GetMapping("/spus/{spuId}")
  @Operation(summary = "查询 SPU 详情")
  public Result<SpuDetailVO> getSpu(@PathVariable Long spuId, Authentication authentication) {
    SpuDetailVO detail = productCatalogService.getSpuById(spuId);
    if (detail == null) {
      throw new BizException(ResultCode.NOT_FOUND, "spu not found");
    }
    if (productMerchantGuard.canWriteMerchant(authentication, detail.getMerchantId())) {
      return Result.success(detail);
    }
    SpuDetailVO publicDetail = toPublicSpu(detail);
    if (publicDetail == null) {
      throw new BizException(ResultCode.NOT_FOUND, "spu not found");
    }
    return Result.success(publicDetail);
  }

  @GetMapping("/categories/{categoryId}/spus")
  @Operation(summary = "按分类查询 SPU")
  public Result<List<SpuDetailVO>> listByCategory(
      @PathVariable Long categoryId,
      @RequestParam(required = false) Integer status,
      Authentication authentication) {
    if (SecurityPermissionUtils.isAdmin(authentication)) {
      return Result.success(productCatalogService.listSpuByCategory(categoryId, status));
    }
    Integer effectiveStatus = ProductPublicStatusSupport.normalizePublicStatus(status);
    return Result.success(
        productCatalogService.listSpuByCategory(categoryId, effectiveStatus).stream()
            .map(this::toPublicSpu)
            .filter(Objects::nonNull)
            .toList());
  }

  @GetMapping("/skus")
  @Operation(summary = "批量查询 SKU 明细")
  public Result<List<SkuDetailVO>> listSkuByIds(
      @RequestParam("ids") List<Long> ids, Authentication authentication) {
    List<SkuDetailVO> skuDetails = productCatalogService.listSkuByIds(ids);
    if (skuDetails.isEmpty() || SecurityPermissionUtils.isAdmin(authentication)) {
      return Result.success(skuDetails);
    }
    Map<Long, SpuDetailVO> spuDetailsById = loadSpuDetailsById(skuDetails);
    Map<Long, Boolean> writableMerchantById = new HashMap<>();
    return Result.success(
        skuDetails.stream()
            .filter(
                sku ->
                    canReadSku(
                        authentication,
                        sku,
                        spuDetailsById.get(sku.getSpuId()),
                        writableMerchantById))
            .toList());
  }

  @PatchMapping("/spus/{spuId}/status")
  @PreAuthorize("hasAuthority('product:edit')")
  @Operation(summary = "更新 SPU 状态")
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

  private boolean isActiveSpu(SpuDetailVO detail) {
    return detail != null && Integer.valueOf(1).equals(detail.getStatus());
  }

  private Map<Long, SpuDetailVO> loadSpuDetailsById(List<SkuDetailVO> skuDetails) {
    return skuDetails.stream()
        .map(SkuDetailVO::getSpuId)
        .filter(Objects::nonNull)
        .distinct()
        .map(productCatalogService::getSpuById)
        .filter(Objects::nonNull)
        .collect(
            java.util.stream.Collectors.toMap(
                SpuDetailVO::getSpuId, detail -> detail, (left, right) -> left));
  }

  private boolean canReadSku(
      Authentication authentication,
      SkuDetailVO skuDetail,
      SpuDetailVO spuDetail,
      Map<Long, Boolean> writableMerchantById) {
    if (skuDetail == null || skuDetail.getSpuId() == null || spuDetail == null) {
      return false;
    }
    Long merchantId = spuDetail.getMerchantId();
    if (merchantId != null
        && writableMerchantById.computeIfAbsent(
            merchantId,
            ignored -> productMerchantGuard.canWriteMerchant(authentication, merchantId))) {
      return true;
    }
    return isActiveSku(skuDetail) && isActiveSpu(spuDetail);
  }
}
