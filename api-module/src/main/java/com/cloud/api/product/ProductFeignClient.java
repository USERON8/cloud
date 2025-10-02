package com.cloud.api.product;

import com.cloud.common.domain.dto.product.ProductDTO;
import com.cloud.common.domain.vo.OperationResultVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品服务Feign客户端接口
 * 提供商品服务对外提供的Feign接口
 *
 * @author what's up
 */
@FeignClient(name = "product-service", path = "/internal/product", contextId = "productFeignClient")
public interface ProductFeignClient {

    /**
     * 创建商品
     *
     * @param product 商品信息
     * @return 创建的商品信息
     */
@PostMapping
    ProductDTO createProduct(@RequestBody ProductDTO product);

    /**
     * 根据ID获取商品
     *
     * @param id 商品ID
     * @return 商品信息
     */
@GetMapping("/{id}")
    ProductDTO getProductById(@PathVariable("id") Long id);

    /**
     * 更新商品
     *
     * @param id      商品ID
     * @param product 商品信息
     * @return 更新后的商品信息
     */
@PutMapping("/{id}")
    ProductDTO updateProduct(@PathVariable("id") Long id, @RequestBody ProductDTO product);

    /**
     * 删除商品
     *
     * @param id 商品ID
     * @return 操作结果
     */
@DeleteMapping("/{id}")
    OperationResultVO deleteProduct(@PathVariable("id") Long id);

    /**
     * 获取所有商品
     *
     * @return 商品列表
     */
@GetMapping
    List<ProductDTO> getAllProducts();

    /**
     * 根据店铺ID获取商品列表
     *
     * @param shopId 店铺ID
     * @return 商品列表
     */
@GetMapping("/shop/{shopId}")
    List<ProductDTO> getProductsByShopId(@PathVariable("shopId") Long shopId);

    /**
     * 商品上架
     *
     * @param id 商品ID
     * @return 操作结果
     */
@PutMapping("/{id}/shelf-on")
    OperationResultVO putOnShelf(@PathVariable("id") Long id);

    /**
     * 商品下架
     *
     * @param id 商品ID
     * @return 操作结果
     */
@PutMapping("/{id}/shelf-off")
    OperationResultVO putOffShelf(@PathVariable("id") Long id);
}