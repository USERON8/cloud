package com.cloud.product.service;

import com.cloud.common.result.PageResult;
import com.cloud.product.dto.ProductItemDTO;

import java.util.List;

public interface ProductQueryService {

    PageResult<ProductItemDTO> listProducts(Integer page,
                                            Integer size,
                                            String name,
                                            Long categoryId,
                                            Long brandId,
                                            Integer status);

    List<ProductItemDTO> searchProducts(String name, Integer size);
}
