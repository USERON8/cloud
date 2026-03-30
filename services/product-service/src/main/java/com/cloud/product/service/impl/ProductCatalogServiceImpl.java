package com.cloud.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.domain.dto.product.SkuDTO;
import com.cloud.common.domain.dto.product.SpuCreateRequestDTO;
import com.cloud.common.domain.dto.product.SpuDTO;
import com.cloud.common.domain.vo.product.SkuDetailVO;
import com.cloud.common.domain.vo.product.SpuDetailVO;
import com.cloud.common.exception.BusinessException;
import com.cloud.product.mapper.BrandMapper;
import com.cloud.product.mapper.CategoryMapper;
import com.cloud.product.mapper.ProductReviewMapper;
import com.cloud.product.mapper.ShopMapper;
import com.cloud.product.mapper.SkuMapper;
import com.cloud.product.mapper.SpuMapper;
import com.cloud.product.messaging.ProductSyncMessageProducer;
import com.cloud.product.module.entity.Brand;
import com.cloud.product.module.entity.Category;
import com.cloud.product.module.entity.ProductReview;
import com.cloud.product.module.entity.Shop;
import com.cloud.product.module.entity.Sku;
import com.cloud.product.module.entity.Spu;
import com.cloud.product.service.ProductCatalogService;
import com.cloud.product.service.support.ProductDetailCacheService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductCatalogServiceImpl implements ProductCatalogService {

  private final SpuMapper spuMapper;
  private final SkuMapper skuMapper;
  private final CategoryMapper categoryMapper;
  private final BrandMapper brandMapper;
  private final ShopMapper shopMapper;
  private final ProductReviewMapper productReviewMapper;
  private final ProductDetailCacheService productDetailCacheService;
  private final ProductSyncMessageProducer productSyncMessageProducer;

  @Value("${product.config.page.max-size:100}")
  private Integer maxListSize;

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
    productSyncMessageProducer.sendUpsert(spu.getId());
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
    existing.setMainImageFile(spuDTO.getMainImageFile());
    spuMapper.updateById(existing);

    List<Sku> oldSkus =
        skuMapper.selectList(
            new LambdaQueryWrapper<Sku>().eq(Sku::getSpuId, spuId).eq(Sku::getDeleted, 0));
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
        existingSku.setImageFile(skuDTO.getImageFile());
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
    productDetailCacheService.evictAfterCommit(spuId);
    productSyncMessageProducer.sendUpsert(spuId);
    return true;
  }

  @Override
  public SpuDetailVO getSpuById(Long spuId) {
    return productDetailCacheService.getOrLoad(spuId, () -> loadSpuDetail(spuId));
  }

  @Override
  public List<SpuDetailVO> listSpuByCategory(Long categoryId, Integer status) {
    int effectiveMax = (maxListSize == null || maxListSize <= 0) ? 100 : maxListSize;
    LambdaQueryWrapper<Spu> wrapper =
        new LambdaQueryWrapper<Spu>().eq(Spu::getCategoryId, categoryId).eq(Spu::getDeleted, 0);
    if (status != null) {
      wrapper.eq(Spu::getStatus, status);
    }
    wrapper.last("LIMIT " + effectiveMax);
    List<Spu> spus = spuMapper.selectList(wrapper);
    return buildSpuDetails(spus);
  }

  @Override
  public List<SpuDetailVO> listSpuByPage(Integer page, Integer size, Integer status) {
    int safePage = page == null || page < 1 ? 1 : page;
    int safeSize = size == null || size <= 0 ? 100 : size;
    int effectiveMax = (maxListSize == null || maxListSize <= 0) ? 100 : maxListSize;
    safeSize = Math.min(safeSize, effectiveMax);

    LambdaQueryWrapper<Spu> wrapper = new LambdaQueryWrapper<Spu>().eq(Spu::getDeleted, 0);
    if (status != null) {
      wrapper.eq(Spu::getStatus, status);
    }
    wrapper.orderByAsc(Spu::getId);

    Page<Spu> pageData = new Page<>(safePage, safeSize);
    Page<Spu> result = spuMapper.selectPage(pageData, wrapper);
    return buildSpuDetails(result == null ? Collections.emptyList() : result.getRecords());
  }

  @Override
  public List<SkuDetailVO> listSkuByIds(List<Long> skuIds) {
    if (skuIds == null || skuIds.isEmpty()) {
      return Collections.emptyList();
    }
    List<Sku> skus =
        skuMapper.selectList(
            new LambdaQueryWrapper<Sku>().in(Sku::getId, skuIds).eq(Sku::getDeleted, 0));
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
  public Map<Long, Long> mapSpuIdsBySkuIds(List<Long> skuIds) {
    if (skuIds == null || skuIds.isEmpty()) {
      return Map.of();
    }
    List<Long> safeSkuIds = skuIds.stream().filter(id -> id != null).distinct().toList();
    if (safeSkuIds.isEmpty()) {
      return Map.of();
    }
    List<Sku> skus =
        skuMapper.selectList(
            new LambdaQueryWrapper<Sku>().in(Sku::getId, safeSkuIds).eq(Sku::getDeleted, 0));
    if (skus == null || skus.isEmpty()) {
      return Map.of();
    }
    return skus.stream()
        .filter(sku -> sku.getId() != null && sku.getSpuId() != null)
        .collect(
            java.util.stream.Collectors.toMap(
                Sku::getId, Sku::getSpuId, (left, right) -> left, LinkedHashMap::new));
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
      productDetailCacheService.evictAfterCommit(spuId);
      productSyncMessageProducer.sendUpsert(spuId);
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
    spu.setMainImageFile(dto.getMainImageFile());
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
    sku.setImageFile(dto.getImageFile());
    return sku;
  }

  private SpuDetailVO toSpuDetail(
      Spu spu,
      List<Sku> skus,
      Map<Long, Category> categoryById,
      Map<Long, Brand> brandById,
      Map<Long, Shop> shopByMerchantId,
      Map<Long, ReviewAggregate> reviewAggregateBySpuId) {
    SpuDetailVO vo = new SpuDetailVO();
    vo.setSpuId(spu.getId());
    vo.setSpuName(spu.getSpuName());
    vo.setSubtitle(spu.getSubtitle());
    vo.setCategoryId(spu.getCategoryId());
    vo.setCategoryName(resolveCategoryName(spu.getCategoryId(), categoryById));
    vo.setBrandId(spu.getBrandId());
    vo.setBrandName(resolveBrandName(spu.getBrandId(), brandById));
    vo.setMerchantId(spu.getMerchantId());
    vo.setShopName(resolveShopName(spu.getMerchantId(), shopByMerchantId));
    vo.setStatus(spu.getStatus());
    vo.setDescription(spu.getDescription());
    vo.setMainImage(spu.getMainImage());
    vo.setMainImageFile(spu.getMainImageFile());
    ReviewAggregate reviewAggregate =
        reviewAggregateBySpuId == null ? null : reviewAggregateBySpuId.get(spu.getId());
    vo.setTags(reviewAggregate == null ? null : reviewAggregate.tags());
    vo.setRating(reviewAggregate == null ? null : reviewAggregate.rating());
    vo.setReviewCount(reviewAggregate == null ? 0 : reviewAggregate.reviewCount());
    vo.setRecommended(resolveBrandFlag(spu.getBrandId(), brandById, Brand::getIsRecommended));
    vo.setIsHot(resolveBrandFlag(spu.getBrandId(), brandById, Brand::getIsHot));
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
    List<Sku> skus =
        skuMapper.selectList(
            new LambdaQueryWrapper<Sku>().eq(Sku::getSpuId, spuId).eq(Sku::getDeleted, 0));
    Map<Long, Category> categoryById = loadCategoryMap(List.of(spu));
    Map<Long, Brand> brandById = loadBrandMap(List.of(spu));
    Map<Long, Shop> shopByMerchantId = loadShopMap(List.of(spu));
    Map<Long, ReviewAggregate> reviewAggregateBySpuId = loadReviewAggregateMap(List.of(spu));
    return toSpuDetail(
        spu, skus, categoryById, brandById, shopByMerchantId, reviewAggregateBySpuId);
  }

  private List<SpuDetailVO> buildSpuDetails(List<Spu> spus) {
    if (spus == null || spus.isEmpty()) {
      return Collections.emptyList();
    }

    Map<Long, Category> categoryById = loadCategoryMap(spus);
    Map<Long, Brand> brandById = loadBrandMap(spus);
    Map<Long, Shop> shopByMerchantId = loadShopMap(spus);
    Map<Long, ReviewAggregate> reviewAggregateBySpuId = loadReviewAggregateMap(spus);
    List<Long> spuIds = spus.stream().map(Spu::getId).filter(id -> id != null).toList();
    Map<Long, List<Sku>> skusBySpuId = new LinkedHashMap<>();
    if (!spuIds.isEmpty()) {
      List<Sku> skus =
          skuMapper.selectList(
              new LambdaQueryWrapper<Sku>().in(Sku::getSpuId, spuIds).eq(Sku::getDeleted, 0));
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
      result.add(
          toSpuDetail(
              spu, skus, categoryById, brandById, shopByMerchantId, reviewAggregateBySpuId));
    }
    return result;
  }

  private Map<Long, Category> loadCategoryMap(List<Spu> spus) {
    List<Long> categoryIds =
        spus.stream().map(Spu::getCategoryId).filter(id -> id != null).distinct().toList();
    if (categoryIds.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<Long, Category> categoryById = new HashMap<>(categoryIds.size());
    List<Category> categories = categoryMapper.selectBatchIds(categoryIds);
    if (categories == null || categories.isEmpty()) {
      return categoryById;
    }
    for (Category category : categories) {
      if (category != null && category.getId() != null) {
        categoryById.put(category.getId(), category);
      }
    }
    return categoryById;
  }

  private Map<Long, Brand> loadBrandMap(List<Spu> spus) {
    List<Long> brandIds =
        spus.stream().map(Spu::getBrandId).filter(id -> id != null).distinct().toList();
    if (brandIds.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<Long, Brand> brandById = new HashMap<>(brandIds.size());
    List<Brand> brands = brandMapper.selectBatchIds(brandIds);
    if (brands == null || brands.isEmpty()) {
      return brandById;
    }
    for (Brand brand : brands) {
      if (brand != null && brand.getId() != null) {
        brandById.put(brand.getId(), brand);
      }
    }
    return brandById;
  }

  private Map<Long, Shop> loadShopMap(List<Spu> spus) {
    List<Long> merchantIds =
        spus.stream().map(Spu::getMerchantId).filter(id -> id != null).distinct().toList();
    if (merchantIds.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<Long, Shop> shopByMerchantId = new HashMap<>(merchantIds.size());
    List<Shop> shops =
        shopMapper.selectList(
            new LambdaQueryWrapper<Shop>()
                .in(Shop::getMerchantId, merchantIds)
                .eq(Shop::getDeleted, 0));
    if (shops == null || shops.isEmpty()) {
      return shopByMerchantId;
    }
    for (Shop shop : shops) {
      if (shop != null && shop.getMerchantId() != null) {
        shopByMerchantId.putIfAbsent(shop.getMerchantId(), shop);
      }
    }
    return shopByMerchantId;
  }

  private String resolveCategoryName(Long categoryId, Map<Long, Category> categoryById) {
    if (categoryId == null || categoryById == null || categoryById.isEmpty()) {
      return null;
    }
    Category category = categoryById.get(categoryId);
    return category == null ? null : category.getName();
  }

  private String resolveBrandName(Long brandId, Map<Long, Brand> brandById) {
    if (brandId == null || brandById == null || brandById.isEmpty()) {
      return null;
    }
    Brand brand = brandById.get(brandId);
    return brand == null ? null : brand.getBrandName();
  }

  private String resolveShopName(Long merchantId, Map<Long, Shop> shopByMerchantId) {
    if (merchantId == null || shopByMerchantId == null || shopByMerchantId.isEmpty()) {
      return null;
    }
    Shop shop = shopByMerchantId.get(merchantId);
    return shop == null ? null : shop.getShopName();
  }

  private Boolean resolveBrandFlag(
      Long brandId,
      Map<Long, Brand> brandById,
      java.util.function.Function<Brand, Integer> extractor) {
    if (brandId == null || brandById == null || brandById.isEmpty()) {
      return Boolean.FALSE;
    }
    Brand brand = brandById.get(brandId);
    if (brand == null) {
      return Boolean.FALSE;
    }
    Integer value = extractor.apply(brand);
    return value != null && value > 0;
  }

  private Map<Long, ReviewAggregate> loadReviewAggregateMap(List<Spu> spus) {
    List<Long> spuIds = spus.stream().map(Spu::getId).filter(id -> id != null).distinct().toList();
    if (spuIds.isEmpty()) {
      return Collections.emptyMap();
    }

    List<ProductReview> reviews =
        productReviewMapper.selectList(
            new LambdaQueryWrapper<ProductReview>()
                .in(ProductReview::getSpuId, spuIds)
                .eq(ProductReview::getDeleted, 0)
                .eq(ProductReview::getAuditStatus, "APPROVED")
                .eq(ProductReview::getIsVisible, 1));
    if (reviews == null || reviews.isEmpty()) {
      return Collections.emptyMap();
    }

    Map<Long, ReviewAggregateAccumulator> accumulatorBySpuId = new HashMap<>();
    for (ProductReview review : reviews) {
      if (review == null || review.getSpuId() == null) {
        continue;
      }
      accumulatorBySpuId
          .computeIfAbsent(review.getSpuId(), ignored -> new ReviewAggregateAccumulator())
          .accumulate(review);
    }

    Map<Long, ReviewAggregate> reviewAggregateBySpuId = new HashMap<>(accumulatorBySpuId.size());
    for (Map.Entry<Long, ReviewAggregateAccumulator> entry : accumulatorBySpuId.entrySet()) {
      reviewAggregateBySpuId.put(entry.getKey(), entry.getValue().toAggregate());
    }
    return reviewAggregateBySpuId;
  }

  private record ReviewAggregate(BigDecimal rating, Integer reviewCount, String tags) {}

  private static final class ReviewAggregateAccumulator {

    private final Set<String> tagSet = new LinkedHashSet<>();
    private int reviewCount;
    private int ratingSum;

    private void accumulate(ProductReview review) {
      reviewCount++;
      if (review.getRating() != null) {
        ratingSum += review.getRating();
      }
      if (review.getTags() == null || review.getTags().isBlank()) {
        return;
      }
      String[] tags = review.getTags().split(",");
      for (String tag : tags) {
        if (tag != null && !tag.isBlank()) {
          tagSet.add(tag.trim());
        }
      }
    }

    private ReviewAggregate toAggregate() {
      BigDecimal rating =
          reviewCount == 0
              ? null
              : BigDecimal.valueOf(ratingSum)
                  .divide(BigDecimal.valueOf(reviewCount), 2, RoundingMode.HALF_UP);
      String tags = tagSet.isEmpty() ? null : String.join(",", tagSet);
      return new ReviewAggregate(rating, reviewCount, tags);
    }
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
    vo.setImageFile(sku.getImageFile());
    vo.setCreatedAt(sku.getCreatedAt());
    vo.setUpdatedAt(sku.getUpdatedAt());
    return vo;
  }
}
