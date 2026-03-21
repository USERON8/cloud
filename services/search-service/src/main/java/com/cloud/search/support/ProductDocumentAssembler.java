package com.cloud.search.support;

import com.cloud.common.domain.vo.product.SkuDetailVO;
import com.cloud.common.domain.vo.product.SpuDetailVO;
import com.cloud.search.document.ProductDocument;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class ProductDocumentAssembler {

  private static final long NEW_PRODUCT_DAYS = 30L;

  private ProductDocumentAssembler() {}

  public static ProductDocument toDocument(SpuDetailVO spu) {
    return toDocument(spu, null, null);
  }

  public static ProductDocument toDocument(
      SpuDetailVO spu, Integer stockQuantity, Integer salesCount) {
    if (spu == null) {
      return null;
    }
    List<SkuDetailVO> skus = spu.getSkus();
    Optional<SkuDetailVO> minPriceSku =
        skus == null
            ? Optional.empty()
            : skus.stream()
                .filter(sku -> sku.getSalePrice() != null)
                .min(Comparator.comparing(SkuDetailVO::getSalePrice));

    BigDecimal price = minPriceSku.map(SkuDetailVO::getSalePrice).orElse(null);
    String skuCode = minPriceSku.map(SkuDetailVO::getSkuCode).orElse(null);
    String imageUrl = minPriceSku.map(SkuDetailVO::getImageUrl).orElse(spu.getMainImage());
    String detailImages = resolveDetailImages(spu);
    int normalizedSalesCount = normalizeCount(salesCount);
    boolean isNew =
        spu.getCreatedAt() != null
            && spu.getCreatedAt().isAfter(LocalDateTime.now().minusDays(NEW_PRODUCT_DAYS));
    double hotScore = calculateHotScore(spu, isNew, normalizedSalesCount);
    double searchWeight = calculateSearchWeight(hotScore, spu, normalizedSalesCount);

    return ProductDocument.builder()
        .id(String.valueOf(spu.getSpuId()))
        .productId(spu.getSpuId())
        .shopId(spu.getMerchantId())
        .shopName(spu.getShopName())
        .productName(spu.getSpuName())
        .productNameKeyword(spu.getSpuName())
        .price(price)
        .stockQuantity(normalizeCount(stockQuantity))
        .categoryId(spu.getCategoryId())
        .categoryName(spu.getCategoryName())
        .categoryNameKeyword(spu.getCategoryName())
        .brandId(spu.getBrandId())
        .brandName(spu.getBrandName())
        .brandNameKeyword(spu.getBrandName())
        .merchantId(spu.getMerchantId())
        .status(spu.getStatus())
        .description(spu.getDescription())
        .imageUrl(imageUrl)
        .detailImages(detailImages)
        .tags(resolveTags(spu.getTags()))
        .sku(skuCode)
        .salesCount(normalizedSalesCount)
        .rating(spu.getRating())
        .reviewCount(spu.getReviewCount())
        .recommended(Boolean.TRUE.equals(spu.getRecommended()))
        .isNew(isNew)
        .isHot(Boolean.TRUE.equals(spu.getIsHot()))
        .createdAt(spu.getCreatedAt())
        .updatedAt(spu.getUpdatedAt())
        .hotScore(hotScore)
        .searchWeight(searchWeight)
        .build();
  }

  private static Integer normalizeCount(Integer value) {
    return value == null ? 0 : Math.max(value, 0);
  }

  private static String resolveDetailImages(SpuDetailVO spu) {
    Set<String> imageUrls = new LinkedHashSet<>();
    if (spu.getMainImage() != null && !spu.getMainImage().isBlank()) {
      imageUrls.add(spu.getMainImage());
    }
    if (spu.getSkus() != null) {
      for (SkuDetailVO sku : spu.getSkus()) {
        if (sku != null && sku.getImageUrl() != null && !sku.getImageUrl().isBlank()) {
          imageUrls.add(sku.getImageUrl());
        }
      }
    }
    return imageUrls.isEmpty() ? null : String.join(",", imageUrls);
  }

  private static List<String> resolveTags(String rawTags) {
    if (rawTags == null || rawTags.isBlank()) {
      return List.of();
    }
    return Arrays.stream(rawTags.split(","))
        .map(String::trim)
        .filter(tag -> !tag.isBlank())
        .distinct()
        .toList();
  }

  private static double calculateHotScore(SpuDetailVO spu, boolean isNew, int salesCount) {
    double score = 0D;
    if (Boolean.TRUE.equals(spu.getRecommended())) {
      score += 30D;
    }
    if (Boolean.TRUE.equals(spu.getIsHot())) {
      score += 40D;
    }
    if (isNew) {
      score += 15D;
    }
    if (spu.getReviewCount() != null) {
      score += Math.min(spu.getReviewCount(), 200) * 2D;
    }
    if (spu.getRating() != null) {
      score += Math.min(spu.getRating().doubleValue(), 5D) * 20D;
    }
    if (salesCount > 0) {
      score += Math.log1p(salesCount) * 25D;
    }
    return score;
  }

  private static double calculateSearchWeight(double hotScore, SpuDetailVO spu, int salesCount) {
    double weight = hotScore;
    if (spu.getSpuName() != null && !spu.getSpuName().isBlank()) {
      weight += 10D;
    }
    if (salesCount > 0) {
      weight += Math.min(salesCount, 500) * 0.1D;
    }
    return weight;
  }
}
