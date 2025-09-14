package com.cloud.product.controller.product;

import com.cloud.api.product.ProductFeignClient;
import com.cloud.common.domain.dto.product.ProductDTO;
import com.cloud.product.converter.ProductConverter;
import com.cloud.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品服务Feign客户端接口实现控制器
 * 实现商品服务对外提供的Feign接口
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ProductController implements ProductFeignClient {

    private final ProductService productService;
    private final ProductConverter productConverter;

    /**
     * 创建商品
     *
     * @param product 商品信息
     * @return 创建的商品信息
     */
    @Override
    public ProductDTO createProduct(ProductDTO product) {
        log.info("[商品Feign客户端控制器] 开始处理创建商品请求，商品名称: {}", product.getName());
        try {
            ProductDTO savedProductDTO = productService.createProductForFeign(product);
            if (savedProductDTO != null) {
                log.info("[商品Feign客户端控制器] 创建商品成功，商品ID: {}", savedProductDTO.getId());
                return savedProductDTO;
            } else {
                log.error("[商品Feign客户端控制器] 创建商品失败: {}", product.getName());
                return null;
            }
        } catch (Exception e) {
            log.error("[商品Feign客户端控制器] 创建商品异常", e);
            return null;
        }
    }

    /**
     * 根据ID获取商品
     *
     * @param id 商品ID
     * @return 商品信息
     */
    @Override
    public ProductDTO getProductById(Long id) {
        log.info("[商品Feign客户端控制器] 开始处理根据ID获取商品请求，商品ID: {}", id);
        try {
            ProductDTO productDTO = productService.getProductByIdForFeign(id);
            if (productDTO != null) {
                log.info("[商品Feign客户端控制器] 根据ID获取商品成功，商品ID: {}", id);
                return productDTO;
            } else {
                log.warn("[商品Feign客户端控制器] 商品不存在，商品ID: {}", id);
                return null;
            }
        } catch (Exception e) {
            log.error("[商品Feign客户端控制器] 根据ID获取商品异常，商品ID: {}", id, e);
            return null;
        }
    }

    /**
     * 更新商品
     *
     * @param id      商品ID
     * @param product 商品信息
     * @return 更新后的商品信息
     */
    @Override
    public ProductDTO updateProduct(Long id, ProductDTO product) {
        log.info("[商品Feign客户端控制器] 开始处理更新商品请求，商品ID: {}", id);
        try {
            ProductDTO updatedProductDTO = productService.updateProductForFeign(id, product);
            if (updatedProductDTO != null) {
                log.info("[商品Feign客户端控制器] 更新商品成功，商品ID: {}", id);
                return updatedProductDTO;
            } else {
                log.error("[商品Feign客户端控制器] 更新商品失败，商品ID: {}", id);
                return null;
            }
        } catch (Exception e) {
            log.error("[商品Feign客户端控制器] 更新商品异常，商品ID: {}", id, e);
            return null;
        }
    }

    /**
     * 删除商品
     *
     * @param id 商品ID
     * @return 操作结果
     */
    @Override
    public Boolean deleteProduct(Long id) {
        log.info("[商品Feign客户端控制器] 开始处理删除商品请求，商品ID: {}", id);
        try {
            boolean result = productService.deleteProduct(id);
            if (result) {
                log.info("[商品Feign客户端控制器] 删除商品成功，商品ID: {}", id);
                return true;
            } else {
                log.warn("[商品Feign客户端控制器] 商品不存在或删除失败，商品ID: {}", id);
                return false;
            }
        } catch (Exception e) {
            log.error("[商品Feign客户端控制器] 删除商品异常，商品ID: {}", id, e);
            return false;
        }
    }

    /**
     * 获取所有商品
     *
     * @return 商品列表
     */
    @Override
    public List<ProductDTO> getAllProducts() {
        log.info("[商品Feign客户端控制器] 开始处理获取所有商品请求");
        try {
            List<ProductDTO> productDTOs = productService.getAllProducts();
            log.info("[商品Feign客户端控制器] 获取所有商品成功，共{}条记录", productDTOs.size());
            return productDTOs;
        } catch (Exception e) {
            log.error("[商品Feign客户端控制器] 获取所有商品异常", e);
            return List.of();
        }
    }

    /**
     * 根据店铺ID获取商品列表
     *
     * @param shopId 店铺ID
     * @return 商品列表
     */
    @Override
    public List<ProductDTO> getProductsByShopId(Long shopId) {
        log.info("[商品Feign客户端控制器] 开始处理根据店铺ID获取商品列表请求，店铺ID: {}", shopId);
        try {
            List<ProductDTO> productDTOs = productService.getProductsByShopId(shopId);
            log.info("[商品Feign客户端控制器] 根据店铺ID获取商品列表成功，店铺ID: {}，共{}条记录", shopId, productDTOs.size());
            return productDTOs;
        } catch (Exception e) {
            log.error("[商品Feign客户端控制器] 根据店铺ID获取商品列表异常，店铺ID: {}", shopId, e);
            return List.of();
        }
    }

    @Override
    public Boolean putOnShelf(Long id) {
        log.info("[商品Feign客户端控制器] 开始处理商品上架请求，商品ID: {}", id);
        try {
            Boolean result = productService.enableProduct(id);
            if (result) {
                log.info("[商品Feign客户端控制器] 商品上架成功，商品ID: {}", id);
                return true;
            } else {
                log.warn("[商品Feign客户端控制器] 商品上架失败，商品ID: {}", id);
                return false;
            }
        } catch (Exception e) {
            log.error("[商品Feign客户端控制器] 商品上架异常，商品ID: {}", id, e);
            return false;
        }
    }

    @Override
    public Boolean putOffShelf(Long id) {
        log.info("[商品Feign客户端控制器] 开始处理商品下架请求，商品ID: {}", id);
        try {
            Boolean result = productService.disableProduct(id);
            if (result) {
                log.info("[商品Feign客户端控制器] 商品下架成功，商品ID: {}", id);
                return true;
            } else {
                log.warn("[商品Feign客户端控制器] 商品下架失败，商品ID: {}", id);
                return false;
            }
        } catch (Exception e) {
            log.error("[商品Feign客户端控制器] 商品下架异常，商品ID: {}", id, e);
            return false;
        }
    }
}