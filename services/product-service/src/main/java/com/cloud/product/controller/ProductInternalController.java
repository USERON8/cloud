package com.cloud.product.controller;

import com.cloud.api.product.ProductDubboApi;
import com.cloud.common.domain.dto.product.SpuCreateRequestDTO;
import com.cloud.common.domain.vo.product.SkuDetailVO;
import com.cloud.common.domain.vo.product.SpuDetailVO;
import com.cloud.product.service.ProductCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal/product")
@RequiredArgsConstructor
public class ProductInternalController implements ProductDubboApi {

    private final ProductCatalogService productCatalogService;

    @Override
    @PostMapping("/spu")
    public Long createSpu(@RequestBody SpuCreateRequestDTO request) {
        return productCatalogService.createSpu(request);
    }

    @Override
    @PutMapping("/spu/{spuId}")
    public Boolean updateSpu(@PathVariable("spuId") Long spuId, @RequestBody SpuCreateRequestDTO request) {
        return productCatalogService.updateSpu(spuId, request);
    }

    @Override
    @GetMapping("/spu/{spuId}")
    public SpuDetailVO getSpuById(@PathVariable("spuId") Long spuId) {
        return productCatalogService.getSpuById(spuId);
    }

    @Override
    @GetMapping("/spu/category/{categoryId}")
    public List<SpuDetailVO> listSpuByCategory(@PathVariable("categoryId") Long categoryId,
                                                @RequestParam(value = "status", required = false) Integer status) {
        return productCatalogService.listSpuByCategory(categoryId, status);
    }

    @Override
    @GetMapping("/sku/batch")
    public List<SkuDetailVO> listSkuByIds(@RequestParam("skuIds") List<Long> skuIds) {
        return productCatalogService.listSkuByIds(skuIds);
    }

    @Override
    @PatchMapping("/spu/{spuId}/status")
    public Boolean updateSpuStatus(@PathVariable("spuId") Long spuId, @RequestParam("status") Integer status) {
        return productCatalogService.updateSpuStatus(spuId, status);
    }
}

