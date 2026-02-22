package com.cloud.search.repository;

import com.cloud.search.document.ShopDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;








@Repository
public interface ShopDocumentRepository extends ElasticsearchRepository<ShopDocument, String> {

    






    @Query("{\"bool\": {\"must\": [{\"match\": {\"shopName\": \"?0\"}}]}}")
    Page<ShopDocument> findByShopNameContaining(String shopName, Pageable pageable);

    






    Page<ShopDocument> findByMerchantId(Long merchantId, Pageable pageable);

    






    Page<ShopDocument> findByStatus(Integer status, Pageable pageable);

    






    Page<ShopDocument> findByRecommended(Boolean recommended, Pageable pageable);

    







    @Query("{\"bool\": {\"must\": [{\"range\": {\"rating\": {\"gte\": ?0, \"lte\": ?1}}}]}}")
    Page<ShopDocument> findByRatingBetween(BigDecimal minRating, BigDecimal maxRating, Pageable pageable);

    






    @Query("{\"bool\": {\"must\": [{\"match\": {\"address\": \"?0\"}}]}}")
    Page<ShopDocument> findByAddressContaining(String address, Pageable pageable);

    







    @Query("{\"bool\": {\"must\": [{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"shopName\", \"description\", \"address\"]}}, {\"term\": {\"status\": ?1}}]}}")
    Page<ShopDocument> searchByKeywordAndStatus(String keyword, Integer status, Pageable pageable);

    







    @Query("{\"bool\": {\"must\": [{\"term\": {\"merchantId\": ?0}}, {\"term\": {\"status\": ?1}}]}}")
    Page<ShopDocument> findByMerchantIdAndStatus(Long merchantId, Integer status, Pageable pageable);

    








    @Query("{\"bool\": {\"must\": [{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"shopName\", \"description\", \"address\"]}}, {\"range\": {\"rating\": {\"gte\": ?1}}}, {\"term\": {\"status\": ?2}}]}}")
    Page<ShopDocument> advancedSearch(String keyword, BigDecimal minRating, Integer status, Pageable pageable);
}
