package com.cloud.product.v2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.product.v2.entity.SkuV2;
import com.cloud.product.v2.entity.SpuV2;
import com.cloud.product.v2.mapper.SkuV2Mapper;
import com.cloud.product.v2.mapper.SpuV2Mapper;
import com.cloud.product.v2.service.ProductV2Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductV2ServiceImpl implements ProductV2Service {

    private final SpuV2Mapper spuV2Mapper;
    private final SkuV2Mapper skuV2Mapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SpuV2 createSpu(SpuV2 spu) {
        if (spu.getSpuNo() == null || spu.getSpuNo().isBlank()) {
            spu.setSpuNo("SPU-" + System.currentTimeMillis());
        }
        if (spu.getSaleStatus() == null) {
            spu.setSaleStatus("DRAFT");
        }
        if (spu.getAuditStatus() == null) {
            spu.setAuditStatus("PENDING");
        }
        spuV2Mapper.insert(spu);
        return spu;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SkuV2 createSku(SkuV2 sku) {
        if (sku.getSkuNo() == null || sku.getSkuNo().isBlank()) {
            sku.setSkuNo("SKU-" + System.currentTimeMillis());
        }
        if (sku.getStatus() == null) {
            sku.setStatus("ON_SHELF");
        }
        skuV2Mapper.insert(sku);
        return sku;
    }

    @Override
    public List<SkuV2> listSkuBySpuId(Long spuId) {
        return skuV2Mapper.selectList(new LambdaQueryWrapper<SkuV2>()
                .eq(SkuV2::getSpuId, spuId)
                .eq(SkuV2::getDeleted, 0));
    }
}

