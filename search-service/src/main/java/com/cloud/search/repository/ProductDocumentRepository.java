package com.cloud.search.repository;

import com.cloud.search.document.ProductDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;








@Repository
public interface ProductDocumentRepository extends ElasticsearchRepository<ProductDocument, String> {

    


    Page<ProductDocument> findByProductNameContaining(String productName, Pageable pageable);

    


    Page<ProductDocument> findByCategoryId(Long categoryId, Pageable pageable);

    


    Page<ProductDocument> findByShopId(Long shopId, Pageable pageable);

    


    Page<ProductDocument> findByStatus(Integer status, Pageable pageable);

    


    Page<ProductDocument> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    


    Page<ProductDocument> findByRecommendedTrue(Pageable pageable);

    


    Page<ProductDocument> findByIsNewTrue(Pageable pageable);

    


    Page<ProductDocument> findByIsHotTrue(Pageable pageable);

    


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

    


    Page<ProductDocument> findByCategoryIdAndStatus(Long categoryId, Integer status, Pageable pageable);

    


    Page<ProductDocument> findByBrandIdAndStatus(Long brandId, Integer status, Pageable pageable);

    


    Page<ProductDocument> findByShopIdAndStatus(Long shopId, Integer status, Pageable pageable);

    


    Page<ProductDocument> findByPriceBetweenAndStatus(BigDecimal minPrice, BigDecimal maxPrice, Integer status, Pageable pageable);

    


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
