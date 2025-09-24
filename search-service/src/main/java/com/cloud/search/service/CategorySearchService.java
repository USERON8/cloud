package com.cloud.search.service;

import com.cloud.common.domain.event.CategorySearchEvent;
import com.cloud.search.document.CategoryDocument;

import java.util.List;

/**
 * 分类搜索服务接口
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
public interface CategorySearchService {

    /**
     * 保存或更新分类到Elasticsearch
     *
     * @param event 分类搜索事件
     */
    void saveOrUpdateCategory(CategorySearchEvent event);

    /**
     * 删除分类从Elasticsearch
     *
     * @param categoryId 分类ID
     */
    void deleteCategory(Long categoryId);

    /**
     * 更新分类状态
     *
     * @param categoryId 分类ID
     * @param status     状态
     */
    void updateCategoryStatus(Long categoryId, Integer status);

    /**
     * 根据分类ID查询分类文档
     *
     * @param categoryId 分类ID
     * @return 分类文档
     */
    CategoryDocument findByCategoryId(Long categoryId);

    /**
     * 批量保存分类
     *
     * @param events 分类事件列表
     */
    void batchSaveCategories(List<CategorySearchEvent> events);

    /**
     * 批量删除分类
     *
     * @param categoryIds 分类ID列表
     */
    void batchDeleteCategories(List<Long> categoryIds);

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
     * 重建分类索引
     */
    void rebuildCategoryIndex();

    /**
     * 检查索引是否存在
     *
     * @return 是否存在
     */
    boolean indexExists();

    /**
     * 创建分类索引
     */
    void createCategoryIndex();

    /**
     * 删除分类索引
     */
    void deleteCategoryIndex();
}
