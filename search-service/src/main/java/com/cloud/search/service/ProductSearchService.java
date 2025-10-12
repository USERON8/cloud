package com.cloud.search.service;


import com.cloud.search.document.ProductDocument;
import com.cloud.search.dto.ProductSearchRequest;
import com.cloud.search.dto.SearchResult;

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
}
