package com.cloud.search.service;


import com.cloud.search.document.ProductDocument;
import com.cloud.search.dto.ProductSearchRequest;
import com.cloud.search.dto.SearchResult;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品搜索服务接口
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
public interface ProductSearchService {

    /**
     * 删除商品从Elasticsearch
     *
     * @param productId 商品ID
     */
    void deleteProduct(Long productId);

    /**
     * 更新商品状态
     *
     * @param productId 商品ID
     * @param status    状态
     */
    void updateProductStatus(Long productId, Integer status);

    /**
     * 根据商品ID查询商品文档
     *
     * @param productId 商品ID
     * @return 商品文档
     */
    ProductDocument findByProductId(Long productId);

    /**
     * 批量删除商品
     *
     * @param productIds 商品ID列表
     */
    void batchDeleteProducts(List<Long> productIds);

    /**
     * 检查事件是否已处理（幂等性检查）
     *
     * @param traceId 追踪ID
     * @return 是否已处理
     */
    boolean isEventProcessed(String traceId);

    /**
     * 标记事件已处理
     *
     * @param traceId 追踪ID
     */
    void markEventProcessed(String traceId);

    /**
     * 重建商品索引
     */
    void rebuildProductIndex();

    /**
     * 检查索引是否存在
     *
     * @return 是否存在
     */
    boolean indexExists();

    /**
     * 创建商品索引
     */
    void createProductIndex();

    /**
     * 删除商品索引
     */
    void deleteProductIndex();

    /**
     * 复杂商品搜索
     *
     * @param request 搜索请求参数
     * @return 搜索结果
     */
    SearchResult<ProductDocument> searchProducts(ProductSearchRequest request);

    /**
     * 获取搜索建议
     *
     * @param keyword 关键字
     * @param size    建议数量
     * @return 建议列表
     */
    List<String> getSearchSuggestions(String keyword, Integer size);

    /**
     * 获取热门搜索关键字
     *
     * @param size 数量
     * @return 热门关键字列表
     */
    List<String> getHotSearchKeywords(Integer size);

    /**
     * 获取商品筛选聚合信息
     *
     * @param request 搜索请求参数
     * @return 聚合信息
     */
    SearchResult<ProductDocument> getProductFilters(ProductSearchRequest request);

    /**
     * 基础搜索 - 根据关键字搜索商品
     *
     * @param keyword 搜索关键字
     * @param page    页码
     * @param size    每页大小
     * @return 搜索结果
     */
    SearchResult<ProductDocument> basicSearch(String keyword, Integer page, Integer size);

    /**
     * 筛选搜索 - 支持多条件筛选
     *
     * @param request 筛选请求参数
     * @return 搜索结果
     */
    SearchResult<ProductDocument> filterSearch(ProductSearchRequest request);

    /**
     * 分类筛选
     *
     * @param categoryId 分类ID
     * @param page       页码
     * @param size       每页大小
     * @return 搜索结果
     */
    SearchResult<ProductDocument> searchByCategory(Long categoryId, Integer page, Integer size);

    /**
     * 品牌筛选
     *
     * @param brandId 品牌ID
     * @param page    页码
     * @param size    每页大小
     * @return 搜索结果
     */
    SearchResult<ProductDocument> searchByBrand(Long brandId, Integer page, Integer size);

    /**
     * 价格区间筛选
     *
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @param page     页码
     * @param size     每页大小
     * @return 搜索结果
     */
    SearchResult<ProductDocument> searchByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Integer page, Integer size);

    /**
     * 店铺筛选
     *
     * @param shopId 店铺ID
     * @param page   页码
     * @param size   每页大小
     * @return 搜索结果
     */
    SearchResult<ProductDocument> searchByShop(Long shopId, Integer page, Integer size);

    /**
     * 组合筛选 - 支持关键字+分类+品牌+价格区间等多条件组合
     *
     * @param keyword    搜索关键字
     * @param categoryId 分类ID
     * @param brandId    品牌ID
     * @param minPrice   最低价格
     * @param maxPrice   最高价格
     * @param shopId     店铺ID
     * @param sortBy     排序字段
     * @param sortOrder  排序方式
     * @param page       页码
     * @param size       每页大小
     * @return 搜索结果
     */
    SearchResult<ProductDocument> combinedSearch(String keyword, Long categoryId, Long brandId,
                                                  BigDecimal minPrice, BigDecimal maxPrice, Long shopId,
                                                  String sortBy, String sortOrder, Integer page, Integer size);
}
