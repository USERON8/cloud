package com.cloud.product.controller;

import com.cloud.common.domain.dto.product.SpuCreateRequestDTO;
import com.cloud.common.domain.vo.product.SkuDetailVO;
import com.cloud.common.domain.vo.product.SpuDetailVO;
import com.cloud.common.result.Result;
import com.cloud.product.service.ProductCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductCatalogController {

    private final ProductCatalogService productCatalogService;

    @PostMapping("/spu")
    public Result<Long> createSpu(@Valid @RequestBody SpuCreateRequestDTO request) {
        return Result.success(productCatalogService.createSpu(request));
    }

    @PutMapping("/spu/{spuId}")
    public Result<Boolean> updateSpu(@PathVariable Long spuId, @Valid @RequestBody SpuCreateRequestDTO request) {
        return Result.success(productCatalogService.updateSpu(spuId, request));
    }

    @GetMapping("/spu/{spuId}")
    public Result<SpuDetailVO> getSpu(@PathVariable Long spuId) {
        return Result.success(productCatalogService.getSpuById(spuId));
    }

    @GetMapping("/spu/category/{categoryId}")
    public Result<List<SpuDetailVO>> listByCategory(@PathVariable Long categoryId,
                                                     @RequestParam(required = false) Integer status) {
        return Result.success(productCatalogService.listSpuByCategory(categoryId, status));
    }

    @GetMapping("/sku/batch")
    public Result<List<SkuDetailVO>> listSkuByIds(@RequestParam List<Long> skuIds) {
        return Result.success(productCatalogService.listSkuByIds(skuIds));
    }

    @PatchMapping("/spu/{spuId}/status")
    public Result<Boolean> updateSpuStatus(@PathVariable Long spuId, @RequestParam Integer status) {
        return Result.success(productCatalogService.updateSpuStatus(spuId, status));
    }
}
