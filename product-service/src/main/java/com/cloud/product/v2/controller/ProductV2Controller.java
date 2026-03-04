package com.cloud.product.v2.controller;

import com.cloud.common.result.Result;
import com.cloud.product.v2.entity.SkuV2;
import com.cloud.product.v2.entity.SpuV2;
import com.cloud.product.v2.service.ProductV2Service;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
public class ProductV2Controller {

    private final ProductV2Service productV2Service;

    @PostMapping("/spu")
    public Result<SpuV2> createSpu(@RequestBody SpuV2 spu) {
        return Result.success(productV2Service.createSpu(spu));
    }

    @PostMapping("/sku")
    public Result<SkuV2> createSku(@RequestBody SkuV2 sku) {
        return Result.success(productV2Service.createSku(sku));
    }

    @GetMapping("/spu/{spuId}/sku")
    public Result<List<SkuV2>> listSku(@PathVariable Long spuId) {
        return Result.success(productV2Service.listSkuBySpuId(spuId));
    }
}

