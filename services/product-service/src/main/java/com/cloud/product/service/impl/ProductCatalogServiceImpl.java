package com.cloud.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.common.domain.dto.product.SkuDTO;
import com.cloud.common.domain.dto.product.SpuCreateRequestDTO;
import com.cloud.common.domain.dto.product.SpuDTO;
import com.cloud.common.domain.vo.product.SkuDetailVO;
import com.cloud.common.domain.vo.product.SpuDetailVO;
import com.cloud.common.exception.BusinessException;
import com.cloud.product.mapper.SkuMapper;
import com.cloud.product.mapper.SpuMapper;
import com.cloud.product.module.entity.Sku;
import com.cloud.product.module.entity.Spu;
import com.cloud.product.service.ProductCatalogService;
import com.cloud.product.service.support.ProductDetailCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductCatalogServiceImpl implements ProductCatalogService {

    private final SpuMapper spuMapper;
    private final SkuMapper skuMapper;
    private final ProductDetailCacheService productDetailCacheService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSpu(SpuCreateRequestDTO request) {
        SpuDTO spuDTO = request.getSpu();
        Spu spu = toSpuEntity(spuDTO);
        try {
            spuMapper.insert(spu);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException("spu already exists");
        }

        for (SkuDTO skuDTO : request.getSkus()) {
            Sku sku = toSkuEntity(spu.getId(), skuDTO);
            skuMapper.insert(sku);
        }
        return spu.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateSpu(Long spuId, SpuCreateRequestDTO request) {
        Spu existing = spuMapper.selectById(spuId);
        if (existing == null || existing.getDeleted() == 1) {
            throw new BusinessException("spu not found");
        }

        SpuDTO spuDTO = request.getSpu();
        existing.setSpuName(spuDTO.getSpuName());
        existing.setSubtitle(spuDTO.getSubtitle());
        existing.setCategoryId(spuDTO.getCategoryId());
        existing.setBrandId(spuDTO.getBrandId());
        existing.setMerchantId(spuDTO.getMerchantId());
        existing.setStatus(spuDTO.getStatus());
        existing.setDescription(spuDTO.getDescription());
        existing.setMainImage(spuDTO.getMainImage());
        spuMapper.updateById(existing);

        List<Sku> oldSkus = skuMapper.selectList(new LambdaQueryWrapper<Sku>()
                .eq(Sku::getSpuId, spuId)
                .eq(Sku::getDeleted, 0));
        Map<Long, Sku> existingById = new LinkedHashMap<>();
        Map<String, Sku> existingByCode = new LinkedHashMap<>();
        for (Sku oldSku : oldSkus) {
            if (oldSku.getId() != null) {
                existingById.put(oldSku.getId(), oldSku);
            }
            if (oldSku.getSkuCode() != null && !oldSku.getSkuCode().isBlank()) {
                existingByCode.put(oldSku.getSkuCode(), oldSku);
            }
        }

        Set<Long> retainedIds = new java.util.HashSet<>();
        for (SkuDTO skuDTO : request.getSkus()) {
            Sku existingSku = null;
            if (skuDTO.getSkuId() != null) {
                existingSku = existingById.get(skuDTO.getSkuId());
            }
            if (existingSku == null && skuDTO.getSkuCode() != null && !skuDTO.getSkuCode().isBlank()) {
                existingSku = existingByCode.get(skuDTO.getSkuCode());
            }

            if (existingSku != null) {
                existingSku.setSkuCode(skuDTO.getSkuCode());
                existingSku.setSkuName(skuDTO.getSkuName());
                existingSku.setSpecJson(skuDTO.getSpecJson());
                existingSku.setSalePrice(skuDTO.getSalePrice());
                existingSku.setMarketPrice(skuDTO.getMarketPrice());
                existingSku.setCostPrice(skuDTO.getCostPrice());
                existingSku.setStatus(skuDTO.getStatus());
                existingSku.setImageUrl(skuDTO.getImageUrl());
                existingSku.setDeleted(0);
                skuMapper.updateById(existingSku);
                if (existingSku.getId() != null) {
                    retainedIds.add(existingSku.getId());
                }
            } else {
                Sku sku = toSkuEntity(spuId, skuDTO);
                skuMapper.insert(sku);
            }
        }

        for (Sku oldSku : oldSkus) {
            if (oldSku.getId() == null) {
                continue;
            }
            if (retainedIds.contains(oldSku.getId())) {
                continue;
            }
            oldSku.setDeleted(1);
            skuMapper.updateById(oldSku);
        }
        productDetailCacheService.evict(spuId);
        return true;
    }

    @Override
    public SpuDetailVO getSpuById(Long spuId) {
        return productDetailCacheService.getOrLoad(spuId, () -> loadSpuDetail(spuId));
    }

    @Override
    public List<SpuDetailVO> listSpuByCategory(Long categoryId, Integer status) {
        LambdaQueryWrapper<Spu> wrapper = new LambdaQueryWrapper<Spu>()
                .eq(Spu::getCategoryId, categoryId)
                .eq(Spu::getDeleted, 0);
        if (status != null) {
            wrapper.eq(Spu::getStatus, status);
        }
        List<Spu> spus = spuMapper.selectList(wrapper);
        if (spus == null || spus.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> spuIds = spus.stream()
                .map(Spu::getId)
                .filter(id -> id != null)
                .toList();
        Map<Long, List<Sku>> skusBySpuId = new LinkedHashMap<>();
        if (!spuIds.isEmpty()) {
            List<Sku> skus = skuMapper.selectList(new LambdaQueryWrapper<Sku>()
                    .in(Sku::getSpuId, spuIds)
                    .eq(Sku::getDeleted, 0));
            for (Sku sku : skus) {
                if (sku.getSpuId() == null) {
                    continue;
                }
                skusBySpuId.computeIfAbsent(sku.getSpuId(), ignored -> new ArrayList<>()).add(sku);
            }
        }

        List<SpuDetailVO> result = new ArrayList<>(spus.size());
        for (Spu spu : spus) {
            List<Sku> skus = skusBySpuId.getOrDefault(spu.getId(), Collections.emptyList());
            result.add(toSpuDetail(spu, skus));
        }
        return result;
    }

    @Override
    public List<SkuDetailVO> listSkuByIds(List<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Sku> skus = skuMapper.selectList(new LambdaQueryWrapper<Sku>()
                .in(Sku::getId, skuIds)
                .eq(Sku::getDeleted, 0));
        if (skus == null || skus.isEmpty()) {
            return Collections.emptyList();
        }
        List<SkuDetailVO> result = new ArrayList<>(skus.size());
        for (Sku sku : skus) {
            result.add(toSkuDetail(sku));
        }
        return result;
    }

    @Override
    public Boolean updateSpuStatus(Long spuId, Integer status) {
        Spu spu = spuMapper.selectById(spuId);
        if (spu == null || spu.getDeleted() == 1) {
            throw new BusinessException("spu not found");
        }
        spu.setStatus(status);
        boolean updated = spuMapper.updateById(spu) > 0;
        if (updated) {
            productDetailCacheService.evict(spuId);
        }
        return updated;
    }

    private Spu toSpuEntity(SpuDTO dto) {
        Spu spu = new Spu();
        if (dto.getSpuId() != null) {
            spu.setId(dto.getSpuId());
        }
        spu.setSpuName(dto.getSpuName());
        spu.setSubtitle(dto.getSubtitle());
        spu.setCategoryId(dto.getCategoryId());
        spu.setBrandId(dto.getBrandId());
        spu.setMerchantId(dto.getMerchantId());
        spu.setStatus(dto.getStatus());
        spu.setDescription(dto.getDescription());
        spu.setMainImage(dto.getMainImage());
        return spu;
    }

    private Sku toSkuEntity(Long spuId, SkuDTO dto) {
        Sku sku = new Sku();
        if (dto.getSkuId() != null) {
            sku.setId(dto.getSkuId());
        }
        sku.setSpuId(spuId);
        sku.setSkuCode(dto.getSkuCode());
        sku.setSkuName(dto.getSkuName());
        sku.setSpecJson(dto.getSpecJson());
        sku.setSalePrice(dto.getSalePrice());
        sku.setMarketPrice(dto.getMarketPrice());
        sku.setCostPrice(dto.getCostPrice());
        sku.setStatus(dto.getStatus());
        sku.setImageUrl(dto.getImageUrl());
        return sku;
    }

    private SpuDetailVO toSpuDetail(Spu spu, List<Sku> skus) {
        SpuDetailVO vo = new SpuDetailVO();
        vo.setSpuId(spu.getId());
        vo.setSpuName(spu.getSpuName());
        vo.setSubtitle(spu.getSubtitle());
        vo.setCategoryId(spu.getCategoryId());
        vo.setBrandId(spu.getBrandId());
        vo.setMerchantId(spu.getMerchantId());
        vo.setStatus(spu.getStatus());
        vo.setDescription(spu.getDescription());
        vo.setMainImage(spu.getMainImage());
        vo.setCreatedAt(spu.getCreatedAt());
        vo.setUpdatedAt(spu.getUpdatedAt());

        List<SkuDetailVO> skuDetails = new ArrayList<>();
        for (Sku sku : skus) {
            skuDetails.add(toSkuDetail(sku));
        }
        vo.setSkus(skuDetails);
        return vo;
    }

    private SpuDetailVO loadSpuDetail(Long spuId) {
        Spu spu = spuMapper.selectById(spuId);
        if (spu == null || spu.getDeleted() == 1) {
            return null;
        }
        List<Sku> skus = skuMapper.selectList(new LambdaQueryWrapper<Sku>()
                .eq(Sku::getSpuId, spuId)
                .eq(Sku::getDeleted, 0));
        return toSpuDetail(spu, skus);
    }

    private SkuDetailVO toSkuDetail(Sku sku) {
        SkuDetailVO vo = new SkuDetailVO();
        vo.setSkuId(sku.getId());
        vo.setSpuId(sku.getSpuId());
        vo.setSkuCode(sku.getSkuCode());
        vo.setSkuName(sku.getSkuName());
        vo.setSpecJson(sku.getSpecJson());
        vo.setSalePrice(sku.getSalePrice());
        vo.setMarketPrice(sku.getMarketPrice());
        vo.setCostPrice(sku.getCostPrice());
        vo.setStatus(sku.getStatus());
        vo.setImageUrl(sku.getImageUrl());
        vo.setCreatedAt(sku.getCreatedAt());
        vo.setUpdatedAt(sku.getUpdatedAt());
        return vo;
    }
}
