package com.cloud.product.service.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.domain.vo.product.SkuDetailVO;
import com.cloud.common.domain.vo.product.SpuDetailVO;
import com.cloud.common.exception.BusinessException;
import com.cloud.product.mapper.BrandMapper;
import com.cloud.product.mapper.CategoryMapper;
import com.cloud.product.mapper.ProductReviewMapper;
import com.cloud.product.mapper.SkuMapper;
import com.cloud.product.mapper.SpuMapper;
import com.cloud.product.messaging.ProductSyncMessageProducer;
import com.cloud.product.module.entity.Brand;
import com.cloud.product.module.entity.Category;
import com.cloud.product.module.entity.ProductReview;
import com.cloud.product.module.entity.Sku;
import com.cloud.product.module.entity.Spu;
import com.cloud.product.service.impl.ProductCatalogServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductCatalogServiceImplTest {

  @Mock private SpuMapper spuMapper;

  @Mock private SkuMapper skuMapper;

  @Mock private CategoryMapper categoryMapper;

  @Mock private BrandMapper brandMapper;

  @Mock private ProductReviewMapper productReviewMapper;

  @Mock private ProductDetailCacheService productDetailCacheService;

  @Mock private ProductSyncMessageProducer productSyncMessageProducer;

  private ProductCatalogServiceImpl service;

  @BeforeEach
  void setUp() {
    service =
        new ProductCatalogServiceImpl(
            spuMapper,
            skuMapper,
            categoryMapper,
            brandMapper,
            productReviewMapper,
            productDetailCacheService,
            productSyncMessageProducer);
  }

  @Test
  void listSkuByIdsShouldReturnMappedResult() {
    Sku sku = new Sku();
    sku.setId(11L);
    sku.setSpuId(22L);
    sku.setSkuCode("SKU-11");
    sku.setSkuName("demo");
    sku.setSalePrice(BigDecimal.valueOf(19.9));
    sku.setStatus(1);
    when(skuMapper.selectList(any())).thenReturn(List.of(sku));

    List<SkuDetailVO> result = service.listSkuByIds(List.of(11L));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getSkuId()).isEqualTo(11L);
    assertThat(result.get(0).getSpuId()).isEqualTo(22L);
    assertThat(result.get(0).getSkuCode()).isEqualTo("SKU-11");
  }

  @Test
  void updateSpuStatusShouldThrowWhenSpuNotFound() {
    when(spuMapper.selectById(99L)).thenReturn(null);

    assertThatThrownBy(() -> service.updateSpuStatus(99L, 1))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("spu not found");
  }

  @Test
  void listSpuByPageShouldPopulateSearchMetadata() {
    Spu spu = new Spu();
    spu.setId(1001L);
    spu.setSpuName("Cloud Phone");
    spu.setCategoryId(2001L);
    spu.setBrandId(3001L);
    spu.setMerchantId(4001L);
    spu.setStatus(1);

    Category category = new Category();
    category.setId(2001L);
    category.setName("Phone");

    Brand brand = new Brand();
    brand.setId(3001L);
    brand.setBrandName("Cloud");
    brand.setIsRecommended(1);
    brand.setIsHot(1);

    ProductReview review = new ProductReview();
    review.setSpuId(1001L);
    review.setRating(5);
    review.setTags("fast,smooth");
    review.setAuditStatus("APPROVED");
    review.setIsVisible(1);

    Page<Spu> page = new Page<>(1, 20);
    page.setRecords(List.of(spu));

    when(spuMapper.selectPage(any(Page.class), any())).thenReturn(page);
    when(skuMapper.selectList(any())).thenReturn(List.of());
    when(categoryMapper.selectBatchIds(List.of(2001L))).thenReturn(List.of(category));
    when(brandMapper.selectBatchIds(List.of(3001L))).thenReturn(List.of(brand));
    when(productReviewMapper.selectList(any())).thenReturn(List.of(review));

    List<SpuDetailVO> result = service.listSpuByPage(1, 20, 1);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getCategoryName()).isEqualTo("Phone");
    assertThat(result.get(0).getBrandName()).isEqualTo("Cloud");
    assertThat(result.get(0).getTags()).isEqualTo("fast,smooth");
    assertThat(result.get(0).getRating()).isEqualByComparingTo("5.00");
    assertThat(result.get(0).getReviewCount()).isEqualTo(1);
    assertThat(result.get(0).getRecommended()).isTrue();
    assertThat(result.get(0).getIsHot()).isTrue();
  }
}
