package com.cloud.product.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.domain.dto.product.ProductSearchItemDTO;
import com.cloud.common.result.PageResult;
import com.cloud.product.converter.ProductItemConverter;
import com.cloud.product.dto.ProductItemDTO;
import com.cloud.product.mapper.SkuMapper;
import com.cloud.product.mapper.SpuMapper;
import com.cloud.product.module.entity.Sku;
import com.cloud.product.module.entity.Spu;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class ProductQueryServiceImplTest {

  @Mock private ProductItemConverter productItemConverter;
  @Mock private SpuMapper spuMapper;
  @Mock private SkuMapper skuMapper;

  @InjectMocks private ProductQueryServiceImpl productQueryService;

  @Test
  void listProductsKeepsOnlyLowestPricedActiveSku() {
    Spu spu = new Spu();
    spu.setId(50001L);
    spu.setMerchantId(30001L);
    spu.setStatus(1);
    spu.setMainImage("spu-cover");

    Page<Spu> page = new Page<>(1, 20);
    page.setRecords(List.of(spu));
    page.setTotal(1L);
    when(spuMapper.selectPage(any(Page.class), any())).thenReturn(page);

    Sku inactiveSku = new Sku();
    inactiveSku.setId(51001L);
    inactiveSku.setSpuId(50001L);
    inactiveSku.setStatus(0);
    inactiveSku.setSalePrice(new BigDecimal("99.00"));

    Sku expensiveActiveSku = new Sku();
    expensiveActiveSku.setId(51002L);
    expensiveActiveSku.setSpuId(50001L);
    expensiveActiveSku.setStatus(1);
    expensiveActiveSku.setSalePrice(new BigDecimal("299.00"));

    Sku cheapestActiveSku = new Sku();
    cheapestActiveSku.setId(51003L);
    cheapestActiveSku.setSpuId(50001L);
    cheapestActiveSku.setStatus(1);
    cheapestActiveSku.setSalePrice(new BigDecimal("199.00"));

    when(skuMapper.selectActiveBySpuIds(List.of(50001L)))
        .thenReturn(List.of(inactiveSku, expensiveActiveSku, cheapestActiveSku));

    ProductItemDTO dto = new ProductItemDTO();
    dto.setId(50001L);
    dto.setPrice(new BigDecimal("199.00"));
    when(productItemConverter.toDTO(spu, cheapestActiveSku)).thenReturn(dto);

    PageResult<ProductItemDTO> result =
        productQueryService.listProducts(1, 20, null, null, null, null, 1);

    assertEquals(1L, result.getTotal());
    assertEquals(1, result.getRecords().size());
    assertEquals(new BigDecimal("199.00"), result.getRecords().get(0).getPrice());
    assertEquals("spu-cover", result.getRecords().get(0).getImageUrl());
    verify(skuMapper).selectActiveBySpuIds(List.of(50001L));
    verify(productItemConverter).toDTO(spu, cheapestActiveSku);
    verify(productItemConverter, never()).toDTO(spu, inactiveSku);
    verify(productItemConverter, never()).toDTO(spu, expensiveActiveSku);
    assertNotNull(result.getRecords().get(0));
  }

  @Test
  void listProductsNormalizesPageSizeToConfiguredMaximum() {
    ReflectionTestUtils.setField(productQueryService, "maxListSize", 5);
    Page<Spu> page = new Page<>(1, 5);
    page.setRecords(List.of());
    page.setTotal(0L);
    when(spuMapper.selectPage(any(Page.class), any())).thenReturn(page);

    PageResult<ProductItemDTO> result =
        productQueryService.listProducts(0, 99, " phone ", null, null, null, null);

    assertEquals(1L, result.getCurrent());
    assertEquals(5L, result.getSize());
    verify(spuMapper)
        .selectPage(
            argThat(pageArg -> pageArg.getCurrent() == 1L && pageArg.getSize() == 5L), any());
  }

  @Test
  void searchProductsReturnsEmptyResultForBlankKeywordWithoutQueryingDatabase() {
    assertEquals(List.of(), productQueryService.searchProducts("   ", 10));

    verifyNoInteractions(spuMapper, skuMapper, productItemConverter);
  }

  @Test
  void searchProductItemsUsesLowestActiveSkuAndFallsBackToSpuImage() {
    Spu spu = spu(60001L, "Phone", "spu-image");
    Sku expensiveSku = sku(61001L, 60001L, 1, "399.00");
    Sku cheapSku = sku(61002L, 60001L, 1, "299.00");
    when(spuMapper.selectList(any())).thenReturn(List.of(spu));
    when(skuMapper.selectActiveBySpuIds(List.of(60001L))).thenReturn(List.of(expensiveSku, cheapSku));

    ProductSearchItemDTO dto = new ProductSearchItemDTO();
    dto.setId(60001L);
    dto.setPrice(new BigDecimal("299.00"));
    when(productItemConverter.toSearchItemDTO(spu, cheapSku)).thenReturn(dto);

    List<ProductSearchItemDTO> result = productQueryService.searchProductItems(" phone ", 10);

    assertEquals(1, result.size());
    assertEquals("spu-image", result.get(0).getImageUrl());
    assertEquals(new BigDecimal("299.00"), result.get(0).getPrice());
    verify(productItemConverter).toSearchItemDTO(spu, cheapSku);
    verify(productItemConverter, never()).toSearchItemDTO(spu, expensiveSku);
  }

  @Test
  void suggestProductsReturnsDistinctNamesWithinSafeSize() {
    ReflectionTestUtils.setField(productQueryService, "maxListSize", 2);
    when(spuMapper.selectList(any()))
        .thenReturn(
            List.of(
                spu(1L, "Phone", "a"),
                spu(2L, "Phone", "b"),
                spu(3L, "Phone Case", "c")));

    List<String> result = productQueryService.suggestProducts("phone", 99);

    assertEquals(List.of("Phone", "Phone Case"), result);
    verify(spuMapper).selectList(any());
  }

  private Spu spu(Long id, String name, String mainImage) {
    Spu spu = new Spu();
    spu.setId(id);
    spu.setSpuName(name);
    spu.setStatus(1);
    spu.setMainImage(mainImage);
    return spu;
  }

  private Sku sku(Long id, Long spuId, Integer status, String salePrice) {
    Sku sku = new Sku();
    sku.setId(id);
    sku.setSpuId(spuId);
    sku.setStatus(status);
    sku.setSalePrice(new BigDecimal(salePrice));
    return sku;
  }
}
