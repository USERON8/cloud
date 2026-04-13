package com.cloud.product.service;

import com.cloud.common.domain.dto.product.ProductSearchItemDTO;
import com.cloud.common.result.PageResult;
import com.cloud.product.dto.ProductItemDTO;
import java.util.List;

public interface ProductQueryService {

  PageResult<ProductItemDTO> listProducts(
      Integer page,
      Integer size,
      String name,
      Long categoryId,
      Long brandId,
      Long merchantId,
      Integer status);

  List<ProductItemDTO> searchProducts(String name, Integer size);

  List<ProductSearchItemDTO> searchProductItems(String name, Integer size);

  List<String> suggestProducts(String keyword, Integer size);
}
