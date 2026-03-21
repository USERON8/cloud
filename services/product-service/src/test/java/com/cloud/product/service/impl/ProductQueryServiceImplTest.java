package com.cloud.product.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.result.PageResult;
import com.cloud.product.dto.ProductItemDTO;
import com.cloud.product.mapper.SkuMapper;
import com.cloud.product.mapper.SpuMapper;
import com.cloud.product.module.entity.Sku;
import com.cloud.product.module.entity.Spu;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductQueryServiceImplTest {

  @Mock private SpuMapper spuMapper;

  @Mock private SkuMapper skuMapper;

  private ProductQueryServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new ProductQueryServiceImpl(spuMapper, skuMapper);
  }

  @Test
  void listProductsShouldDefaultToActiveSpuAndActiveSku() {
    Spu spu = new Spu();
    spu.setId(100L);
    spu.setMerchantId(200L);
    spu.setSpuName("Cloud Phone");
    spu.setCategoryId(300L);
    spu.setBrandId(400L);
    spu.setStatus(1);

    Sku inactiveSku = new Sku();
    inactiveSku.setSpuId(100L);
    inactiveSku.setSalePrice(BigDecimal.valueOf(29.9));
    inactiveSku.setStatus(0);

    Sku activeSku = new Sku();
    activeSku.setSpuId(100L);
    activeSku.setSalePrice(BigDecimal.valueOf(19.9));
    activeSku.setImageUrl("active.png");
    activeSku.setStatus(1);

    Page<Spu> page = new Page<>(1, 20);
    page.setRecords(List.of(spu));
    page.setTotal(1L);

    when(spuMapper.selectPage(any(Page.class), any())).thenReturn(page);
    when(skuMapper.selectList(any())).thenReturn(List.of(inactiveSku, activeSku));

    PageResult<ProductItemDTO> result = service.listProducts(1, 20, null, null, null, null);

    assertThat(result.getRecords()).hasSize(1);
    assertThat(result.getRecords().get(0).getPrice()).isEqualByComparingTo("19.9");
    assertThat(result.getRecords().get(0).getImageUrl()).isEqualTo("active.png");
  }

  @Test
  void searchProductsShouldSkipSpuWithoutActiveSku() {
    Spu spu = new Spu();
    spu.setId(101L);
    spu.setMerchantId(201L);
    spu.setSpuName("Hidden Phone");
    spu.setStatus(1);
    spu.setMainImage("hidden.png");

    when(spuMapper.selectList(any())).thenReturn(List.of(spu));
    when(skuMapper.selectList(any())).thenReturn(List.of());

    List<ProductItemDTO> result = service.searchProducts("phone", 10);

    assertThat(result).isEmpty();
  }
}
