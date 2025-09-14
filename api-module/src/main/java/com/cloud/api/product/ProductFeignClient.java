package com.cloud.api.product;

import com.cloud.common.domain.dto.product.ProductDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "product-service")
public interface ProductFeignClient {

    /**
     * 创建商品
     *
     * @param product 商品信息
     * @return 创建的商品信息
     */
    @PostMapping("/products")
    ProductDTO createProduct(@RequestBody ProductDTO product);

    /**
     * 根据ID获取商品
     *
     * @param id 商品ID
     * @return 商品信息
     */
    @GetMapping("/products/{id}")
    ProductDTO getProductById(@PathVariable("id") Long id);

    /**
     * 更新商品
     *
     * @param id      商品ID
     * @param product 商品信息
     * @return 更新后的商品信息
     */
    @PutMapping("/products/{id}")
    ProductDTO updateProduct(@PathVariable("id") Long id, @RequestBody ProductDTO product);

    /**
     * 删除商品
     *
     * @param id 商品ID
     * @return 操作结果
     */
    @DeleteMapping("/products/{id}")
    Boolean deleteProduct(@PathVariable("id") Long id);

    /**
     * 获取所有商品
     *
     * @return 商品列表
     */
    @GetMapping("/products")
    List<ProductDTO> getAllProducts();

    /**
     * 根据店铺ID获取商品列表
     *
     * @param shopId 店铺ID
     * @return 商品列表
     */
    @GetMapping("/products/shop/{shopId}")
    List<ProductDTO> getProductsByShopId(@PathVariable("shopId") Long shopId);

    Boolean putOnShelf(Long id);

    Boolean putOffShelf(Long id);
}