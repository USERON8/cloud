package com.cloud.product.service.impl;

import com.cloud.product.converter.ShopConverter;
import com.cloud.product.module.entity.Shop;
import com.cloud.product.module.vo.ShopVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ShopServiceImplTest {

    @Mock
    private ShopConverter shopConverter;

    private ShopServiceImpl service;

    @BeforeEach
    void setUp() {
        service = spy(new ShopServiceImpl(shopConverter));
    }

    @Test
    void searchShopsByNameShouldReturnEmptyWhenBlank() {
        List<ShopVO> result = service.searchShopsByName("   ", null);

        assertThat(result).isEmpty();
        verify(service, never()).list(org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.conditions.Wrapper<Shop>>any());
    }

    @Test
    void getShopsByMerchantIdShouldMapResult() {
        Shop shop = new Shop();
        shop.setId(11L);
        doReturn(List.of(shop)).when(service)
                .list(org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.conditions.Wrapper<Shop>>any());
        doReturn(List.of(new ShopVO())).when(shopConverter).toVOList(List.of(shop));

        List<ShopVO> result = service.getShopsByMerchantId(99L, 1);

        assertThat(result).hasSize(1);
    }
}
