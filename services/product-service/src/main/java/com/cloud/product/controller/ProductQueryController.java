package com.cloud.product.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.domain.vo.product.SpuDetailVO;
import com.cloud.common.result.PageResult;
import com.cloud.common.result.Result;
import com.cloud.common.security.SecurityPermissionUtils;
import com.cloud.product.dto.ProductItemDTO;
import com.cloud.product.mapper.SkuMapper;
import com.cloud.product.mapper.SpuMapper;
import com.cloud.product.module.entity.Sku;
import com.cloud.product.module.entity.Spu;
import com.cloud.product.service.ProductCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
@Tag(name = "Product Query API", description = "Product list and search APIs")
public class ProductQueryController {

    private final SpuMapper spuMapper;
    private final SkuMapper skuMapper;
    private final ProductCatalogService productCatalogService;

    @Value("${product.config.page.max-size:100}")
    private Integer maxListSize;

    @GetMapping
    @Operation(summary = "List products")
    public Result<PageResult<ProductItemDTO>> listProducts(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Integer status) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size <= 0 ? 20 : size;
        int maxSize = maxListSize == null || maxListSize <= 0 ? 100 : maxListSize;
        safeSize = Math.min(safeSize, maxSize);

        LambdaQueryWrapper<Spu> wrapper = new LambdaQueryWrapper<Spu>()
                .eq(Spu::getDeleted, 0);
        if (StrUtil.isNotBlank(name)) {
            wrapper.like(Spu::getSpuName, name.trim());
        }
        if (categoryId != null) {
            wrapper.eq(Spu::getCategoryId, categoryId);
        }
        if (brandId != null) {
            wrapper.eq(Spu::getBrandId, brandId);
        }
        if (status != null) {
            wrapper.eq(Spu::getStatus, status);
        }
        wrapper.orderByDesc(Spu::getId);

        Page<Spu> pageData = new Page<>(safePage, safeSize);
        Page<Spu> result = spuMapper.selectPage(pageData, wrapper);
        List<ProductItemDTO> items = buildProductItems(result == null ? Collections.emptyList() : result.getRecords());
        long total = result == null ? 0L : result.getTotal();
        PageResult<ProductItemDTO> pageResult = PageResult.of((long) safePage, (long) safeSize, total, items);
        return Result.success(pageResult);
    }

    @GetMapping("/search")
    @Operation(summary = "Search products by name")
    public Result<List<ProductItemDTO>> searchProducts(@RequestParam String name,
                                                       @RequestParam(required = false) Integer size) {
        if (StrUtil.isBlank(name)) {
            return Result.success(List.of());
        }
        int safeSize = size == null || size <= 0 ? 10 : size;
        int maxSize = maxListSize == null || maxListSize <= 0 ? 100 : maxListSize;
        safeSize = Math.min(safeSize, maxSize);

        LambdaQueryWrapper<Spu> wrapper = new LambdaQueryWrapper<Spu>()
                .eq(Spu::getDeleted, 0)
                .like(Spu::getSpuName, name.trim())
                .orderByDesc(Spu::getId)
                .last("LIMIT " + safeSize);

        List<Spu> spus = spuMapper.selectList(wrapper);
        return Result.success(buildProductItems(spus));
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

    private List<ProductItemDTO> buildProductItems(List<Spu> spus) {
        if (spus == null || spus.isEmpty()) {
            return List.of();
        }
        List<Long> spuIds = spus.stream()
                .map(Spu::getId)
                .filter(id -> id != null)
                .toList();
        Map<Long, Sku> minSkuBySpu = new HashMap<>();
        if (!spuIds.isEmpty()) {
            List<Sku> skus = skuMapper.selectList(new LambdaQueryWrapper<Sku>()
                    .in(Sku::getSpuId, spuIds)
                    .eq(Sku::getDeleted, 0));
            for (Sku sku : skus) {
                if (sku == null || sku.getSpuId() == null) {
                    continue;
                }
                Sku current = minSkuBySpu.get(sku.getSpuId());
                if (shouldReplaceSku(current, sku)) {
                    minSkuBySpu.put(sku.getSpuId(), sku);
                }
            }
        }

        List<ProductItemDTO> items = new ArrayList<>(spus.size());
        for (Spu spu : spus) {
            if (spu == null) {
                continue;
            }
            ProductItemDTO item = new ProductItemDTO();
            item.setId(spu.getId());
            item.setShopId(spu.getMerchantId());
            item.setName(spu.getSpuName());
            item.setCategoryId(spu.getCategoryId());
            item.setBrandId(spu.getBrandId());
            item.setStatus(spu.getStatus());
            item.setDescription(spu.getDescription());

            Sku sku = minSkuBySpu.get(spu.getId());
            if (sku != null) {
                item.setPrice(sku.getSalePrice());
                item.setImageUrl(sku.getImageUrl());
            }
            if (item.getImageUrl() == null) {
                item.setImageUrl(spu.getMainImage());
            }
            items.add(item);
        }
        return items;
    }

    private boolean shouldReplaceSku(Sku current, Sku candidate) {
        if (candidate == null) {
            return false;
        }
        if (current == null) {
            return true;
        }
        BigDecimal currentPrice = current.getSalePrice();
        BigDecimal candidatePrice = candidate.getSalePrice();
        if (currentPrice == null) {
            return candidatePrice != null;
        }
        if (candidatePrice == null) {
            return false;
        }
        return candidatePrice.compareTo(currentPrice) < 0;
    }
}
