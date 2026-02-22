package com.cloud.search.service.impl;

import com.cloud.search.document.ProductDocument;
import com.cloud.search.dto.ProductSearchRequest;
import com.cloud.search.dto.SearchResult;
import com.cloud.search.repository.ProductDocumentRepository;
import com.cloud.search.service.ElasticsearchOptimizedService;
import com.cloud.search.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;








@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchServiceImpl implements ProductSearchService {

    private static final String PROCESSED_EVENT_KEY_PREFIX = "search:processed:";
    private static final long PROCESSED_EVENT_TTL = 24 * 60 * 60; 
    private final ProductDocumentRepository productDocumentRepository;
    private final ElasticsearchOptimizedService elasticsearchOptimizedService;
    private final StringRedisTemplate redisTemplate;


    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "productSearchCache", allEntries = true),
                    @CacheEvict(cacheNames = "searchSuggestionCache", allEntries = true),
                    @CacheEvict(cacheNames = "hotSearchCache", allEntries = true),
                    @CacheEvict(cacheNames = "filterCache", allEntries = true)
            }
    )
    public void deleteProduct(Long productId) {
        try {
            

            productDocumentRepository.deleteById(String.valueOf(productId));

            

        } catch (Exception e) {
            log.error("鉂?浠嶦S鍒犻櫎鍟嗗搧澶辫触 - 鍟嗗搧ID: {}, 閿欒: {}",
                    productId, e.getMessage(), e);
            throw new RuntimeException("浠嶦S鍒犻櫎鍟嗗搧澶辫触", e);
        }
    }

    @Override
    public void updateProductStatus(Long productId, Integer status) {
        try {
            

            Optional<ProductDocument> optionalDoc = productDocumentRepository.findById(String.valueOf(productId));
            if (optionalDoc.isPresent()) {
                ProductDocument document = optionalDoc.get();
                document.setStatus(status);
                document.setUpdatedAt(LocalDateTime.now());
                productDocumentRepository.save(document);

                
            } else {
                log.warn("鈿狅笍 鍟嗗搧涓嶅瓨鍦紝鏃犳硶鏇存柊鐘舵€?- 鍟嗗搧ID: {}", productId);
            }

        } catch (Exception e) {
            log.error("鉂?鏇存柊鍟嗗搧鐘舵€佸け璐?- 鍟嗗搧ID: {}, 閿欒: {}",
                    productId, e.getMessage(), e);
            throw new RuntimeException("鏇存柊鍟嗗搧鐘舵€佸け璐?, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productSearchCache",
            key = "'product:' + #productId",
            condition = "#productId != null")
    public ProductDocument findByProductId(Long productId) {
        return productDocumentRepository.findById(String.valueOf(productId)).orElse(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "productSearchCache", allEntries = true),
                    @CacheEvict(cacheNames = "searchSuggestionCache", allEntries = true),
                    @CacheEvict(cacheNames = "hotSearchCache", allEntries = true),
                    @CacheEvict(cacheNames = "filterCache", allEntries = true),
                    @CacheEvict(cacheNames = "aggregationCache", allEntries = true)
            }
    )

    public void batchDeleteProducts(List<Long> productIds) {
        try {
            

            List<String> ids = productIds.stream()
                    .map(String::valueOf)
                    .toList();

            productDocumentRepository.deleteAllById(ids);

            

        } catch (Exception e) {
            log.error("鉂?鎵归噺鍒犻櫎鍟嗗搧浠嶦S澶辫触 - 閿欒: {}", e.getMessage(), e);
            throw new RuntimeException("鎵归噺鍒犻櫎鍟嗗搧浠嶦S澶辫触", e);
        }
    }

    @Override
    public boolean isEventProcessed(String traceId) {
        try {
            String key = PROCESSED_EVENT_KEY_PREFIX + traceId;
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.warn("妫€鏌ヤ簨浠跺鐞嗙姸鎬佸け璐?- TraceId: {}, 閿欒: {}", traceId, e.getMessage());
            return false;
        }
    }

    @Override
    public void markEventProcessed(String traceId) {
        try {
            String key = PROCESSED_EVENT_KEY_PREFIX + traceId;
            redisTemplate.opsForValue().set(key, "1", PROCESSED_EVENT_TTL, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("鏍囪浜嬩欢宸插鐞嗗け璐?- TraceId: {}, 閿欒: {}", traceId, e.getMessage());
        }
    }

    @Override
    public void rebuildProductIndex() {
        try {
            

        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productSearchCache",
            key = "'category:' + #categoryId + ':' + #page + ':' + #size")
    public SearchResult<ProductDocument> searchByCategory(Long categoryId, Integer page, Integer size) {
        try {
            
            long startTime = System.currentTimeMillis();

            int pageNum = page != null ? page : 0;
            int pageSize = size != null ? size : 20;
            Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "hotScore"));

            Page<ProductDocument> pageResult = productDocumentRepository.findByCategoryIdAndStatus(categoryId, 1, pageable);

            long took = System.currentTimeMillis() - startTime;
            SearchResult<ProductDocument> result = convertToSearchResult(pageResult, took);

            
            return result;

        } catch (Exception e) {
            log.error("鎸夊垎绫绘悳绱㈠け璐?- 閿欒: {}", e.getMessage(), e);
            throw new RuntimeException("鎸夊垎绫绘悳绱㈠け璐?, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productSearchCache",
            key = "'brand:' + #brandId + ':' + #page + ':' + #size")
    public SearchResult<ProductDocument> searchByBrand(Long brandId, Integer page, Integer size) {
        try {
            
            long startTime = System.currentTimeMillis();

            int pageNum = page != null ? page : 0;
            int pageSize = size != null ? size : 20;
            Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "hotScore"));

            Page<ProductDocument> pageResult = productDocumentRepository.findByBrandIdAndStatus(brandId, 1, pageable);

            long took = System.currentTimeMillis() - startTime;
            SearchResult<ProductDocument> result = convertToSearchResult(pageResult, took);

            
            return result;

        } catch (Exception e) {
            log.error("鎸夊搧鐗屾悳绱㈠け璐?- 閿欒: {}", e.getMessage(), e);
            throw new RuntimeException("鎸夊搧鐗屾悳绱㈠け璐?, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productSearchCache",
            key = "'price:' + #minPrice + ':' + #maxPrice + ':' + #page + ':' + #size")
    public SearchResult<ProductDocument> searchByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Integer page, Integer size) {
        try {
            
            long startTime = System.currentTimeMillis();

            BigDecimal min = minPrice != null ? minPrice : BigDecimal.ZERO;
            BigDecimal max = maxPrice != null ? maxPrice : new BigDecimal("999999.99");

            int pageNum = page != null ? page : 0;
            int pageSize = size != null ? size : 20;
            Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "hotScore"));

            Page<ProductDocument> pageResult = productDocumentRepository.findByPriceBetweenAndStatus(min, max, 1, pageable);

            long took = System.currentTimeMillis() - startTime;
            SearchResult<ProductDocument> result = convertToSearchResult(pageResult, took);

            
            return result;

        } catch (Exception e) {
            log.error("鎸変环鏍煎尯闂存悳绱㈠け璐?- 閿欒: {}", e.getMessage(), e);
            throw new RuntimeException("鎸変环鏍煎尯闂存悳绱㈠け璐?, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productSearchCache",
            key = "'shop:' + #shopId + ':' + #page + ':' + #size")
    public SearchResult<ProductDocument> searchByShop(Long shopId, Integer page, Integer size) {
        try {
            
            long startTime = System.currentTimeMillis();

            int pageNum = page != null ? page : 0;
            int pageSize = size != null ? size : 20;
            Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "hotScore"));

            Page<ProductDocument> pageResult = productDocumentRepository.findByShopIdAndStatus(shopId, 1, pageable);

            long took = System.currentTimeMillis() - startTime;
            SearchResult<ProductDocument> result = convertToSearchResult(pageResult, took);

            
            return result;

        } catch (Exception e) {
            log.error("鎸夊簵閾烘悳绱㈠け璐?- 閿欒: {}", e.getMessage(), e);
            throw new RuntimeException("鎸夊簵閾烘悳绱㈠け璐?, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productSearchCache",
            key = "'combined:' + #keyword + ':' + #categoryId + ':' + #brandId + ':' + #minPrice + ':' + #maxPrice + ':' + #shopId + ':' + #page + ':' + #size")
    public SearchResult<ProductDocument> combinedSearch(String keyword, Long categoryId, Long brandId,
                                                        BigDecimal minPrice, BigDecimal maxPrice, Long shopId,
                                                        String sortBy, String sortOrder, Integer page, Integer size) {
        try {
            

            long startTime = System.currentTimeMillis();

            int pageNum = page != null ? page : 0;
            int pageSize = size != null ? size : 20;

            Sort.Direction direction = "asc".equalsIgnoreCase(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;
            String sortField = StringUtils.hasText(sortBy) ? sortBy : "hotScore";
            Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(direction, sortField));

            Page<ProductDocument> pageResult = productDocumentRepository.combinedSearch(
                    keyword,
                    categoryId,
                    brandId,
                    shopId,
                    minPrice,
                    maxPrice,
                    1,
                    pageable
            );

            long took = System.currentTimeMillis() - startTime;
            SearchResult<ProductDocument> result = convertToSearchResult(pageResult, took);

            
            return result;

        } catch (Exception e) {
            log.error("缁勫悎鎼滅储澶辫触 - 閿欒: {}", e.getMessage(), e);
            throw new RuntimeException("缁勫悎鎼滅储澶辫触", e);
        }
    }

    


    private SearchResult<ProductDocument> convertToSearchResult(Page<ProductDocument> page, long took) {
        return SearchResult.<ProductDocument>builder()
                .list(page.getContent())
                .total(page.getTotalElements())
                .page(page.getNumber())
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .took(took)
                .aggregations(null)
                .highlights(null)
                .build();
    }

}
