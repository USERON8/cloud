package com.cloud.product.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.common.domain.dto.product.SkuDTO;
import com.cloud.common.domain.dto.product.SpuCreateRequestDTO;
import com.cloud.common.domain.dto.product.SpuDTO;
import com.cloud.common.exception.BusinessException;
import com.cloud.product.mapper.BrandMapper;
import com.cloud.product.mapper.CategoryMapper;
import com.cloud.product.mapper.SkuMapper;
import com.cloud.product.mapper.SpuMapper;
import com.cloud.product.messaging.ProductSyncMessageProducer;
import com.cloud.product.module.entity.Spu;
import com.cloud.product.service.support.ProductDetailCacheService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class ProductCatalogServiceImplTest {

  @Mock private SpuMapper spuMapper;

  @Mock private SkuMapper skuMapper;

  @Mock private CategoryMapper categoryMapper;

  @Mock private BrandMapper brandMapper;

  @Mock private ProductDetailCacheService productDetailCacheService;

  @Mock private ProductSyncMessageProducer productSyncMessageProducer;

  @InjectMocks private ProductCatalogServiceImpl productCatalogService;

  @Test
  void createSpu_duplicate_throwsBusinessException() {
    SpuDTO spuDTO = new SpuDTO();
    spuDTO.setSpuName("spu");
    SpuCreateRequestDTO request = new SpuCreateRequestDTO();
    request.setSpu(spuDTO);
    request.setSkus(List.of(new SkuDTO()));

    when(spuMapper.insert(any(Spu.class))).thenThrow(new DuplicateKeyException("dup"));

    assertThatThrownBy(() -> productCatalogService.createSpu(request))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("spu already exists");
  }

  @Test
  void updateSpuStatus_success_evictsCacheAndSyncs() {
    Spu spu = new Spu();
    spu.setId(10L);
    spu.setDeleted(0);
    when(spuMapper.selectById(10L)).thenReturn(spu);
    when(spuMapper.updateById(spu)).thenReturn(1);

    productCatalogService.updateSpuStatus(10L, 1);

    verify(productDetailCacheService).evict(10L);
    verify(productSyncMessageProducer).sendUpsert(10L);
  }

  @Test
  void updateSpuStatus_missingSpu_throws() {
    when(spuMapper.selectById(10L)).thenReturn(null);

    assertThatThrownBy(() -> productCatalogService.updateSpuStatus(10L, 1))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("spu not found");
  }
}
