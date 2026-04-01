package com.cloud.search.service.impl;

import cn.hutool.core.util.StrUtil;
import com.cloud.search.document.ProductDocument;
import com.cloud.search.dto.ProductSearchRequest;
import com.cloud.search.dto.SearchResultDTO;
import com.cloud.search.repository.ProductDocumentRepository;
import com.cloud.search.service.ElasticsearchOptimizedService;
import com.cloud.search.service.ProductSearchService;
import com.cloud.search.service.support.HotKeywordKeys;
import com.cloud.search.service.support.SearchHotDataCacheService;
import com.cloud.search.service.support.SellRankKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchServiceImpl implements ProductSearchService {

  private static final int ACTIVE_STATUS = 1;

  private final ProductDocumentRepository productDocumentRepository;
  private final StringRedisTemplate redisTemplate;
  private final ElasticsearchOptimizedService elasticsearchOptimizedService;
  private final SearchHotDataCacheService searchHotDataCacheService;
  private final ObjectMapper objectMapper;

  @Value("${search.hot-keyword.daily-ttl-days:7}")
  private long hotKeywordDailyTtlDays;

  @Override
  @Transactional(readOnly = true)
  public SearchResultDTO<ProductDocument> searchProducts(ProductSearchRequest request) {
    ProductSearchRequest safeRequest = request == null ? new ProductSearchRequest() : request;
    long start = System.currentTimeMillis();

    int pageNum = normalizePage(safeRequest.getPage());
    int pageSize = normalizeSize(safeRequest.getSize());
    int status = safeRequest.getStatus() != null ? safeRequest.getStatus() : ACTIVE_STATUS;

    Pageable pageable =
        PageRequest.of(
            pageNum, pageSize, buildSort(safeRequest.getSortBy(), safeRequest.getSortOrder()));

    Page<ProductDocument> page =
        productDocumentRepository.combinedSearch(
            safeRequest.getKeyword(),
            safeRequest.getCategoryId(),
            safeRequest.getBrandId(),
            safeRequest.getShopId(),
            safeRequest.getMinPrice(),
            safeRequest.getMaxPrice(),
            status,
            pageable);

    if (StrUtil.isNotBlank(safeRequest.getKeyword())) {
      recordHotSearch(safeRequest.getKeyword());
    }

    long took = System.currentTimeMillis() - start;
    return SearchResultDTO.of(
        page.getContent(), page.getTotalElements(), page.getNumber(), page.getSize(), took);
  }

  @Override
  @Transactional(readOnly = true)
  public List<String> getSearchSuggestions(String keyword, Integer size) {
    if (StrUtil.isBlank(keyword)) {
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
          .filter(StrUtil::isNotBlank)
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
      var keywords =
          redisTemplate.opsForZSet().reverseRange(HotKeywordKeys.TOTAL_KEY, 0, limit - 1L);
      if (keywords == null || keywords.isEmpty()) {
        return Collections.emptyList();
      }
      return keywords.stream().filter(StrUtil::isNotBlank).collect(Collectors.toList());
    } catch (Exception e) {
      log.error("Get hot search keywords failed", e);
      return Collections.emptyList();
    }
  }

  @Override
  @Transactional(readOnly = true)
  public SearchResultDTO<ProductDocument> getProductFilters(ProductSearchRequest request) {
    ProductSearchRequest aggregationRequest = copyRequest(request);
    aggregationRequest.setIncludeAggregations(true);
    long start = System.currentTimeMillis();
    ElasticsearchOptimizedService.SearchResultDTO esResult =
        elasticsearchOptimizedService.productSearchAfter(aggregationRequest, List.of());
    SearchResultDTO<ProductDocument> result =
        toSearchResultDTO(
            esResult,
            normalizePage(aggregationRequest.getPage()),
            normalizeSize(aggregationRequest.getSize()),
            System.currentTimeMillis() - start);
    result.setAggregations(normalizeProductAggregations(result.getAggregations()));
    return result;
  }

  @Override
  @Transactional(readOnly = true)
  public SearchResultDTO<ProductDocument> basicSearch(String keyword, Integer page, Integer size) {
    long start = System.currentTimeMillis();
    int pageNum = normalizePage(page);
    int pageSize = normalizeSize(size);

    Page<ProductDocument> resultPage;
    Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "hotScore"));
    if (StrUtil.isNotBlank(keyword)) {
      resultPage = productDocumentRepository.searchByKeyword(keyword, pageable);
      recordHotSearch(keyword);
    } else {
      resultPage = productDocumentRepository.findByStatus(ACTIVE_STATUS, pageable);
    }

    long took = System.currentTimeMillis() - start;
    return SearchResultDTO.of(
        resultPage.getContent(),
        resultPage.getTotalElements(),
        resultPage.getNumber(),
        resultPage.getSize(),
        took);
  }

  @Override
  @Transactional(readOnly = true)
  public SearchResultDTO<ProductDocument> filterSearch(ProductSearchRequest request) {
    return searchProducts(request);
  }

  @Override
  @Transactional(readOnly = true)
  public SearchResultDTO<ProductDocument> searchByCategory(
      Long categoryId, Integer page, Integer size) {
    long start = System.currentTimeMillis();
    Pageable pageable =
        PageRequest.of(
            normalizePage(page), normalizeSize(size), Sort.by(Sort.Direction.DESC, "hotScore"));
    Page<ProductDocument> resultPage =
        productDocumentRepository.findByCategoryIdAndStatus(categoryId, ACTIVE_STATUS, pageable);
    long took = System.currentTimeMillis() - start;
    return SearchResultDTO.of(
        resultPage.getContent(),
        resultPage.getTotalElements(),
        resultPage.getNumber(),
        resultPage.getSize(),
        took);
  }

  @Override
  @Transactional(readOnly = true)
  public SearchResultDTO<ProductDocument> searchByBrand(Long brandId, Integer page, Integer size) {
    long start = System.currentTimeMillis();
    Pageable pageable =
        PageRequest.of(
            normalizePage(page), normalizeSize(size), Sort.by(Sort.Direction.DESC, "hotScore"));
    Page<ProductDocument> resultPage =
        productDocumentRepository.findByBrandIdAndStatus(brandId, ACTIVE_STATUS, pageable);
    long took = System.currentTimeMillis() - start;
    return SearchResultDTO.of(
        resultPage.getContent(),
        resultPage.getTotalElements(),
        resultPage.getNumber(),
        resultPage.getSize(),
        took);
  }

  @Override
  @Transactional(readOnly = true)
  public SearchResultDTO<ProductDocument> searchByPriceRange(
      BigDecimal minPrice, BigDecimal maxPrice, Integer page, Integer size) {
    long start = System.currentTimeMillis();
    Pageable pageable =
        PageRequest.of(
            normalizePage(page), normalizeSize(size), Sort.by(Sort.Direction.DESC, "hotScore"));
    BigDecimal min = minPrice == null ? BigDecimal.ZERO : minPrice;
    BigDecimal max = maxPrice == null ? new BigDecimal("99999999") : maxPrice;
    Page<ProductDocument> resultPage =
        productDocumentRepository.findByPriceBetweenAndStatus(min, max, ACTIVE_STATUS, pageable);
    long took = System.currentTimeMillis() - start;
    return SearchResultDTO.of(
        resultPage.getContent(),
        resultPage.getTotalElements(),
        resultPage.getNumber(),
        resultPage.getSize(),
        took);
  }

  @Override
  @Transactional(readOnly = true)
  public SearchResultDTO<ProductDocument> searchByShop(Long shopId, Integer page, Integer size) {
    long start = System.currentTimeMillis();
    Pageable pageable =
        PageRequest.of(
            normalizePage(page), normalizeSize(size), Sort.by(Sort.Direction.DESC, "hotScore"));
    Page<ProductDocument> resultPage =
        productDocumentRepository.findByShopIdAndStatus(shopId, ACTIVE_STATUS, pageable);
    long took = System.currentTimeMillis() - start;
    return SearchResultDTO.of(
        resultPage.getContent(),
        resultPage.getTotalElements(),
        resultPage.getNumber(),
        resultPage.getSize(),
        took);
  }

  @Override
  @Transactional(readOnly = true)
  public SearchResultDTO<ProductDocument> getRecommendedProducts(Integer page, Integer size) {
    long start = System.currentTimeMillis();
    int pageNum = normalizePage(page);
    int pageSize = normalizeSize(size);
    Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "hotScore"));
    Page<ProductDocument> resultPage;
    try {
      resultPage =
          productDocumentRepository.findByRecommendedTrueAndStatus(ACTIVE_STATUS, pageable);
    } catch (Exception ex) {
      log.warn("Recommended query fallback triggered", ex);
      resultPage = loadActiveProductFallback(pageNum, pageSize, "createdAt");
    }
    long took = System.currentTimeMillis() - start;
    return SearchResultDTO.of(
        resultPage.getContent(),
        resultPage.getTotalElements(),
        resultPage.getNumber(),
        resultPage.getSize(),
        took);
  }

  @Override
  @Transactional(readOnly = true)
  public SearchResultDTO<ProductDocument> getNewProducts(Integer page, Integer size) {
    long start = System.currentTimeMillis();
    int pageNum = normalizePage(page);
    int pageSize = normalizeSize(size);
    Pageable pageable =
        PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    Page<ProductDocument> resultPage;
    try {
      resultPage = productDocumentRepository.findByIsNewTrueAndStatus(ACTIVE_STATUS, pageable);
    } catch (Exception ex) {
      log.warn("New products query fallback triggered", ex);
      resultPage = loadActiveProductFallback(pageNum, pageSize, "createdAt");
    }
    long took = System.currentTimeMillis() - start;
    return SearchResultDTO.of(
        resultPage.getContent(),
        resultPage.getTotalElements(),
        resultPage.getNumber(),
        resultPage.getSize(),
        took);
  }

  @Override
  @Transactional(readOnly = true)
  public SearchResultDTO<ProductDocument> getHotProducts(Integer page, Integer size) {
    long start = System.currentTimeMillis();
    int pageNum = normalizePage(page);
    int pageSize = normalizeSize(size);
    Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "hotScore"));
    Page<ProductDocument> resultPage;
    try {
      resultPage = productDocumentRepository.findByIsHotTrueAndStatus(ACTIVE_STATUS, pageable);
    } catch (Exception ex) {
      log.warn("Hot products query fallback triggered", ex);
      resultPage = loadActiveProductFallback(pageNum, pageSize, "createdAt");
    }
    long took = System.currentTimeMillis() - start;
    return SearchResultDTO.of(
        resultPage.getContent(),
        resultPage.getTotalElements(),
        resultPage.getNumber(),
        resultPage.getSize(),
        took);
  }

  @Override
  @Transactional(readOnly = true)
  public SearchResultDTO<ProductDocument> getTodayHotSellingProducts(Integer page, Integer size) {
    long start = System.currentTimeMillis();
    int pageNum = normalizePage(page);
    int pageSize = normalizeSize(size);

    try {
      List<String> orderedIds =
          searchHotDataCacheService.getTodayHotProductIds(
              () -> {
                var zSetOperations = redisTemplate.opsForZSet();
                Long total = zSetOperations.size(SellRankKeys.TODAY_KEY);
                if (total == null || total <= 0L) {
                  return List.of();
                }
                var rankedProductIds =
                    zSetOperations.reverseRange(SellRankKeys.TODAY_KEY, 0L, total - 1L);
                if (rankedProductIds == null || rankedProductIds.isEmpty()) {
                  return List.of();
                }
                return rankedProductIds.stream().toList();
              });

      if (orderedIds.isEmpty()) {
        return SearchResultDTO.of(
            List.of(), 0L, pageNum, pageSize, System.currentTimeMillis() - start);
      }
      Map<String, ProductDocument> documentsById = new LinkedHashMap<>();
      for (ProductDocument document : productDocumentRepository.findAllById(orderedIds)) {
        if (document == null
            || document.getId() == null
            || !Integer.valueOf(ACTIVE_STATUS).equals(document.getStatus())) {
          continue;
        }
        documentsById.put(document.getId(), document);
      }

      List<ProductDocument> orderedDocuments = new ArrayList<>(orderedIds.size());
      for (String productId : orderedIds) {
        ProductDocument document = documentsById.get(productId);
        if (document != null) {
          orderedDocuments.add(document);
        }
      }

      int fromIndex = Math.min(pageNum * pageSize, orderedDocuments.size());
      int toIndex = Math.min(fromIndex + pageSize, orderedDocuments.size());
      List<ProductDocument> pageList = orderedDocuments.subList(fromIndex, toIndex);
      long filteredTotal = orderedDocuments.size();

      return SearchResultDTO.of(
          pageList, filteredTotal, pageNum, pageSize, System.currentTimeMillis() - start);
    } catch (Exception ex) {
      log.error("Get today hot selling products failed", ex);
      return SearchResultDTO.of(
          List.of(), 0L, pageNum, pageSize, System.currentTimeMillis() - start);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public SearchResultDTO<ProductDocument> combinedSearch(
      String keyword,
      Long categoryId,
      Long brandId,
      BigDecimal minPrice,
      BigDecimal maxPrice,
      Long shopId,
      String sortBy,
      String sortOrder,
      Integer page,
      Integer size) {
    long start = System.currentTimeMillis();
    Pageable pageable =
        PageRequest.of(normalizePage(page), normalizeSize(size), buildSort(sortBy, sortOrder));
    Page<ProductDocument> resultPage =
        productDocumentRepository.combinedSearch(
            keyword, categoryId, brandId, shopId, minPrice, maxPrice, ACTIVE_STATUS, pageable);
    if (StrUtil.isNotBlank(keyword)) {
      recordHotSearch(keyword);
    }
    long took = System.currentTimeMillis() - start;
    return SearchResultDTO.of(
        resultPage.getContent(),
        resultPage.getTotalElements(),
        resultPage.getNumber(),
        resultPage.getSize(),
        took);
  }

  private void recordHotSearch(String keyword) {
    if (StrUtil.isBlank(keyword)) {
      return;
    }
    try {
      String normalized = keyword.trim().toLowerCase();
      String dailyKey = HotKeywordKeys.todayKey();
      redisTemplate.opsForZSet().incrementScore(dailyKey, normalized, 1.0D);
      redisTemplate.expire(dailyKey, Math.max(1, hotKeywordDailyTtlDays), TimeUnit.DAYS);
      redisTemplate.opsForZSet().incrementScore(HotKeywordKeys.TOTAL_KEY, normalized, 1.0D);
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
    String field = StrUtil.isNotBlank(sortBy) ? sortBy : "hotScore";
    Sort.Direction direction =
        "asc".equalsIgnoreCase(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;
    return Sort.by(direction, field);
  }

  private ProductSearchRequest copyRequest(ProductSearchRequest request) {
    if (request == null) {
      return new ProductSearchRequest();
    }
    return objectMapper.convertValue(request, ProductSearchRequest.class);
  }

  private SearchResultDTO<ProductDocument> toSearchResultDTO(
      ElasticsearchOptimizedService.SearchResultDTO esResult, int page, int size, long took) {
    List<ProductDocument> list =
        esResult == null || esResult.getDocuments() == null
            ? Collections.emptyList()
            : esResult.getDocuments().stream()
                .map(document -> objectMapper.convertValue(document, ProductDocument.class))
                .toList();
    long total = esResult == null ? list.size() : esResult.getTotal();
    int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) total / size);
    return SearchResultDTO.<ProductDocument>builder()
        .list(list)
        .total(total)
        .page(page)
        .size(size)
        .totalPages(totalPages)
        .hasNext(page < totalPages - 1)
        .hasPrevious(page > 0)
        .took(took)
        .aggregations(esResult == null ? Map.of() : esResult.getAggregations())
        .highlights(esResult == null ? Map.of() : esResult.getHighlights())
        .searchAfter(esResult == null ? List.of() : esResult.getSearchAfter())
        .build();
  }

  private Page<ProductDocument> loadActiveProductFallback(int page, int size, String sortField) {
    Pageable fallbackPageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortField));
    Page<ProductDocument> sourcePage =
        productDocumentRepository.findByStatus(ACTIVE_STATUS, fallbackPageable);
    List<ProductDocument> normalized =
        sourcePage.getContent().stream().map(this::normalizeDerivedFlags).toList();
    return new PageImpl<>(normalized, fallbackPageable, sourcePage.getTotalElements());
  }

  private ProductDocument normalizeDerivedFlags(ProductDocument document) {
    if (document == null) {
      return null;
    }
    int salesCount = document.getSalesCount() == null ? 0 : Math.max(document.getSalesCount(), 0);
    boolean isNew =
        document.getCreatedAt() != null
            && document.getCreatedAt().isAfter(java.time.LocalDateTime.now().minusDays(30));
    boolean isHot = salesCount > 0;
    boolean recommended = Boolean.TRUE.equals(document.getRecommended()) || isHot || isNew;
    double hotScore = salesCount > 0 ? Math.log1p(salesCount) * 25D : 0D;
    if (recommended) {
      hotScore += 30D;
    }
    if (isNew) {
      hotScore += 15D;
    }
    document.setSalesCount(salesCount);
    document.setIsNew(isNew);
    document.setIsHot(isHot);
    document.setRecommended(recommended);
    document.setHotScore(hotScore);
    document.setSearchWeight(hotScore + (document.getProductName() == null ? 0D : 10D));
    return document;
  }

  private Map<String, Object> normalizeProductAggregations(Map<String, Object> rawAggregations) {
    if (rawAggregations == null || rawAggregations.isEmpty()) {
      return Map.of();
    }
    Map<String, Object> normalized = new LinkedHashMap<>();
    normalized.put("categories", normalizeBucketCounts(rawAggregations.get("categories")));
    normalized.put("brands", normalizeBucketCounts(rawAggregations.get("brands")));
    if (rawAggregations.containsKey("priceRanges")) {
      normalized.put("priceRanges", rawAggregations.get("priceRanges"));
    }
    return normalized;
  }

  private Map<String, Long> normalizeBucketCounts(Object rawBuckets) {
    if (!(rawBuckets instanceof Map<?, ?> bucketMap)) {
      return Map.of();
    }
    Map<String, Long> normalized = new LinkedHashMap<>();
    for (Map.Entry<?, ?> entry : bucketMap.entrySet()) {
      String key = entry.getKey() == null ? "" : String.valueOf(entry.getKey()).trim();
      if (key.isEmpty() || !(entry.getValue() instanceof Number number)) {
        continue;
      }
      normalized.put(key, number.longValue());
    }
    return normalized;
  }
}
