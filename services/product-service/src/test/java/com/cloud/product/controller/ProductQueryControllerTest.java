package com.cloud.product.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.common.result.PageResult;
import com.cloud.product.controller.support.ProductMerchantGuard;
import com.cloud.product.dto.ProductItemDTO;
import com.cloud.product.service.ProductCatalogService;
import com.cloud.product.service.ProductQueryService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;

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

  @Test
  void listManageProductsShouldAllowAdminToQueryMerchantScope() {
    ProductCatalogService productCatalogService = mock(ProductCatalogService.class);
    ProductQueryService productQueryService = mock(ProductQueryService.class);
    ProductQueryController controller =
        new ProductQueryController(
            productCatalogService,
            productQueryService,
            new ProductMerchantGuard(productCatalogService));
    TestingAuthenticationToken authentication =
        new TestingAuthenticationToken("admin-user", "password", "ROLE_ADMIN", "product:edit");
    authentication.setAuthenticated(true);
    authentication.setDetails(null);

    PageResult<ProductItemDTO> expected = PageResult.of(1L, 20L, 0L, List.of());
    when(productQueryService.listProducts(1, 20, null, null, null, 3001L, null))
        .thenReturn(expected);

    PageResult<ProductItemDTO> result =
        controller
            .listManageProducts(1, 20, null, null, null, 3001L, null, authentication)
            .getData();

    assertThat(result).isSameAs(expected);
  }
}
