package com.cloud.product.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.product.controller.support.ProductMerchantGuard;
import com.cloud.product.service.ProductCatalogService;
import com.cloud.product.service.ProductQueryService;
import org.junit.jupiter.api.Test;

class ProductQueryControllerTest {

  @Test
  void listProductsShouldRejectInactiveStatusFilter() {
    ProductCatalogService productCatalogService = mock(ProductCatalogService.class);
    ProductQueryService productQueryService = mock(ProductQueryService.class);
    ProductQueryController controller =
        new ProductQueryController(
            productCatalogService,
            productQueryService,
            new ProductMerchantGuard(productCatalogService));

    BizException exception =
        assertThrows(BizException.class, () -> controller.listProducts(1, 20, null, null, null, 0));

    assertThat(exception.getCode()).isEqualTo(ResultCode.BAD_REQUEST.getCode());
  }
}
