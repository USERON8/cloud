package com.cloud.search.support;

import com.cloud.common.domain.vo.product.SkuDetailVO;
import com.cloud.common.domain.vo.product.SpuDetailVO;
import com.cloud.search.document.ProductDocument;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class ProductDocumentAssembler {

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

    return ProductDocument.builder()
        .id(String.valueOf(spu.getSpuId()))
        .productId(spu.getSpuId())
        .productName(spu.getSpuName())
        .productNameKeyword(spu.getSpuName())
        .price(price)
        .categoryId(spu.getCategoryId())
        .brandId(spu.getBrandId())
        .merchantId(spu.getMerchantId())
        .status(spu.getStatus())
        .description(spu.getDescription())
        .imageUrl(imageUrl)
        .sku(skuCode)
        .createdAt(spu.getCreatedAt())
        .updatedAt(spu.getUpdatedAt())
        .build();
  }
}
