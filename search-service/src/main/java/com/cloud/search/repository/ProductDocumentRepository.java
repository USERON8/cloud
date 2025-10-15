package com.cloud.search.repository;

import com.cloud.search.document.ProductDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品文档Repository
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Repository
public interface ProductDocumentRepository extends ElasticsearchRepository<ProductDocument, String> {

    /**
     * 根据商品名称搜索
     */
    Page<ProductDocument> findByProductNameContaining(String productName, Pageable pageable);

    /**
     * 根据分类ID查询商品
     */
    Page<ProductDocument> findByCategoryId(Long categoryId, Pageable pageable);

    /**
     * 根据店铺ID查询商品
     */
    Page<ProductDocument> findByShopId(Long shopId, Pageable pageable);

    /**
     * 根据状态查询商品
     */
    Page<ProductDocument> findByStatus(Integer status, Pageable pageable);

    /**
     * 根据价格区间查询商品
     */
    Page<ProductDocument> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    /**
     * 查询推荐商品
     */
    Page<ProductDocument> findByRecommendedTrue(Pageable pageable);

    /**
     * 查询新品
     */
    Page<ProductDocument> findByIsNewTrue(Pageable pageable);

    /**
     * 查询热销商品
     */
    Page<ProductDocument> findByIsHotTrue(Pageable pageable);

    /**
     * 复杂搜索查询
     */
    @Query("""
            {
              "bool": {
                "should": [
                  {
                    "multi_match": {
                      "query": "?0",
                      "fields": ["productName^3", "description^1", "tags^2"],
                      "type": "best_fields",
                      "fuzziness": "AUTO"
                    }
                  },
                  {
                    "match": {
                      "productName.pinyin": "?0"
                    }
                  }
                ],
                "filter": [
                  {
                    "term": {
                      "status": 1
                    }
                  }
                ]
              }
            }
            """)
    Page<ProductDocument> searchByKeyword(String keyword, Pageable pageable);

    /**
     * 根据分类和关键词搜索
     */
    @Query("""
            {
              "bool": {
                "must": [
                  {
                    "multi_match": {
                      "query": "?0",
                      "fields": ["productName^3", "description^1", "tags^2"],
                      "type": "best_fields"
                    }
                  }
                ],
                "filter": [
                  {
                    "term": {
                      "categoryId": "?1"
                    }
                  },
                  {
                    "term": {
                      "status": 1
                    }
                  }
                ]
              }
            }
            """)
    Page<ProductDocument> searchByKeywordAndCategory(String keyword, Long categoryId, Pageable pageable);

    /**
     * 根据店铺和关键词搜索
     */
    @Query("""
            {
              "bool": {
                "must": [
                  {
                    "multi_match": {
                      "query": "?0",
                      "fields": ["productName^3", "description^1", "tags^2"],
                      "type": "best_fields"
                    }
                  }
                ],
                "filter": [
                  {
                    "term": {
                      "shopId": "?1"
                    }
                  },
                  {
                    "term": {
                      "status": 1
                    }
                  }
                ]
              }
            }
            """)
    Page<ProductDocument> searchByKeywordAndShop(String keyword, Long shopId, Pageable pageable);

    /**
     * 高级搜索 - 支持多条件组合
     */
    @Query("""
            {
              "bool": {
                "must": [
                  {
                    "multi_match": {
                      "query": "?0",
                      "fields": ["productName^3", "description^1", "tags^2"],
                      "type": "best_fields"
                    }
                  }
                ],
                "filter": [
                  {
                    "term": {
                      "status": 1
                    }
                  },
                  {
                    "range": {
                      "price": {
                        "gte": "?1",
                        "lte": "?2"
                      }
                    }
                  }
                ]
              }
            }
            """)
    Page<ProductDocument> advancedSearch(String keyword, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    /**
     * 获取热门搜索建议
     */
    @Query("""
            {
              "bool": {
                "should": [
                  {
                    "prefix": {
                      "productName": "?0"
                    }
                  },
                  {
                    "prefix": {
                      "productName.pinyin": "?0"
                    }
                  }
                ],
                "filter": [
                  {
                    "term": {
                      "status": 1
                    }
                  }
                ]
              }
            }
            """)
    List<ProductDocument> findSuggestions(String prefix);

    /**
     * 根据分类ID和状态查询商品
     */
    Page<ProductDocument> findByCategoryIdAndStatus(Long categoryId, Integer status, Pageable pageable);

    /**
     * 根据品牌ID和状态查询商品
     */
    Page<ProductDocument> findByBrandIdAndStatus(Long brandId, Integer status, Pageable pageable);

    /**
     * 根据店铺ID和状态查询商品
     */
    Page<ProductDocument> findByShopIdAndStatus(Long shopId, Integer status, Pageable pageable);

    /**
     * 根据价格区间和状态查询商品
     */
    Page<ProductDocument> findByPriceBetweenAndStatus(BigDecimal minPrice, BigDecimal maxPrice, Integer status, Pageable pageable);

    /**
     * 多条件筛选搜索
     */
    @Query("""
            {
              "bool": {
                "must": [
                  #{#keyword != null && !#keyword.isEmpty() ? '{"multi_match": {"query": "' + #keyword + '", "fields": ["productName^3", "description^1", "tags^2"], "type": "best_fields"}}' : '{"match_all": {}}'}
                ],
                "filter": [
                  {"term": {"status": "?7"}},
                  #{#categoryId != null ? '{"term": {"categoryId": ' + #categoryId + '}}' : ''},
                  #{#brandId != null ? '{"term": {"brandId": ' + #brandId + '}}' : ''},
                  #{#shopId != null ? '{"term": {"shopId": ' + #shopId + '}}' : ''},
                  #{#minPrice != null ? '{"range": {"price": {"gte": ' + #minPrice + '}}}' : ''},
                  #{#maxPrice != null ? '{"range": {"price": {"lte": ' + #maxPrice + '}}}' : ''},
                  #{#minSalesCount != null ? '{"range": {"salesCount": {"gte": ' + #minSalesCount + '}}}' : ''}
                ]
              }
            }
            """)
    Page<ProductDocument> filterSearch(String keyword, Long categoryId, Long brandId, Long shopId,
                                        BigDecimal minPrice, BigDecimal maxPrice, Integer minSalesCount,
                                        Integer status, Pageable pageable);

    /**
     * 组合搜索 - 支持所有条件的组合查询
     */
    @Query("""
            {
              "bool": {
                "must": [
                  #{#keyword != null && !#keyword.isEmpty() ? '{"multi_match": {"query": "' + #keyword + '", "fields": ["productName^3", "description^1", "tags^2"], "type": "best_fields", "fuzziness": "AUTO"}}' : '{"match_all": {}}'}
                ],
                "filter": [
                  {"term": {"status": "?6"}},
                  #{#categoryId != null ? '{"term": {"categoryId": ' + #categoryId + '}}' : ''},
                  #{#brandId != null ? '{"term": {"brandId": ' + #brandId + '}}' : ''},
                  #{#shopId != null ? '{"term": {"shopId": ' + #shopId + '}}' : ''},
                  #{#minPrice != null ? '{"range": {"price": {"gte": ' + #minPrice + '}}}' : ''},
                  #{#maxPrice != null ? '{"range": {"price": {"lte": ' + #maxPrice + '}}}' : ''}
                ]
              }
            }
            """)
    Page<ProductDocument> combinedSearch(String keyword, Long categoryId, Long brandId, Long shopId,
                                          BigDecimal minPrice, BigDecimal maxPrice, Integer status, Pageable pageable);
}
