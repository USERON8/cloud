package com.cloud.search.service.impl;

import com.cloud.search.document.ProductDocument;
import com.cloud.search.dto.ProductSearchRequest;
import com.cloud.search.dto.SearchResult;
import com.cloud.search.repository.ProductDocumentRepository;
import com.cloud.search.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchServiceImpl implements ProductSearchService {

    private static final String PROCESSED_EVENT_KEY_PREFIX = "search:processed:";
    private static final String HOT_SEARCH_ZSET_KEY = "search:hot:zset";
    private static final long PROCESSED_EVENT_TTL_SECONDS = 24 * 60 * 60;

    private final ProductDocumentRepository productDocumentRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final StringRedisTemplate redisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void upsertProduct(ProductDocument productDocument) {
        if (productDocument == null || productDocument.getProductId() == null) {
            return;
        }
        try {
            if (productDocument.getId() == null || productDocument.getId().isBlank()) {
                productDocument.setId(String.valueOf(productDocument.getProductId()));
            }
            productDocumentRepository.save(productDocument);
        } catch (Exception e) {
            throw new RuntimeException("Upsert product into index failed", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long productId) {
        try {
            productDocumentRepository.deleteById(String.valueOf(productId));
        } catch (Exception e) {
            throw new RuntimeException("Delete product from index failed", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProductStatus(Long productId, Integer status) {
        try {
            ProductDocument document = findByProductId(productId);
            if (document == null) {
                return;
            }
            document.setStatus(status);
            productDocumentRepository.save(document);
        } catch (Exception e) {
            throw new RuntimeException("Update product status in index failed", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDocument findByProductId(Long productId) {
        return productDocumentRepository.findById(String.valueOf(productId)).orElse(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteProducts(List<Long> productIds) {
        try {
            List<String> ids = productIds == null ? Collections.emptyList()
                    : productIds.stream().map(String::valueOf).toList();
            if (!ids.isEmpty()) {
                productDocumentRepository.deleteAllById(ids);
            }
        } catch (Exception e) {
            throw new RuntimeException("Batch delete products from index failed", e);
        }
    }

    @Override
    public boolean isEventProcessed(String traceId) {
        if (!StringUtils.hasText(traceId)) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(PROCESSED_EVENT_KEY_PREFIX + traceId));
        } catch (Exception e) {
            log.warn("Check processed event failed: traceId={}", traceId, e);
            return false;
        }
    }

    @Override
    public void markEventProcessed(String traceId) {
        if (!StringUtils.hasText(traceId)) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(
                    PROCESSED_EVENT_KEY_PREFIX + traceId,
                    "1",
                    PROCESSED_EVENT_TTL_SECONDS,
                    TimeUnit.SECONDS
            );
        } catch (Exception e) {
            log.warn("Mark event processed failed: traceId={}", traceId, e);
        }
    }

    @Override
    public void rebuildProductIndex() {
        if (indexExists()) {
            deleteProductIndex();
        }
        createProductIndex();
    }

    @Override
    public boolean indexExists() {
        try {
            return elasticsearchOperations.indexOps(ProductDocument.class).exists();
        } catch (Exception e) {
            log.error("Check product index existence failed", e);
            return false;
        }
    }

    @Override
    public void createProductIndex() {
        try {
            elasticsearchOperations.indexOps(ProductDocument.class).create();
            elasticsearchOperations.indexOps(ProductDocument.class).putMapping();
        } catch (Exception e) {
            throw new RuntimeException("Create product index failed", e);
        }
    }

    @Override
    public void deleteProductIndex() {
        try {
            elasticsearchOperations.indexOps(ProductDocument.class).delete();
        } catch (Exception e) {
            throw new RuntimeException("Delete product index failed", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResult<ProductDocument> searchProducts(ProductSearchRequest request) {
        ProductSearchRequest safeRequest = request == null ? new ProductSearchRequest() : request;
        long start = System.currentTimeMillis();

        int pageNum = normalizePage(safeRequest.getPage());
        int pageSize = normalizeSize(safeRequest.getSize());
        int status = safeRequest.getStatus() != null ? safeRequest.getStatus() : 1;

        Pageable pageable = PageRequest.of(pageNum, pageSize, buildSort(safeRequest.getSortBy(), safeRequest.getSortOrder()));

        Page<ProductDocument> page = productDocumentRepository.combinedSearch(
                safeRequest.getKeyword(),
                safeRequest.getCategoryId(),
                safeRequest.getBrandId(),
                safeRequest.getShopId(),
                safeRequest.getMinPrice(),
                safeRequest.getMaxPrice(),
                status,
                pageable
        );

        if (StringUtils.hasText(safeRequest.getKeyword())) {
            recordHotSearch(safeRequest.getKeyword());
        }

        long took = System.currentTimeMillis() - start;
        return SearchResult.of(page.getContent(), page.getTotalElements(), page.getNumber(), page.getSize(), took);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getSearchSuggestions(String keyword, Integer size) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }
        int limit = size == null || size <= 0 ? 10 : Math.min(size, 50);

        try {
            List<ProductDocument> docs = productDocumentRepository.findSuggestions(keyword);
            if (docs == null || docs.isEmpty()) {
                return Collections.emptyList();
            }
            return docs.stream()
                    .map(ProductDocument::getProductName)
                    .filter(StringUtils::hasText)
                    .distinct()
                    .limit(limit)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Get search suggestions failed", e);
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getHotSearchKeywords(Integer size) {
        int limit = size == null || size <= 0 ? 10 : Math.min(size, 100);
        try {
            var keywords = redisTemplate.opsForZSet().reverseRange(HOT_SEARCH_ZSET_KEY, 0, limit - 1L);
            if (keywords == null || keywords.isEmpty()) {
                return Collections.emptyList();
            }
            return keywords.stream()
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Get hot search keywords failed", e);
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResult<ProductDocument> getProductFilters(ProductSearchRequest request) {
        SearchResult<ProductDocument> base = searchProducts(request);
        List<ProductDocument> list = base.getList() == null ? new ArrayList<>() : base.getList();

        Map<String, Object> aggregations = new LinkedHashMap<>();
        aggregations.put("categories", list.stream()
                .filter(item -> StringUtils.hasText(item.getCategoryName()))
                .collect(Collectors.groupingBy(ProductDocument::getCategoryName, Collectors.counting())));
        aggregations.put("brands", list.stream()
                .filter(item -> StringUtils.hasText(item.getBrandName()))
                .collect(Collectors.groupingBy(ProductDocument::getBrandName, Collectors.counting())));

        base.setAggregations(aggregations);
        return base;
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResult<ProductDocument> basicSearch(String keyword, Integer page, Integer size) {
        long start = System.currentTimeMillis();
        int pageNum = normalizePage(page);
        int pageSize = normalizeSize(size);

        Page<ProductDocument> resultPage;
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "hotScore"));
        if (StringUtils.hasText(keyword)) {
            resultPage = productDocumentRepository.searchByKeyword(keyword, pageable);
            recordHotSearch(keyword);
        } else {
            resultPage = productDocumentRepository.findByStatus(1, pageable);
        }

        long took = System.currentTimeMillis() - start;
        return SearchResult.of(resultPage.getContent(), resultPage.getTotalElements(), resultPage.getNumber(), resultPage.getSize(), took);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResult<ProductDocument> filterSearch(ProductSearchRequest request) {
        return searchProducts(request);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResult<ProductDocument> searchByCategory(Long categoryId, Integer page, Integer size) {
        long start = System.currentTimeMillis();
        Pageable pageable = PageRequest.of(normalizePage(page), normalizeSize(size), Sort.by(Sort.Direction.DESC, "hotScore"));
        Page<ProductDocument> resultPage = productDocumentRepository.findByCategoryIdAndStatus(categoryId, 1, pageable);
        long took = System.currentTimeMillis() - start;
        return SearchResult.of(resultPage.getContent(), resultPage.getTotalElements(), resultPage.getNumber(), resultPage.getSize(), took);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResult<ProductDocument> searchByBrand(Long brandId, Integer page, Integer size) {
        long start = System.currentTimeMillis();
        Pageable pageable = PageRequest.of(normalizePage(page), normalizeSize(size), Sort.by(Sort.Direction.DESC, "hotScore"));
        Page<ProductDocument> resultPage = productDocumentRepository.findByBrandIdAndStatus(brandId, 1, pageable);
        long took = System.currentTimeMillis() - start;
        return SearchResult.of(resultPage.getContent(), resultPage.getTotalElements(), resultPage.getNumber(), resultPage.getSize(), took);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResult<ProductDocument> searchByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Integer page, Integer size) {
        long start = System.currentTimeMillis();
        Pageable pageable = PageRequest.of(normalizePage(page), normalizeSize(size), Sort.by(Sort.Direction.DESC, "hotScore"));
        BigDecimal min = minPrice == null ? BigDecimal.ZERO : minPrice;
        BigDecimal max = maxPrice == null ? new BigDecimal("99999999") : maxPrice;
        Page<ProductDocument> resultPage = productDocumentRepository.findByPriceBetweenAndStatus(min, max, 1, pageable);
        long took = System.currentTimeMillis() - start;
        return SearchResult.of(resultPage.getContent(), resultPage.getTotalElements(), resultPage.getNumber(), resultPage.getSize(), took);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResult<ProductDocument> searchByShop(Long shopId, Integer page, Integer size) {
        long start = System.currentTimeMillis();
        Pageable pageable = PageRequest.of(normalizePage(page), normalizeSize(size), Sort.by(Sort.Direction.DESC, "hotScore"));
        Page<ProductDocument> resultPage = productDocumentRepository.findByShopIdAndStatus(shopId, 1, pageable);
        long took = System.currentTimeMillis() - start;
        return SearchResult.of(resultPage.getContent(), resultPage.getTotalElements(), resultPage.getNumber(), resultPage.getSize(), took);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResult<ProductDocument> combinedSearch(String keyword, Long categoryId, Long brandId,
                                                        BigDecimal minPrice, BigDecimal maxPrice, Long shopId,
                                                        String sortBy, String sortOrder, Integer page, Integer size) {
        long start = System.currentTimeMillis();
        Pageable pageable = PageRequest.of(normalizePage(page), normalizeSize(size), buildSort(sortBy, sortOrder));
        Page<ProductDocument> resultPage = productDocumentRepository.combinedSearch(
                keyword,
                categoryId,
                brandId,
                shopId,
                minPrice,
                maxPrice,
                1,
                pageable
        );
        if (StringUtils.hasText(keyword)) {
            recordHotSearch(keyword);
        }
        long took = System.currentTimeMillis() - start;
        return SearchResult.of(resultPage.getContent(), resultPage.getTotalElements(), resultPage.getNumber(), resultPage.getSize(), took);
    }

    private void recordHotSearch(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return;
        }
        try {
            String normalized = keyword.trim().toLowerCase();
            redisTemplate.opsForZSet().incrementScore(HOT_SEARCH_ZSET_KEY, normalized, 1.0D);
            redisTemplate.expire(HOT_SEARCH_ZSET_KEY, 7, TimeUnit.DAYS);
        } catch (Exception e) {
            log.warn("Record hot search failed: keyword={}", keyword, e);
        }
    }

    private int normalizePage(Integer page) {
        return page == null || page < 0 ? 0 : page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size <= 0) {
            return 20;
        }
        return Math.min(size, 100);
    }

    private Sort buildSort(String sortBy, String sortOrder) {
        String field = StringUtils.hasText(sortBy) ? sortBy : "hotScore";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, field);
    }
}
