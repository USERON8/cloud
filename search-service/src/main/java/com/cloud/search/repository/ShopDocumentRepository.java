package com.cloud.search.repository;

import com.cloud.search.document.ShopDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

/**
 * 店铺文档Repository
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Repository
public interface ShopDocumentRepository extends ElasticsearchRepository<ShopDocument, String> {

    /**
     * 根据店铺名称搜索
     *
     * @param shopName 店铺名称
     * @param pageable 分页参数
     * @return 搜索结果
     */
    @Query("{\"bool\": {\"must\": [{\"match\": {\"shopName\": \"?0\"}}]}}")
    Page<ShopDocument> findByShopNameContaining(String shopName, Pageable pageable);

    /**
     * 根据商家ID查询店铺
     *
     * @param merchantId 商家ID
     * @param pageable   分页参数
     * @return 搜索结果
     */
    Page<ShopDocument> findByMerchantId(Long merchantId, Pageable pageable);

    /**
     * 根据状态查询店铺
     *
     * @param status   状态
     * @param pageable 分页参数
     * @return 搜索结果
     */
    Page<ShopDocument> findByStatus(Integer status, Pageable pageable);

    /**
     * 查询推荐店铺
     *
     * @param recommended 是否推荐
     * @param pageable    分页参数
     * @return 搜索结果
     */
    Page<ShopDocument> findByRecommended(Boolean recommended, Pageable pageable);

    /**
     * 根据评分范围查询店铺
     *
     * @param minRating 最低评分
     * @param maxRating 最高评分
     * @param pageable  分页参数
     * @return 搜索结果
     */
    @Query("{\"bool\": {\"must\": [{\"range\": {\"rating\": {\"gte\": ?0, \"lte\": ?1}}}]}}")
    Page<ShopDocument> findByRatingBetween(BigDecimal minRating, BigDecimal maxRating, Pageable pageable);

    /**
     * 根据地址关键字搜索店铺
     *
     * @param address  地址关键字
     * @param pageable 分页参数
     * @return 搜索结果
     */
    @Query("{\"bool\": {\"must\": [{\"match\": {\"address\": \"?0\"}}]}}")
    Page<ShopDocument> findByAddressContaining(String address, Pageable pageable);

    /**
     * 复合搜索：关键字 + 状态
     *
     * @param keyword  关键字
     * @param status   状态
     * @param pageable 分页参数
     * @return 搜索结果
     */
    @Query("{\"bool\": {\"must\": [{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"shopName\", \"description\", \"address\"]}}, {\"term\": {\"status\": ?1}}]}}")
    Page<ShopDocument> searchByKeywordAndStatus(String keyword, Integer status, Pageable pageable);

    /**
     * 复合搜索：商家ID + 状态
     *
     * @param merchantId 商家ID
     * @param status     状态
     * @param pageable   分页参数
     * @return 搜索结果
     */
    @Query("{\"bool\": {\"must\": [{\"term\": {\"merchantId\": ?0}}, {\"term\": {\"status\": ?1}}]}}")
    Page<ShopDocument> findByMerchantIdAndStatus(Long merchantId, Integer status, Pageable pageable);

    /**
     * 高级搜索：关键字 + 评分范围 + 状态
     *
     * @param keyword   关键字
     * @param minRating 最低评分
     * @param status    状态
     * @param pageable  分页参数
     * @return 搜索结果
     */
    @Query("{\"bool\": {\"must\": [{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"shopName\", \"description\", \"address\"]}}, {\"range\": {\"rating\": {\"gte\": ?1}}}, {\"term\": {\"status\": ?2}}]}}")
    Page<ShopDocument> advancedSearch(String keyword, BigDecimal minRating, Integer status, Pageable pageable);
}
