package com.cloud.product.rpc;

import com.cloud.api.product.ProductFeignClient;
import com.cloud.common.domain.dto.product.ProductRequestDTO;
import com.cloud.common.domain.vo.product.ProductVO;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.exception.ResourceNotFoundException;
import com.cloud.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.Collections;
import java.util.List;

@Slf4j
@DubboService(interfaceClass = ProductFeignClient.class, timeout = 5000, retries = 0)
@RequiredArgsConstructor
public class ProductDubboService implements ProductFeignClient {

    private final ProductService productService;

    @Override
    public Long createProduct(ProductRequestDTO productRequestDTO) {
        Long productId = productService.createProduct(productRequestDTO);
        if (productId == null) {
            throw new BusinessException("Create product failed");
        }
        return productId;
    }

    @Override
    public ProductVO getProductById(Long id) {
        ProductVO productVO = productService.getProductById(id);
        if (productVO == null) {
            throw new ResourceNotFoundException("Product", String.valueOf(id));
        }
        return productVO;
    }

    @Override
    public Boolean updateProduct(Long id, ProductRequestDTO productRequestDTO) {
        return Boolean.TRUE.equals(productService.updateProduct(id, productRequestDTO));
    }

    @Override
    public Boolean patchProduct(Long id, ProductRequestDTO productRequestDTO) {
        return Boolean.TRUE.equals(productService.updateProduct(id, productRequestDTO));
    }

    @Override
    public Boolean deleteProduct(Long id) {
        return productService.deleteProduct(id);
    }

    @Override
    public ProductVO getProductProfile(Long id) {
        return getProductById(id);
    }

    @Override
    public Boolean updateProductProfile(Long id, ProductRequestDTO profileDTO) {
        return updateProduct(id, profileDTO);
    }

    @Override
    public Boolean updateProductStatus(Long id, Integer status) {
        if (status == null) {
            throw new BusinessException("Product status is required");
        }
        if (status == 1) {
            return Boolean.TRUE.equals(productService.enableProduct(id));
        }
        if (status == 0) {
            return Boolean.TRUE.equals(productService.disableProduct(id));
        }
        throw new BusinessException("Invalid product status: " + status);
    }

    @Override
    public List<ProductVO> getProductsByCategoryId(Long categoryId, Integer status) {
        List<ProductVO> products = productService.getProductsByCategoryId(categoryId, status);
        return products == null ? Collections.emptyList() : products;
    }

    @Override
    public List<ProductVO> getProductsByBrandId(Long brandId, Integer status) {
        List<ProductVO> products = productService.getProductsByBrandId(brandId, status);
        return products == null ? Collections.emptyList() : products;
    }

    @Override
    public List<ProductVO> getProductsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<ProductVO> products = productService.getProductsByIds(ids);
        return products == null ? Collections.emptyList() : products;
    }

    @Override
    public Boolean deleteProductsBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return true;
        }
        return Boolean.TRUE.equals(productService.batchDeleteProducts(ids));
    }

    @Override
    public Boolean enableProductsBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return true;
        }
        return Boolean.TRUE.equals(productService.batchEnableProducts(ids));
    }

    @Override
    public Boolean disableProductsBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return true;
        }
        return Boolean.TRUE.equals(productService.batchDisableProducts(ids));
    }

    @Override
    public Integer createProductsBatch(List<ProductRequestDTO> productList) {
        if (productList == null || productList.isEmpty()) {
            return 0;
        }
        int successCount = 0;
        for (ProductRequestDTO requestDTO : productList) {
            try {
                if (productService.createProduct(requestDTO) != null) {
                    successCount++;
                }
            } catch (Exception ex) {
                log.warn("Batch create product failed: productName={}", requestDTO.getName(), ex);
            }
        }
        return successCount;
    }

    @Override
    public Boolean checkStock(Long productId, Integer quantity) {
        return Boolean.TRUE.equals(productService.checkStock(productId, quantity));
    }

    @Override
    public Boolean deductStock(Long productId, Integer quantity) {
        return Boolean.TRUE.equals(productService.decreaseStock(productId, quantity));
    }

    @Override
    public Boolean restoreStock(Long productId, Integer quantity) {
        return Boolean.TRUE.equals(productService.increaseStock(productId, quantity));
    }
}
