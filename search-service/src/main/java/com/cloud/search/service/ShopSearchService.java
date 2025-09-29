package com.cloud.search.service;

import com.cloud.common.domain.event.product.ShopSearchEvent;
import com.cloud.search.document.ShopDocument;
import com.cloud.search.dto.SearchResult;
import com.cloud.search.dto.ShopSearchRequest;

import java.util.List;

/**
 * 店铺搜索服务接口
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
public interface ShopSearchService {

    /**
     * 保存或更新店铺到Elasticsearch
     *
     * @param event 店铺搜索事件
     */
    void saveOrUpdateShop(ShopSearchEvent event);

    /**
     * 删除店铺从Elasticsearch
     *
     * @param shopId 店铺ID
     */
    void deleteShop(Long shopId);

    /**
     * 更新店铺状态
     *
     * @param shopId 店铺ID
     * @param status 状态
     */
    void updateShopStatus(Long shopId, Integer status);

    /**
     * 根据店铺ID查询店铺文档
     *
     * @param shopId 店铺ID
     * @return 店铺文档
     */
    ShopDocument findByShopId(Long shopId);

    /**
     * 批量保存店铺
     *
     * @param events 店铺事件列表
     */
    void batchSaveShops(List<ShopSearchEvent> events);

    /**
     * 批量删除店铺
     *
     * @param shopIds 店铺ID列表
     */
    void batchDeleteShops(List<Long> shopIds);

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
     * 重建店铺索引
     */
    void rebuildShopIndex();

    /**
     * 检查索引是否存在
     *
     * @return 是否存在
     */
    boolean indexExists();

    /**
     * 创建店铺索引
     */
    void createShopIndex();

    /**
     * 删除店铺索引
     */
    void deleteShopIndex();

    /**
     * 复杂店铺搜索
     *
     * @param request 搜索请求参数
     * @return 搜索结果
     */
    SearchResult<ShopDocument> searchShops(ShopSearchRequest request);

    /**
     * 获取搜索建议
     *
     * @param keyword 关键字
     * @param size    建议数量
     * @return 建议列表
     */
    List<String> getSearchSuggestions(String keyword, Integer size);

    /**
     * 获取热门店铺
     *
     * @param size 数量
     * @return 热门店铺列表
     */
    List<ShopDocument> getHotShops(Integer size);

    /**
     * 获取店铺筛选聚合信息
     *
     * @param request 搜索请求参数
     * @return 聚合信息
     */
    SearchResult<ShopDocument> getShopFilters(ShopSearchRequest request);
}
