package com.cloud.product.controller.product;

import com.cloud.api.product.ProductFeignClient;
import com.cloud.common.domain.dto.product.ProductRequestDTO;
import com.cloud.common.domain.vo.product.ProductVO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.exception.ResourceNotFoundException;
import com.cloud.common.result.Result;
import com.cloud.product.converter.ProductConverter;
import com.cloud.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品服务Feign客户端接口实现控制器
 * 实现商品服务对外提供的Feign接口
 * 该控制器专门用于处理其他微服务通过Feign发起的调用
 */
@Slf4j
@RestController
@RequestMapping("/internal/product")
@RequiredArgsConstructor
public class ProductFeignController implements ProductFeignClient {

    private final ProductService productService;
    private final ProductConverter productConverter;

    /**
     * 创建商品
     *
     * @param productRequestDTO 商品信息
     * @return 创建的商品ID
     */
    @Override
    @PostMapping("/create")
    public Long createProduct(@RequestBody ProductRequestDTO productRequestDTO) {
        log.info("[商品Feign控制器] 开始处理创建商品请求，商品名称: {}", productRequestDTO.getName());
        Long productId = productService.createProduct(productRequestDTO);
        if (productId == null) {
            log.error("[商品Feign控制器] 创建商品失败: {}", productRequestDTO.getName());
            throw new BusinessException("创建商品失败");
        }
        log.info("[商品Feign控制器] 创建商品成功，商品ID: {}", productId);
        return productId;
    }

    /**
     * 根据ID获取商品
     *
     * @param id 商品ID
     * @return 商品信息
     */
    @Override
    @GetMapping("/{id}")
    public ProductVO getProductById(@PathVariable("id") Long id) {
        log.info("[商品Feign控制器] 开始处理根据ID获取商品请求，商品ID: {}", id);
        ProductVO productVO = productService.getProductById(id);
        if (productVO == null) {
            log.warn("[商品Feign控制器] 商品不存在，商品ID: {}", id);
            throw new ResourceNotFoundException("Product", String.valueOf(id));
        }
        log.info("[商品Feign控制器] 根据ID获取商品成功，商品ID: {}", id);
        return productVO;
    }

    /**
     * 更新商品
     *
     * @param id      商品ID
     * @param productRequestDTO 商品信息
     * @return 是否更新成功
     */
    @Override
    @PutMapping("/{id}")
    public Boolean updateProduct(@PathVariable("id") Long id, @RequestBody ProductRequestDTO productRequestDTO) {
        log.info("[商品Feign控制器] 开始处理更新商品请求，商品ID: {}", id);
        Boolean result = productService.updateProduct(id, productRequestDTO);
        if (result == null || !result) {
            log.error("[商品Feign控制器] 更新商品失败，商品ID: {}", id);
            throw new BusinessException("更新商品失败");
        }
        log.info("[商品Feign控制器] 更新商品成功，商品ID: {}", id);
        return result;
    }

    /**
     * 部分更新商品
     *
     * @param id      商品ID
     * @param productRequestDTO 商品信息
     * @return 是否更新成功
     */
    @Override
    @PatchMapping("/{id}")
    public Boolean patchProduct(@PathVariable("id") Long id, @RequestBody ProductRequestDTO productRequestDTO) {
        log.info("[商品Feign控制器] 开始处理部分更新商品请求，商品ID: {}", id);
        Boolean result = productService.updateProduct(id, productRequestDTO);
        if (result == null || !result) {
            log.error("[商品Feign控制器] 部分更新商品失败，商品ID: {}", id);
            throw new BusinessException("部分更新商品失败");
        }
        log.info("[商品Feign控制器] 部分更新商品成功，商品ID: {}", id);
        return result;
    }

    /**
     * 删除商品
     *
     * @param id 商品ID
     * @return 是否删除成功
     */
    @Override
    @DeleteMapping("/{id}")
    public Boolean deleteProduct(@PathVariable("id") Long id) {
        log.info("[商品Feign控制器] 开始处理删除商品请求，商品ID: {}", id);
        boolean result = productService.deleteProduct(id);
        if (!result) {
            log.warn("[商品Feign控制器] 商品不存在或删除失败，商品ID: {}", id);
            throw new BusinessException("商品不存在或删除失败");
        }
        log.info("[商品Feign控制器] 删除商品成功，商品ID: {}", id);
        return result;
    }

    /**
     * 获取商品档案
     *
     * @param id 商品ID
     * @return 商品档案信息
     */
    @Override
    @GetMapping("/{id}/profile")
    public ProductVO getProductProfile(@PathVariable("id") Long id) {
        log.info("[商品Feign控制器] 开始处理获取商品档案请求，商品ID: {}", id);
        return getProductById(id);
    }

    /**
     * 更新商品档案
     *
     * @param id 商品ID
     * @param profileDTO 商品档案信息
     * @return 是否更新成功
     */
    @Override
    @PutMapping("/{id}/profile")
    public Boolean updateProductProfile(@PathVariable("id") Long id, @RequestBody ProductRequestDTO profileDTO) {
        log.info("[商品Feign控制器] 开始处理更新商品档案请求，商品ID: {}", id);
        return updateProduct(id, profileDTO);
    }

    /**
     * 更新商品状态
     *
     * @param id 商品ID
     * @param status 商品状态
     * @return 是否更新成功
     */
    @Override
    @PatchMapping("/{id}/status")
    public Boolean updateProductStatus(@PathVariable("id") Long id, @RequestParam Integer status) {
        log.info("[商品Feign控制器] 开始处理更新商品状态请求，商品ID: {}, 状态: {}", id, status);
        Boolean result;
        if (status == 1) {
            result = productService.enableProduct(id);
        } else if (status == 0) {
            result = productService.disableProduct(id);
        } else {
            throw new BusinessException("无效的商品状态: " + status);
        }
        if (result == null || !result) {
            log.error("[商品Feign控制器] 更新商品状态失败，商品ID: {}", id);
            throw new BusinessException("更新商品状态失败");
        }
        log.info("[商品Feign控制器] 更新商品状态成功，商品ID: {}", id);
        return result;
    }

    /**
     * 根据分类查询商品
     *
     * @param categoryId 分类ID
     * @param status 商品状态
     * @return 商品列表
     */
    @Override
    @GetMapping("/category/{categoryId}")
    public List<ProductVO> getProductsByCategoryId(@PathVariable("categoryId") Long categoryId,
                                                    @RequestParam(required = false) Integer status) {
        log.info("[商品Feign控制器] 开始处理根据分类获取商品列表请求，分类ID: {}", categoryId);
        List<ProductVO> productVOs = productService.getProductsByCategoryId(categoryId, status);
        log.info("[商品Feign控制器] 根据分类获取商品列表成功，分类ID: {}，共{}条记录", categoryId, productVOs.size());
        return productVOs;
    }

    /**
     * 根据品牌查询商品
     *
     * @param brandId 品牌ID
     * @param status 商品状态
     * @return 商品列表
     */
    @Override
    @GetMapping("/brand/{brandId}")
    public List<ProductVO> getProductsByBrandId(@PathVariable("brandId") Long brandId,
                                                 @RequestParam(required = false) Integer status) {
        log.info("[商品Feign控制器] 开始处理根据品牌获取商品列表请求，品牌ID: {}", brandId);
        List<ProductVO> productVOs = productService.getProductsByBrandId(brandId, status);
        log.info("[商品Feign控制器] 根据品牌获取商品列表成功，品牌ID: {}，共{}条记录", brandId, productVOs.size());
        return productVOs;
    }

    // ==================== 批量操作接口 ====================

    /**
     * 批量获取商品
     *
     * @param ids 商品ID列表
     * @return 商品列表
     */
    @Override
    @GetMapping("/batch")
    public List<ProductVO> getProductsByIds(@RequestParam List<Long> ids) {
        log.info("[商品Feign控制器] 开始处理批量获取商品请求，商品ID数量: {}", ids.size());
        List<ProductVO> productVOs = productService.getProductsByIds(ids);
        log.info("[商品Feign控制器] 批量获取商品成功，共{}条记录", productVOs.size());
        return productVOs;
    }

    /**
     * 批量删除商品
     *
     * @param ids 商品ID列表
     * @return 是否删除成功
     */
    @Override
    @DeleteMapping("/batch")
    public Boolean deleteProductsBatch(@RequestBody List<Long> ids) {
        log.info("[商品Feign控制器] 开始处理批量删除商品请求，商品ID数量: {}", ids.size());
        Boolean result = productService.batchDeleteProducts(ids);
        log.info("[商品Feign控制器] 批量删除商品完成，结果: {}", result);
        return result;
    }

    /**
     * 批量上架商品
     *
     * @param ids 商品ID列表
     * @return 是否上架成功
     */
    @Override
    @PutMapping("/batch/enable")
    public Boolean enableProductsBatch(@RequestBody List<Long> ids) {
        log.info("[商品Feign控制器] 开始处理批量上架商品请求，商品ID数量: {}", ids.size());
        Boolean result = productService.batchEnableProducts(ids);
        log.info("[商品Feign控制器] 批量上架商品完成，结果: {}", result);
        return result;
    }

    /**
     * 批量下架商品
     *
     * @param ids 商品ID列表
     * @return 是否下架成功
     */
    @Override
    @PutMapping("/batch/disable")
    public Boolean disableProductsBatch(@RequestBody List<Long> ids) {
        log.info("[商品Feign控制器] 开始处理批量下架商品请求，商品ID数量: {}", ids.size());
        Boolean result = productService.batchDisableProducts(ids);
        log.info("[商品Feign控制器] 批量下架商品完成，结果: {}", result);
        return result;
    }

    /**
     * 批量创建商品
     *
     * @param productList 商品信息列表
     * @return 创建成功的商品数量
     */
    @Override
    @PostMapping("/batch")
    public Integer createProductsBatch(@RequestBody List<ProductRequestDTO> productList) {
        log.info("[商品Feign控制器] 开始处理批量创建商品请求，商品数量: {}", productList.size());
        Integer count = 0;
        for (ProductRequestDTO productRequestDTO : productList) {
            try {
                Long productId = productService.createProduct(productRequestDTO);
                if (productId != null) {
                    count++;
                }
            } catch (Exception e) {
                log.error("[商品Feign控制器] 创建商品失败: {}", productRequestDTO.getName(), e);
            }
        }
        log.info("[商品Feign控制器] 批量创建商品完成，成功创建{}条记录", count);
        return count;
    }

    // ==================== 库存相关接口 ====================

    /**
     * 检查商品库存
     *
     * @param productId 商品ID
     * @param quantity 需要的数量
     * @return 是否有足够库存
     */
    @Override
    @GetMapping("/{productId}/stock/check")
    public Boolean checkStock(@PathVariable("productId") Long productId, @RequestParam Integer quantity) {
        log.info("[商品Feign控制器] 开始处理检查商品库存请求，商品ID: {}, 数量: {}", productId, quantity);
        Boolean result = productService.checkStock(productId, quantity);
        log.info("[商品Feign控制器] 检查商品库存完成，商品ID: {}, 结果: {}", productId, result);
        return result;
    }

    /**
     * 扣减商品库存
     *
     * @param productId 商品ID
     * @param quantity 扣减数量
     * @return 是否扣减成功
     */
    @Override
    @PostMapping("/{productId}/stock/deduct")
    public Boolean deductStock(@PathVariable("productId") Long productId, @RequestParam Integer quantity) {
        log.info("[商品Feign控制器] 开始处理扣减商品库存请求，商品ID: {}, 数量: {}", productId, quantity);
        Boolean result = productService.decreaseStock(productId, quantity);
        log.info("[商品Feign控制器] 扣减商品库存完成，商品ID: {}, 结果: {}", productId, result);
        return result;
    }

    /**
     * 恢复商品库存
     *
     * @param productId 商品ID
     * @param quantity 恢复数量
     * @return 是否恢复成功
     */
    @Override
    @PostMapping("/{productId}/stock/restore")
    public Boolean restoreStock(@PathVariable("productId") Long productId, @RequestParam Integer quantity) {
        log.info("[商品Feign控制器] 开始处理恢复商品库存请求，商品ID: {}, 数量: {}", productId, quantity);
        Boolean result = productService.increaseStock(productId, quantity);
        log.info("[商品Feign控制器] 恢复商品库存完成，商品ID: {}, 结果: {}", productId, result);
        return result;
    }
}