package com.cloud.product.service.support;

import com.cloud.common.domain.vo.product.SkuDetailVO;
import com.cloud.common.exception.BusinessException;
import com.cloud.product.mapper.SkuMapper;
import com.cloud.product.mapper.SpuMapper;
import com.cloud.product.messaging.ProductSyncMessageProducer;
import com.cloud.product.module.entity.Sku;
import com.cloud.product.module.entity.Spu;
import com.cloud.product.service.impl.ProductCatalogServiceImpl;
import com.cloud.product.service.support.ProductDetailCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductCatalogServiceImplTest {

    @Mock
    private SpuMapper spuMapper;

    @Mock
    private SkuMapper skuMapper;

    @Mock
    private ProductDetailCacheService productDetailCacheService;

    @Mock
    private ProductSyncMessageProducer productSyncMessageProducer;

    private ProductCatalogServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProductCatalogServiceImpl(
                spuMapper,
                skuMapper,
                productDetailCacheService,
                productSyncMessageProducer
        );
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
}
