package com.cloud.product.controller;

import com.cloud.common.domain.vo.product.SpuDetailVO;
import com.cloud.common.result.PageResult;
import com.cloud.common.result.Result;
import com.cloud.common.security.SecurityPermissionUtils;
import com.cloud.product.dto.ProductItemDTO;
import com.cloud.product.service.ProductCatalogService;
import com.cloud.product.service.ProductQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
@Tag(name = "Product Query API", description = "Product list and search APIs")
public class ProductQueryController {

    private final ProductCatalogService productCatalogService;
    private final ProductQueryService productQueryService;

    @GetMapping
    @Operation(summary = "List products")
    public Result<PageResult<ProductItemDTO>> listProducts(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Integer status) {
        return Result.success(productQueryService.listProducts(page, size, name, categoryId, brandId, status));
    }

    @GetMapping("/search")
    @Operation(summary = "Search products by name")
    public Result<List<ProductItemDTO>> searchProducts(@RequestParam String name,
                                                       @RequestParam(required = false) Integer size) {
        return Result.success(productQueryService.searchProducts(name, size));
    }

    @PatchMapping("/{spuId}/status")
    @PreAuthorize("hasAuthority('product:edit')")
    @Operation(summary = "Update product status")
    public Result<Boolean> updateProductStatus(@PathVariable Long spuId,
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
