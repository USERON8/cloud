package com.cloud.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.common.domain.dto.product.ProductDTO;
import com.cloud.common.domain.dto.product.ProductRequestDTO;
import com.cloud.common.domain.vo.product.ProductVO;
import com.cloud.common.result.PageResult;
import com.cloud.product.exception.ProductServiceException;
import com.cloud.product.module.dto.ProductPageDTO;
import com.cloud.product.module.entity.Product;

import java.util.List;

/**
 * 商品服务接口
 * 提供商品相关的业务操作，包括CRUD、分页查询、状态管理等
 * 使用多级缓存提升性能，遵循用户服务标�?
 *
 * @author what's up
 * @since 1.0.0
 */
public interface ProductService extends IService<Product> {

    // ================= 基础CRUD操作 =================

    /**
     * 创建商品
     *
     * @param requestDTO 商品创建请求DTO
     * @return 商品ID
     * @throws ProductServiceException 商品服务异常
     */
    Long createProduct(ProductRequestDTO requestDTO) throws ProductServiceException;

    /**
     * 更新商品
     *
     * @param id         商品ID
     * @param requestDTO 商品更新请求DTO
     * @return 是否更新成功
     * @throws ProductServiceException.ProductNotFoundException 商品不存在异�?
     * @throws ProductServiceException                          商品服务异常
     */
    Boolean updateProduct(Long id, ProductRequestDTO requestDTO) throws ProductServiceException.ProductNotFoundException, ProductServiceException;

    /**
     * 删除商品
     *
     * @param id 商品ID
     * @return 是否删除成功
     * @throws ProductServiceException.ProductNotFoundException 商品不存在异�?
     * @throws ProductServiceException                          商品服务异常
     */
    Boolean deleteProduct(Long id) throws ProductServiceException.ProductNotFoundException, ProductServiceException;

    /**
     * 批量删除商品
     *
     * @param ids 商品ID列表
     * @return 是否删除成功
     * @throws ProductServiceException 商品服务异常
     */
    Boolean batchDeleteProducts(List<Long> ids) throws ProductServiceException;

    // ================= 查询操作 =================

    /**
     * 根据ID获取商品详情
     *
     * @param id 商品ID
     * @return 商品VO
     * @throws ProductServiceException.ProductNotFoundException 商品不存在异�?
     */
    ProductVO getProductById(Long id) throws ProductServiceException.ProductNotFoundException;

    /**
     * 根据ID列表批量获取商品
     *
     * @param ids 商品ID列表
     * @return 商品VO列表
     */
    List<ProductVO> getProductsByIds(List<Long> ids);

    /**
     * 分页查询商品
     *
     * @param pageDTO 分页查询参数
     * @return 分页结果
     */
    PageResult<ProductVO> getProductsPage(ProductPageDTO pageDTO);

    /**
     * 根据分类ID获取商品列表
     *
     * @param categoryId 分类ID
     * @param status     商品状态，null表示查询所有状�?
     * @return 商品VO列表
     * @throws ProductServiceException.CategoryNotFoundException 分类不存在异�?
     */
    List<ProductVO> getProductsByCategoryId(Long categoryId, Integer status) throws ProductServiceException.CategoryNotFoundException;

    /**
     * 根据品牌ID获取商品列表
     *
     * @param brandId 品牌ID
     * @param status  商品状态，null表示查询所有状�?
     * @return 商品VO列表
     */
    List<ProductVO> getProductsByBrandId(Long brandId, Integer status);

    /**
     * 根据商品名称模糊查询
     *
     * @param name   商品名称关键�?
     * @param status 商品状态，null表示查询所有状�?
     * @return 商品VO列表
     */
    List<ProductVO> searchProductsByName(String name, Integer status);

    // ================= 状态管�?=================

    /**
     * 上架商品
     *
     * @param id 商品ID
     * @return 是否操作成功
     * @throws ProductServiceException.ProductNotFoundException 商品不存在异�?
     * @throws ProductServiceException.ProductStatusException   商品状态异�?
     * @throws ProductServiceException                          商品服务异常
     */
    Boolean enableProduct(Long id) throws ProductServiceException.ProductNotFoundException, ProductServiceException.ProductStatusException, ProductServiceException;

    /**
     * 下架商品
     *
     * @param id 商品ID
     * @return 是否操作成功
     * @throws ProductServiceException.ProductNotFoundException 商品不存在异�?
     * @throws ProductServiceException.ProductStatusException   商品状态异�?
     * @throws ProductServiceException                          商品服务异常
     */
    Boolean disableProduct(Long id) throws ProductServiceException.ProductNotFoundException, ProductServiceException.ProductStatusException, ProductServiceException;

    /**
     * 批量上架商品
     *
     * @param ids 商品ID列表
     * @return 是否操作成功
     * @throws ProductServiceException.ProductNotFoundException 商品不存在异�?
     * @throws ProductServiceException.ProductStatusException   商品状态异�?
     * @throws ProductServiceException                          商品服务异常
     */
    Boolean batchEnableProducts(List<Long> ids) throws ProductServiceException.ProductNotFoundException, ProductServiceException.ProductStatusException, ProductServiceException;

    /**
     * 批量下架商品
     *
     * @param ids 商品ID列表
     * @return 是否操作成功
     * @throws ProductServiceException.ProductNotFoundException 商品不存在异�?
     * @throws ProductServiceException.ProductStatusException   商品状态异�?
     * @throws ProductServiceException                          商品服务异常
     */
    Boolean batchDisableProducts(List<Long> ids) throws ProductServiceException.ProductNotFoundException, ProductServiceException.ProductStatusException, ProductServiceException;

    // ================= 库存管理 =================

    /**
     * 更新商品库存
     *
     * @param id    商品ID
     * @param stock 新库存数�?
     * @return 是否更新成功
     */
    Boolean updateStock(Long id, Integer stock);

    /**
     * 增加商品库存
     *
     * @param id     商品ID
     * @param amount 增加数量
     * @return 是否操作成功
     * @throws ProductServiceException.ProductNotFoundException 商品不存在异�?
     * @throws ProductServiceException                          商品服务异常
     */
    Boolean increaseStock(Long id, Integer amount) throws ProductServiceException.ProductNotFoundException, ProductServiceException;

    /**
     * 减少商品库存
     *
     * @param id     商品ID
     * @param amount 减少数量
     * @return 是否操作成功
     * @throws ProductServiceException.ProductNotFoundException 商品不存在异�?
     * @throws ProductServiceException                          商品服务异常
     */
    Boolean decreaseStock(Long id, Integer amount) throws ProductServiceException.ProductNotFoundException, ProductServiceException;

    /**
     * 检查商品库存是否充�?
     *
     * @param id     商品ID
     * @param amount 需要的数量
     * @return 是否充足
     * @throws ProductServiceException.ProductNotFoundException 商品不存在异�?
     * @throws ProductServiceException                          商品服务异常
     */
    Boolean checkStock(Long id, Integer amount) throws ProductServiceException.ProductNotFoundException, ProductServiceException;

    // ================= 统计分析 =================

    /**
     * 获取商品总数
     *
     * @return 商品总数
     */
    Long getTotalProductCount();

    /**
     * 获取上架商品数量
     *
     * @return 上架商品数量
     */
    Long getEnabledProductCount();

    /**
     * 获取下架商品数量
     *
     * @return 下架商品数量
     */
    Long getDisabledProductCount();

    /**
     * 根据分类统计商品数量
     *
     * @param categoryId 分类ID
     * @return 该分类下的商品数�?
     */
    Long getProductCountByCategoryId(Long categoryId);

    /**
     * 根据品牌统计商品数量
     *
     * @param brandId 品牌ID
     * @return 该品牌下的商品数�?
     */
    Long getProductCountByBrandId(Long brandId);

    // ================= 缓存管理 =================

    /**
     * 清除商品缓存
     *
     * @param id 商品ID
     */
    void evictProductCache(Long id);

    /**
     * 清除所有商品缓�?
     */
    void evictAllProductCache();

    /**
     * 预热商品缓存
     *
     * @param ids 需要预热的商品ID列表
     */
    void warmupProductCache(List<Long> ids);

    // ================= Feign客户端接口方�?=================

    /**
     * 创建商品（Feign客户端接口）
     *
     * @param productDTO 商品DTO
     * @return 创建的商品DTO
     */
    ProductDTO createProductForFeign(ProductDTO productDTO);

    /**
     * 根据ID获取商品（Feign客户端接口）
     *
     * @param id 商品ID
     * @return 商品DTO
     */
    ProductDTO getProductByIdForFeign(Long id);

    /**
     * 更新商品（Feign客户端接口）
     *
     * @param id         商品ID
     * @param productDTO 商品DTO
     * @return 更新后的商品DTO
     */
    ProductDTO updateProductForFeign(Long id, ProductDTO productDTO);

    /**
     * 获取所有商品（Feign客户端接口）
     *
     * @return 商品DTO列表
     */
    List<ProductDTO> getAllProducts();

    /**
     * 根据店铺ID获取商品列表（Feign客户端接口）
     *
     * @param shopId 店铺ID
     * @return 商品DTO列表
     */
    List<ProductDTO> getProductsByShopId(Long shopId);
}
