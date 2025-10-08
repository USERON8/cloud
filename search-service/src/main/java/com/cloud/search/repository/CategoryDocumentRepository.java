package com.cloud.search.repository;

import com.cloud.search.document.CategoryDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 分类文档Repository
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Repository
public interface CategoryDocumentRepository extends ElasticsearchRepository<CategoryDocument, String> {

    /**
     * 根据分类名称搜索
     *
     * @param name     分类名称
     * @param pageable 分页参数
     * @return 搜索结果
     */
    @Query("{\"bool\": {\"must\": [{\"match\": {\"name\": \"?0\"}}]}}")
    Page<CategoryDocument> findByNameContaining(String name, Pageable pageable);

    /**
     * 根据父分类ID查询子分类
     *
     * @param parentId 父分类ID
     * @param pageable 分页参数
     * @return 搜索结果
     */
    Page<CategoryDocument> findByParentId(Long parentId, Pageable pageable);

    /**
     * 根据分类级别查询
     *
     * @param level    分类级别
     * @param pageable 分页参数
     * @return 搜索结果
     */
    Page<CategoryDocument> findByLevel(Integer level, Pageable pageable);

    /**
     * 根据状态查询分类
     *
     * @param status   状态
     * @param pageable 分页参数
     * @return 搜索结果
     */
    Page<CategoryDocument> findByStatus(Integer status, Pageable pageable);

    /**
     * 查询根分类（一级分类）
     *
     * @param pageable 分页参数
     * @return 搜索结果
     */
    @Query("{\"bool\": {\"must\": [{\"term\": {\"level\": 1}}, {\"term\": {\"status\": 1}}]}}")
    Page<CategoryDocument> findRootCategories(Pageable pageable);

    /**
     * 根据父分类ID和状态查询子分类
     *
     * @param parentId 父分类ID
     * @param status   状态
     * @param pageable 分页参数
     * @return 搜索结果
     */
    @Query("{\"bool\": {\"must\": [{\"term\": {\"parentId\": ?0}}, {\"term\": {\"status\": ?1}}]}}")
    Page<CategoryDocument> findByParentIdAndStatus(Long parentId, Integer status, Pageable pageable);

    /**
     * 根据级别和状态查询分类
     *
     * @param level    分类级别
     * @param status   状态
     * @param pageable 分页参数
     * @return 搜索结果
     */
    @Query("{\"bool\": {\"must\": [{\"term\": {\"level\": ?0}}, {\"term\": {\"status\": ?1}}]}}")
    Page<CategoryDocument> findByLevelAndStatus(Integer level, Integer status, Pageable pageable);

    /**
     * 搜索分类（名称模糊匹配 + 状态过滤）
     *
     * @param name     分类名称关键字
     * @param status   状态
     * @param pageable 分页参数
     * @return 搜索结果
     */
    @Query("{\"bool\": {\"must\": [{\"match\": {\"name\": \"?0\"}}, {\"term\": {\"status\": ?1}}]}}")
    Page<CategoryDocument> searchByNameAndStatus(String name, Integer status, Pageable pageable);

    /**
     * 获取分类树（根据父分类ID获取所有子分类）
     *
     * @param parentId 父分类ID
     * @param status   状态
     * @return 分类列表
     */
    @Query("{\"bool\": {\"must\": [{\"term\": {\"parentId\": ?0}}, {\"term\": {\"status\": ?1}}]}}")
    List<CategoryDocument> findCategoryTree(Long parentId, Integer status);

    /**
     * 根据排序字段查询分类
     *
     * @param parentId 父分类ID
     * @param status   状态
     * @param pageable 分页参数（包含排序）
     * @return 搜索结果
     */
    @Query("{\"bool\": {\"must\": [{\"term\": {\"parentId\": ?0}}, {\"term\": {\"status\": ?1}}]}}")
    Page<CategoryDocument> findByParentIdAndStatusOrderBySortOrder(Long parentId, Integer status, Pageable pageable);

    /**
     * 高级搜索：名称 + 级别 + 状态
     *
     * @param name     分类名称
     * @param level    分类级别
     * @param status   状态
     * @param pageable 分页参数
     * @return 搜索结果
     */
    @Query("{\"bool\": {\"must\": [{\"match\": {\"name\": \"?0\"}}, {\"term\": {\"level\": ?1}}, {\"term\": {\"status\": ?2}}]}}")
    Page<CategoryDocument> advancedSearch(String name, Integer level, Integer status, Pageable pageable);
}
