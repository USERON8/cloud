package com.cloud.api.product;

import com.cloud.common.domain.dto.product.ProductRequestDTO;
import com.cloud.common.domain.vo.product.ProductVO;

import java.util.List;

public interface ProductFeignClient {

    Long createProduct(ProductRequestDTO productRequestDTO);

    ProductVO getProductById(Long id);

    Boolean updateProduct(Long id, ProductRequestDTO productRequestDTO);

    Boolean patchProduct(Long id, ProductRequestDTO productRequestDTO);

    Boolean deleteProduct(Long id);

    ProductVO getProductProfile(Long id);

    Boolean updateProductProfile(Long id, ProductRequestDTO profileDTO);

    Boolean updateProductStatus(Long id, Integer status);

    List<ProductVO> getProductsByCategoryId(Long categoryId, Integer status);

    List<ProductVO> getProductsByBrandId(Long brandId, Integer status);

    List<ProductVO> getProductsByIds(List<Long> ids);

    Boolean deleteProductsBatch(List<Long> ids);

    Boolean enableProductsBatch(List<Long> ids);

    Boolean disableProductsBatch(List<Long> ids);

    Integer createProductsBatch(List<ProductRequestDTO> productList);

    Boolean checkStock(Long productId, Integer quantity);

    Boolean deductStock(Long productId, Integer quantity);

    Boolean restoreStock(Long productId, Integer quantity);
}
