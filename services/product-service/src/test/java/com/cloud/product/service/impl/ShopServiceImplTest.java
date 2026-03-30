package com.cloud.product.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.product.converter.ShopConverter;
import com.cloud.product.module.dto.ShopPageDTO;
import com.cloud.product.module.entity.Shop;
import com.cloud.product.module.vo.ShopVO;
import com.cloud.product.service.cache.ShopRedisCacheService;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShopServiceImplTest {

  @Mock private ShopConverter shopConverter;
  @Mock private ShopRedisCacheService shopRedisCacheService;

  private ShopServiceImpl service;

  @BeforeEach
  void setUp() {
    service = spy(new ShopServiceImpl(shopConverter, shopRedisCacheService));
    TableInfoHelper.initTableInfo(
        new MapperBuilderAssistant(new MybatisConfiguration(), "shop-service-test"), Shop.class);
  }

  @Test
  void searchShopsByNameShouldReturnEmptyWhenBlank() {
    List<ShopVO> result = service.searchShopsByName("   ", null);

    assertThat(result).isEmpty();
    verify(service, never())
        .list(
            org.mockito.ArgumentMatchers
                .<com.baomidou.mybatisplus.core.conditions.Wrapper<Shop>>any());
  }

  @Test
  void getShopsByMerchantIdShouldMapResult() {
    when(shopRedisCacheService.getMerchantList(99L, 1)).thenReturn(null);
    Shop shop = new Shop();
    shop.setId(11L);
    doReturn(List.of(shop))
        .when(service)
        .list(
            org.mockito.ArgumentMatchers
                .<com.baomidou.mybatisplus.core.conditions.Wrapper<Shop>>any());
    doReturn(List.of(new ShopVO())).when(shopConverter).toVOList(List.of(shop));

    List<ShopVO> result = service.getShopsByMerchantId(99L, 1);

    assertThat(result).hasSize(1);
  }

  @Test
  void getShopsPageShouldHonorMerchantAddressAndUpdateSortFields() {
    when(shopRedisCacheService.getPage(any())).thenReturn(null);
    Shop shop = new Shop();
    shop.setId(21L);

    @SuppressWarnings("unchecked")
    Page<Shop> pageResult = new Page<>(2, 5, 1);
    pageResult.setRecords(List.of(shop));

    final LambdaQueryWrapper<Shop>[] capturedWrapper = new LambdaQueryWrapper[1];
    doReturn(pageResult)
        .when(service)
        .page(
            any(Page.class),
            org.mockito.ArgumentMatchers.<Wrapper<Shop>>argThat(
                wrapper -> {
                  capturedWrapper[0] = (LambdaQueryWrapper<Shop>) wrapper;
                  return true;
                }));
    doReturn(List.of(new ShopVO())).when(shopConverter).toVOList(List.of(shop));

    ShopPageDTO request = new ShopPageDTO();
    request.setCurrent(2L);
    request.setSize(5L);
    request.setMerchantId(9L);
    request.setShopNameKeyword("cloud");
    request.setAddressKeyword("road");
    request.setStatus(1);
    request.setUpdateTimeSort("asc");

    var result = service.getShopsPage(request);

    assertThat(result.getRecords()).hasSize(1);
    assertThat(capturedWrapper[0]).isNotNull();
    String sqlSegment = capturedWrapper[0].getSqlSegment();
    assertThat(sqlSegment).contains("merchant_id");
    assertThat(sqlSegment).contains("shop_name");
    assertThat(sqlSegment).contains("address");
    assertThat(sqlSegment).contains("status");
    assertThat(sqlSegment).contains("updated_at");
  }
}
