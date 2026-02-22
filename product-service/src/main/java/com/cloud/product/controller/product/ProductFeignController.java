package com.cloud.product.controller.product;

import com.cloud.api.product.ProductFeignClient;
import com.cloud.common.domain.dto.product.ProductRequestDTO;
import com.cloud.common.domain.vo.product.ProductVO;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.exception.ResourceNotFoundException;
import com.cloud.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

/**
 * Internal product endpoints for service-to-service calls.
 */
@Slf4j
@RestController
@RequestMapping("/internal/product")
@RequiredArgsConstructor
public class ProductFeignController implements ProductFeignClient {

    private final ProductService productService;

    @Override
    @PostMapping("/create")
    public Long createProduct(@RequestBody ProductRequestDTO productRequestDTO) {
        Long productId = productService.createProduct(productRequestDTO);
        if (productId == null) {
            throw new BusinessException("Create product failed");
        }
        return productId;
    }

    @Override
    @GetMapping("/{id}")
    public ProductVO getProductById(@PathVariable("id") Long id) {
        ProductVO productVO = productService.getProductById(id);
        if (productVO == null) {
            throw new ResourceNotFoundException("Product", String.valueOf(id));
        }
        return productVO;
    }

    @Override
    @PutMapping("/{id}")
    public Boolean updateProduct(@PathVariable("id") Long id, @RequestBody ProductRequestDTO productRequestDTO) {
        return Boolean.TRUE.equals(productService.updateProduct(id, productRequestDTO));
    }

    @Override
    @PatchMapping("/{id}")
    public Boolean patchProduct(@PathVariable("id") Long id, @RequestBody ProductRequestDTO productRequestDTO) {
        return Boolean.TRUE.equals(productService.updateProduct(id, productRequestDTO));
    }

    @Override
    @DeleteMapping("/{id}")
    public Boolean deleteProduct(@PathVariable("id") Long id) {
        return productService.deleteProduct(id);
    }

    @Override
    @GetMapping("/{id}/profile")
    public ProductVO getProductProfile(@PathVariable("id") Long id) {
        return getProductById(id);
    }

    @Override
    @PutMapping("/{id}/profile")
    public Boolean updateProductProfile(@PathVariable("id") Long id, @RequestBody ProductRequestDTO profileDTO) {
        return updateProduct(id, profileDTO);
    }

    @Override
    @PatchMapping("/{id}/status")
    public Boolean updateProductStatus(@PathVariable("id") Long id, @RequestParam Integer status) {
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
    @GetMapping("/category/{categoryId}")
    public List<ProductVO> getProductsByCategoryId(@PathVariable("categoryId") Long categoryId,
                                                   @RequestParam(required = false) Integer status) {
        List<ProductVO> products = productService.getProductsByCategoryId(categoryId, status);
        return products == null ? Collections.emptyList() : products;
    }

    @Override
    @GetMapping("/brand/{brandId}")
    public List<ProductVO> getProductsByBrandId(@PathVariable("brandId") Long brandId,
                                                @RequestParam(required = false) Integer status) {
        List<ProductVO> products = productService.getProductsByBrandId(brandId, status);
        return products == null ? Collections.emptyList() : products;
    }

    @Override
    @GetMapping("/batch")
    public List<ProductVO> getProductsByIds(@RequestParam List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<ProductVO> products = productService.getProductsByIds(ids);
        return products == null ? Collections.emptyList() : products;
    }

    @Override
    @DeleteMapping("/batch")
    public Boolean deleteProductsBatch(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return true;
        }
        return Boolean.TRUE.equals(productService.batchDeleteProducts(ids));
    }

    @Override
    @PutMapping("/batch/enable")
    public Boolean enableProductsBatch(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return true;
        }
        return Boolean.TRUE.equals(productService.batchEnableProducts(ids));
    }

    @Override
    @PutMapping("/batch/disable")
    public Boolean disableProductsBatch(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return true;
        }
        return Boolean.TRUE.equals(productService.batchDisableProducts(ids));
    }

    @Override
    @PostMapping("/batch")
    public Integer createProductsBatch(@RequestBody List<ProductRequestDTO> productList) {
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
    @GetMapping("/{productId}/stock/check")
    public Boolean checkStock(@PathVariable("productId") Long productId, @RequestParam Integer quantity) {
        return Boolean.TRUE.equals(productService.checkStock(productId, quantity));
    }

    @Override
    @PostMapping("/{productId}/stock/deduct")
    public Boolean deductStock(@PathVariable("productId") Long productId, @RequestParam Integer quantity) {
        return Boolean.TRUE.equals(productService.decreaseStock(productId, quantity));
    }

    @Override
    @PostMapping("/{productId}/stock/restore")
    public Boolean restoreStock(@PathVariable("productId") Long productId, @RequestParam Integer quantity) {
        return Boolean.TRUE.equals(productService.increaseStock(productId, quantity));
    }
}
