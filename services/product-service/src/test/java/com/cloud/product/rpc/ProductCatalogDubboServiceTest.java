package com.cloud.product.rpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.common.domain.dto.product.SpuCreateRequestDTO;
import com.cloud.common.domain.vo.product.SpuDetailVO;
import com.cloud.product.service.ProductCatalogService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductCatalogDubboServiceTest {

  @Mock private ProductCatalogService productCatalogService;

  private ProductCatalogDubboService productCatalogDubboService;

  @BeforeEach
  void setUp() {
    productCatalogDubboService = new ProductCatalogDubboService(productCatalogService);
  }

  @Test
  void createSpu_delegates() {
    SpuCreateRequestDTO request = new SpuCreateRequestDTO();
    when(productCatalogService.createSpu(request)).thenReturn(5L);

    Long id = productCatalogDubboService.createSpu(request);

    assertThat(id).isEqualTo(5L);
    verify(productCatalogService).createSpu(request);
  }

  @Test
  void listSpuByCategory_delegates() {
    SpuDetailVO vo = new SpuDetailVO();
    when(productCatalogService.listSpuByCategory(1L, 1)).thenReturn(List.of(vo));

    List<SpuDetailVO> result = productCatalogDubboService.listSpuByCategory(1L, 1);

    assertThat(result).containsExactly(vo);
  }
}
