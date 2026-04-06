package com.cloud.product.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.result.PageResult;
import com.cloud.product.converter.ProductItemConverter;
import com.cloud.product.dto.ProductItemDTO;
import com.cloud.product.mapper.SkuMapper;
import com.cloud.product.mapper.SpuMapper;
import com.cloud.product.module.entity.Sku;
import com.cloud.product.module.entity.Spu;
import com.cloud.product.service.ProductQueryService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductQueryServiceImpl implements ProductQueryService {

  private static final Integer ACTIVE_STATUS = 1;

  private final ProductItemConverter productItemConverter;
  private final SpuMapper spuMapper;
  private final SkuMapper skuMapper;

  @Value("${product.config.page.max-size:100}")
  private Integer maxListSize;

  @Override
  public PageResult<ProductItemDTO> listProducts(
      Integer page,
      Integer size,
      String name,
      Long categoryId,
      Long brandId,
      Long merchantId,
      Integer status) {
    int safePage = page == null || page < 1 ? 1 : page;
    int safeSize = size == null || size <= 0 ? 20 : size;
    int maxSize = maxListSize == null || maxListSize <= 0 ? 100 : maxListSize;
    safeSize = Math.min(safeSize, maxSize);

    LambdaQueryWrapper<Spu> wrapper = new LambdaQueryWrapper<Spu>().eq(Spu::getDeleted, 0);
    if (StrUtil.isNotBlank(name)) {
      wrapper.like(Spu::getSpuName, name.trim());
    }
    if (categoryId != null) {
      wrapper.eq(Spu::getCategoryId, categoryId);
    }
    if (brandId != null) {
      wrapper.eq(Spu::getBrandId, brandId);
    }
    if (merchantId != null) {
      wrapper.eq(Spu::getMerchantId, merchantId);
    }
    if (status != null) {
      wrapper.eq(Spu::getStatus, status);
    }
    wrapper.orderByDesc(Spu::getId);

    Page<Spu> pageData = new Page<>(safePage, safeSize);
    Page<Spu> result = spuMapper.selectPage(pageData, wrapper);
    List<ProductItemDTO> items =
        buildProductItems(result == null ? Collections.emptyList() : result.getRecords());
    long total = result == null ? 0L : result.getTotal();
    return PageResult.of((long) safePage, (long) safeSize, total, items);
  }

  @Override
  public List<ProductItemDTO> searchProducts(String name, Integer size) {
    if (StrUtil.isBlank(name)) {
      return List.of();
    }
    int safeSize = size == null || size <= 0 ? 10 : size;
    int maxSize = maxListSize == null || maxListSize <= 0 ? 100 : maxListSize;
    safeSize = Math.min(safeSize, maxSize);

    LambdaQueryWrapper<Spu> wrapper =
        new LambdaQueryWrapper<Spu>()
            .eq(Spu::getDeleted, 0)
            .eq(Spu::getStatus, ACTIVE_STATUS)
            .like(Spu::getSpuName, name.trim())
            .orderByDesc(Spu::getId)
            .last("LIMIT " + safeSize);

    List<Spu> spus = spuMapper.selectList(wrapper);
    return buildProductItems(spus);
  }

  private List<ProductItemDTO> buildProductItems(List<Spu> spus) {
    if (spus == null || spus.isEmpty()) {
      return List.of();
    }
    List<Long> spuIds = spus.stream().map(Spu::getId).filter(id -> id != null).toList();
    Map<Long, Sku> minSkuBySpu = new HashMap<>();
    if (!spuIds.isEmpty()) {
      List<Sku> skus =
          skuMapper.selectList(
              new LambdaQueryWrapper<Sku>()
                  .in(Sku::getSpuId, spuIds)
                  .eq(Sku::getDeleted, 0)
                  .eq(Sku::getStatus, ACTIVE_STATUS));
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
      Sku sku = minSkuBySpu.get(spu.getId());
      if (sku == null) {
        continue;
      }
      ProductItemDTO item = productItemConverter.toDTO(spu, sku);
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
