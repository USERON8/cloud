package com.cloud.product.v2.service;

import com.cloud.product.v2.entity.SkuV2;
import com.cloud.product.v2.entity.SpuV2;

import java.util.List;

public interface ProductV2Service {
    SpuV2 createSpu(SpuV2 spu);
    SkuV2 createSku(SkuV2 sku);
    List<SkuV2> listSkuBySpuId(Long spuId);
}

