package com.cloud.api.product;

import com.cloud.common.domain.dto.product.ProductRequestDTO;
import com.cloud.common.domain.vo.product.ProductVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;








@FeignClient(name = "product-service", path = "/internal/product", contextId = "productFeignClient")
public interface ProductFeignClient {

    





    @PostMapping("/create")
    Long createProduct(@RequestBody ProductRequestDTO productRequestDTO);

    





    @GetMapping("/{id}")
    ProductVO getProductById(@PathVariable("id") Long id);

    






    @PutMapping("/{id}")
    Boolean updateProduct(@PathVariable("id") Long id, @RequestBody ProductRequestDTO productRequestDTO);

    






    @PatchMapping("/{id}")
    Boolean patchProduct(@PathVariable("id") Long id, @RequestBody ProductRequestDTO productRequestDTO);

    





    @DeleteMapping("/{id}")
    Boolean deleteProduct(@PathVariable("id") Long id);

    





    @GetMapping("/{id}/profile")
    ProductVO getProductProfile(@PathVariable("id") Long id);

    






    @PutMapping("/{id}/profile")
    Boolean updateProductProfile(@PathVariable("id") Long id, @RequestBody ProductRequestDTO profileDTO);

    






    @PatchMapping("/{id}/status")
    Boolean updateProductStatus(@PathVariable("id") Long id, @RequestParam Integer status);

    






    @GetMapping("/category/{categoryId}")
    List<ProductVO> getProductsByCategoryId(@PathVariable("categoryId") Long categoryId,
                                            @RequestParam(required = false) Integer status);

    






    @GetMapping("/brand/{brandId}")
    List<ProductVO> getProductsByBrandId(@PathVariable("brandId") Long brandId,
                                         @RequestParam(required = false) Integer status);

    

    





    @GetMapping("/batch")
    List<ProductVO> getProductsByIds(@RequestParam List<Long> ids);

    





    @DeleteMapping("/batch")
    Boolean deleteProductsBatch(@RequestBody List<Long> ids);

    





    @PutMapping("/batch/enable")
    Boolean enableProductsBatch(@RequestBody List<Long> ids);

    





    @PutMapping("/batch/disable")
    Boolean disableProductsBatch(@RequestBody List<Long> ids);

    





    @PostMapping("/batch")
    Integer createProductsBatch(@RequestBody List<ProductRequestDTO> productList);

    

    






    @GetMapping("/{productId}/stock/check")
    Boolean checkStock(@PathVariable("productId") Long productId, @RequestParam Integer quantity);

    






    @PostMapping("/{productId}/stock/deduct")
    Boolean deductStock(@PathVariable("productId") Long productId, @RequestParam Integer quantity);

    






    @PostMapping("/{productId}/stock/restore")
    Boolean restoreStock(@PathVariable("productId") Long productId, @RequestParam Integer quantity);
}
