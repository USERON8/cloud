package com.cloud.search.support;

import com.cloud.common.domain.vo.product.SkuDetailVO;
import com.cloud.common.domain.vo.product.SpuDetailVO;
import com.cloud.search.document.ProductDocument;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class ProductDocumentAssembler {

  private static final long NEW_PRODUCT_DAYS = 30L;

  private ProductDocumentAssembler() {}

  public static ProductDocument toDocument(SpuDetailVO spu) {
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
    boolean isNew =
        spu.getCreatedAt() != null
            && spu.getCreatedAt().isAfter(LocalDateTime.now().minusDays(NEW_PRODUCT_DAYS));

    return ProductDocument.builder()
        .id(String.valueOf(spu.getSpuId()))
        .productId(spu.getSpuId())
        .shopId(spu.getMerchantId())
        .productName(spu.getSpuName())
        .productNameKeyword(spu.getSpuName())
        .price(price)
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
        .sku(skuCode)
        .recommended(Boolean.TRUE.equals(spu.getRecommended()))
        .isNew(isNew)
        .isHot(Boolean.TRUE.equals(spu.getIsHot()))
        .createdAt(spu.getCreatedAt())
        .updatedAt(spu.getUpdatedAt())
        .build();
  }
}
