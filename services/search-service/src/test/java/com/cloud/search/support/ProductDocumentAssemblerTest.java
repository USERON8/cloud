package com.cloud.search.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.cloud.common.domain.vo.product.SkuDetailVO;
import com.cloud.common.domain.vo.product.SpuDetailVO;
import com.cloud.search.document.ProductDocument;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProductDocumentAssemblerTest {

  @Test
  void toDocumentShouldPopulateSearchFields() {
    SkuDetailVO lowPriceSku = new SkuDetailVO();
    lowPriceSku.setSkuCode("SKU-LOW");
    lowPriceSku.setSalePrice(new BigDecimal("99.00"));
    lowPriceSku.setImageUrl("https://img.example.com/sku-low.jpg");

    SkuDetailVO highPriceSku = new SkuDetailVO();
    highPriceSku.setSkuCode("SKU-HIGH");
    highPriceSku.setSalePrice(new BigDecimal("129.00"));

    SpuDetailVO spu = new SpuDetailVO();
    spu.setSpuId(1001L);
    spu.setSpuName("Cloud Phone");
    spu.setCategoryId(2001L);
    spu.setCategoryName("Phone");
    spu.setBrandId(3001L);
    spu.setBrandName("Cloud");
    spu.setMerchantId(4001L);
    spu.setStatus(1);
    spu.setDescription("flagship");
    spu.setMainImage("https://img.example.com/spu.jpg");
    spu.setRecommended(true);
    spu.setIsHot(true);
    spu.setCreatedAt(LocalDateTime.now().minusDays(7));
    spu.setUpdatedAt(LocalDateTime.now());
    spu.setSkus(List.of(highPriceSku, lowPriceSku));

    ProductDocument document = ProductDocumentAssembler.toDocument(spu);

    assertThat(document.getShopId()).isEqualTo(4001L);
    assertThat(document.getCategoryName()).isEqualTo("Phone");
    assertThat(document.getBrandName()).isEqualTo("Cloud");
    assertThat(document.getRecommended()).isTrue();
    assertThat(document.getIsHot()).isTrue();
    assertThat(document.getIsNew()).isTrue();
    assertThat(document.getPrice()).isEqualByComparingTo("99.00");
    assertThat(document.getSku()).isEqualTo("SKU-LOW");
    assertThat(document.getImageUrl()).isEqualTo("https://img.example.com/sku-low.jpg");
  }
}
