package com.cloud.search.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.cloud.common.exception.BizException;
import com.cloud.search.dto.ProductSearchRequest;
import com.cloud.search.service.SearchFacadeService;
import org.junit.jupiter.api.Test;

class ProductSearchControllerTest {

  @Test
  void complexSearchShouldRejectInactiveStatusFilter() {
    ProductSearchController controller =
        new ProductSearchController(mock(SearchFacadeService.class));
    ProductSearchRequest request = new ProductSearchRequest();
    request.setStatus(0);

    assertThrows(BizException.class, () -> controller.complexSearch(request, null));
  }
}
