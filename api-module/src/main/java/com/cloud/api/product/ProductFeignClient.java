package com.cloud.api.product;

import com.cloud.common.domain.dto.product.ProductRequestDTO;
import com.cloud.common.domain.vo.product.ProductVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品服务Feign客户端接口
 * 提供商品服务对外提供的Feign接口
 * 直接返回业务对象，仅用于服务内部调用
 *
 * @author cloud
 */
@FeignClient(name = "product-service", path = "/internal/product", contextId = "productFeignClient")
public interface ProductFeignClient {

    /**
     * 创建商品
     *
     * @param productRequestDTO 商品信息
     * @return 创建的商品ID
     */
    @PostMapping("/create")
    Long createProduct(@RequestBody ProductRequestDTO productRequestDTO);

    /**
     * 根据ID获取商品
     *
     * @param id 商品ID
     * @return 商品信息，不存在时返回null
     */
    @GetMapping("/{id}")
    ProductVO getProductById(@PathVariable("id") Long id);

    /**
     * 更新商品
     *
     * @param id      商品ID
     * @param productRequestDTO 商品信息
     * @return 是否更新成功
     */
    @PutMapping("/{id}")
    Boolean updateProduct(@PathVariable("id") Long id, @RequestBody ProductRequestDTO productRequestDTO);

    /**
     * 部分更新商品
     *
     * @param id      商品ID
     * @param productRequestDTO 商品信息
     * @return 是否更新成功
     */
    @PatchMapping("/{id}")
    Boolean patchProduct(@PathVariable("id") Long id, @RequestBody ProductRequestDTO productRequestDTO);

    /**
     * 删除商品
     *
     * @param id 商品ID
     * @return 是否删除成功
     */
    @DeleteMapping("/{id}")
    Boolean deleteProduct(@PathVariable("id") Long id);

    /**
     * 获取商品档案
     *
     * @param id 商品ID
     * @return 商品档案信息，不存在时返回null
     */
    @GetMapping("/{id}/profile")
    ProductVO getProductProfile(@PathVariable("id") Long id);

    /**
     * 更新商品档案
     *
     * @param id 商品ID
     * @param profileDTO 商品档案信息
     * @return 是否更新成功
     */
    @PutMapping("/{id}/profile")
    Boolean updateProductProfile(@PathVariable("id") Long id, @RequestBody ProductRequestDTO profileDTO);

    /**
     * 更新商品状态
     *
     * @param id 商品ID
     * @param status 商品状态
     * @return 是否更新成功
     */
    @PatchMapping("/{id}/status")
    Boolean updateProductStatus(@PathVariable("id") Long id, @RequestParam Integer status);

    /**
     * 根据分类查询商品
     *
     * @param categoryId 分类ID
     * @param status 商品状态
     * @return 商品列表，无数据时返回空列表
     */
    @GetMapping("/category/{categoryId}")
    List<ProductVO> getProductsByCategoryId(@PathVariable("categoryId") Long categoryId,
                                             @RequestParam(required = false) Integer status);

    /**
     * 根据品牌查询商品
     *
     * @param brandId 品牌ID
     * @param status 商品状态
     * @return 商品列表，无数据时返回空列表
     */
    @GetMapping("/brand/{brandId}")
    List<ProductVO> getProductsByBrandId(@PathVariable("brandId") Long brandId,
                                          @RequestParam(required = false) Integer status);

    // ==================== 批量操作接口 ====================

    /**
     * 批量获取商品
     *
     * @param ids 商品ID列表
     * @return 商品列表，无数据时返回空列表
     */
    @GetMapping("/batch")
    List<ProductVO> getProductsByIds(@RequestParam List<Long> ids);

    /**
     * 批量删除商品
     *
     * @param ids 商品ID列表
     * @return 是否删除成功
     */
    @DeleteMapping("/batch")
    Boolean deleteProductsBatch(@RequestBody List<Long> ids);

    /**
     * 批量上架商品
     *
     * @param ids 商品ID列表
     * @return 是否上架成功
     */
    @PutMapping("/batch/enable")
    Boolean enableProductsBatch(@RequestBody List<Long> ids);

    /**
     * 批量下架商品
     *
     * @param ids 商品ID列表
     * @return 是否下架成功
     */
    @PutMapping("/batch/disable")
    Boolean disableProductsBatch(@RequestBody List<Long> ids);

    /**
     * 批量创建商品
     *
     * @param productList 商品信息列表
     * @return 创建成功的商品数量
     */
    @PostMapping("/batch")
    Integer createProductsBatch(@RequestBody List<ProductRequestDTO> productList);

    // ==================== 库存相关接口 ====================

    /**
     * 检查商品库存
     *
     * @param productId 商品ID
     * @param quantity 需要的数量
     * @return 是否有足够库存
     */
    @GetMapping("/{productId}/stock/check")
    Boolean checkStock(@PathVariable("productId") Long productId, @RequestParam Integer quantity);

    /**
     * 扣减商品库存
     *
     * @param productId 商品ID
     * @param quantity 扣减数量
     * @return 是否扣减成功
     */
    @PostMapping("/{productId}/stock/deduct")
    Boolean deductStock(@PathVariable("productId") Long productId, @RequestParam Integer quantity);

    /**
     * 恢复商品库存
     *
     * @param productId 商品ID
     * @param quantity 恢复数量
     * @return 是否恢复成功
     */
    @PostMapping("/{productId}/stock/restore")
    Boolean restoreStock(@PathVariable("productId") Long productId, @RequestParam Integer quantity);
}