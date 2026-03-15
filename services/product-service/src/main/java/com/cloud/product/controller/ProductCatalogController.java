package com.cloud.product.controller;

import com.cloud.common.domain.dto.product.SpuCreateRequestDTO;
import com.cloud.common.domain.vo.product.SkuDetailVO;
import com.cloud.common.domain.vo.product.SpuDetailVO;
import com.cloud.common.result.Result;
import com.cloud.common.security.SecurityPermissionUtils;
import com.cloud.product.service.ProductCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

import java.util.List;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
@Tag(name = "Product Catalog API", description = "SPU/SKU catalog management APIs")
public class ProductCatalogController {

    private final ProductCatalogService productCatalogService;

    @PostMapping("/spu")
    @PreAuthorize("hasAuthority('product:create')")
    @Operation(summary = "Create SPU")
    public Result<Long> createSpu(@Valid @RequestBody SpuCreateRequestDTO request, Authentication authentication) {
        if (!canWriteMerchantData(authentication, request.getSpu().getMerchantId())) {
            return Result.forbidden("forbidden to create product for another merchant");
        }
        return Result.success(productCatalogService.createSpu(request));
    }

    @PutMapping("/spu/{spuId}")
    @PreAuthorize("hasAuthority('product:edit')")
    @Operation(summary = "Update SPU")
    public Result<Boolean> updateSpu(@PathVariable Long spuId,
                                     @Valid @RequestBody SpuCreateRequestDTO request,
                                     Authentication authentication) {
        SpuDetailVO existing = productCatalogService.getSpuById(spuId);
        if (existing == null) {
            return Result.notFound("spu not found");
        }
        if (!canWriteMerchantData(authentication, existing.getMerchantId())) {
            return Result.forbidden("forbidden to update another merchant's product");
        }
        if (!SecurityPermissionUtils.isAdmin(authentication)) {
            request.getSpu().setMerchantId(existing.getMerchantId());
        }
        return Result.success(productCatalogService.updateSpu(spuId, request));
    }

    @GetMapping("/spu/{spuId}")
    @Operation(summary = "Get SPU detail")
    public Result<SpuDetailVO> getSpu(@PathVariable Long spuId) {
        return Result.success(productCatalogService.getSpuById(spuId));
    }

    @GetMapping("/spu/category/{categoryId}")
    @Operation(summary = "List SPU by category")
    public Result<List<SpuDetailVO>> listByCategory(@PathVariable Long categoryId,
                                                     @RequestParam(required = false) Integer status) {
        return Result.success(productCatalogService.listSpuByCategory(categoryId, status));
    }

    @GetMapping("/sku/batch")
    @Operation(summary = "Batch query SKU details")
    public Result<List<SkuDetailVO>> listSkuByIds(@RequestParam List<Long> skuIds) {
        return Result.success(productCatalogService.listSkuByIds(skuIds));
    }

    @PatchMapping("/spu/{spuId}/status")
    @PreAuthorize("hasAuthority('product:edit')")
    @Operation(summary = "Update SPU status")
    public Result<Boolean> updateSpuStatus(@PathVariable Long spuId,
                                           @RequestParam Integer status,
                                           Authentication authentication) {
        SpuDetailVO existing = productCatalogService.getSpuById(spuId);
        if (existing == null) {
            return Result.notFound("spu not found");
        }
        if (!canWriteMerchantData(authentication, existing.getMerchantId())) {
            return Result.forbidden("forbidden to update another merchant's product");
        }
        return Result.success(productCatalogService.updateSpuStatus(spuId, status));
    }

    private boolean canWriteMerchantData(Authentication authentication, Long merchantId) {
        return SecurityPermissionUtils.isAdmin(authentication)
                || SecurityPermissionUtils.isMerchantOwner(authentication, merchantId);
    }
}
