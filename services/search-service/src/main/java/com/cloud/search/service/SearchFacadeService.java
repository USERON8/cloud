package com.cloud.search.service;

import com.cloud.search.document.ProductDocument;
import com.cloud.search.dto.ProductFilterRequest;
import com.cloud.search.dto.ProductSearchRequest;
import com.cloud.search.dto.SearchResultDTO;
import com.cloud.search.mapper.SearchRequestMapper;
import com.cloud.search.service.support.SearchHotDataCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchFacadeService {

  private final ProductSearchService productSearchService;
  private final ElasticsearchOptimizedService elasticsearchOptimizedService;
  private final SearchRequestMapper searchRequestMapper;
  private final SearchHotDataCacheService searchHotDataCacheService;
  private final ObjectMapper objectMapper;

  public SearchResultDTO<ProductDocument> searchProducts(ProductSearchRequest request) {
    return searchProducts(request, null);
  }

  public SearchResultDTO<ProductDocument> searchProducts(
      ProductSearchRequest request, String searchAfter) {
    if (requiresOptimizedProductSearch(request)) {
      return searchProductsViaElasticsearch(request, searchAfter);
    }
    List<Object> searchAfterValues = parseSearchAfter(searchAfter);
    if (searchAfterValues.isEmpty()) {
      return productSearchService.searchProducts(request);
    }
    return searchProductsViaElasticsearch(request, searchAfter);
  }

  private SearchResultDTO<ProductDocument> searchProductsViaElasticsearch(
      ProductSearchRequest request, String searchAfter) {
    List<Object> searchAfterValues = parseSearchAfter(searchAfter);
    boolean usedSearchAfter = !searchAfterValues.isEmpty();
    long start = System.currentTimeMillis();
    ElasticsearchOptimizedService.SearchResultDTO esResult =
        elasticsearchOptimizedService.productSearchAfter(request, searchAfterValues);
    int safePage = resolvePage(request == null ? null : request.getPage());
    int safeSize = resolveSize(request == null ? null : request.getSize());
    return toSearchResultDTO(
        esResult, safePage, safeSize, System.currentTimeMillis() - start, usedSearchAfter);
  }

  public SearchResultDTO<ProductDocument> getProductFilters(ProductSearchRequest request) {
    return getProductFilters(request, null);
  }

  public SearchResultDTO<ProductDocument> getProductFilters(
      ProductSearchRequest request, String searchAfter) {
    ProductSearchRequest aggregationRequest = copyRequest(request);
    aggregationRequest.setIncludeAggregations(true);
    List<Object> searchAfterValues = parseSearchAfter(searchAfter);
    boolean usedSearchAfter = !searchAfterValues.isEmpty();
    long start = System.currentTimeMillis();
    ElasticsearchOptimizedService.SearchResultDTO esResult =
        elasticsearchOptimizedService.productSearchAfter(aggregationRequest, searchAfterValues);
    SearchResultDTO<ProductDocument> result =
        toSearchResultDTO(
            esResult,
            resolvePage(aggregationRequest.getPage()),
            resolveSize(aggregationRequest.getSize()),
            System.currentTimeMillis() - start,
            usedSearchAfter);
    result.setAggregations(normalizeProductAggregations(result.getAggregations()));
    return result;
  }

  public List<String> getSearchSuggestions(String keyword, Integer size) {
    int safeSize = size == null ? 10 : size;
    return elasticsearchOptimizedService.getSearchSuggestions(keyword, safeSize);
  }

  public List<String> getHotSearchKeywords(Integer size) {
    int safeSize = size == null ? 10 : size;
    return searchHotDataCacheService.getHotKeywords(
        safeSize, () -> elasticsearchOptimizedService.getHotSearchKeywords(safeSize));
  }

  public List<String> getKeywordRecommendations(String keyword, Integer size) {
    int safeSize = size == null ? 10 : size;
    return elasticsearchOptimizedService.getKeywordRecommendations(keyword, safeSize);
  }

  public SearchResultDTO<ProductDocument> searchByKeyword(
      String keyword, int page, int size, String sortBy, String sortDir, String searchAfter) {
    int safePage = Math.max(page, 0);
    int safeSize = size <= 0 ? 20 : size;
    String safeSortBy = (sortBy == null || sortBy.isBlank()) ? "hotScore" : sortBy;
    String safeSortDir = (sortDir == null || sortDir.isBlank()) ? "desc" : sortDir;
    List<Object> searchAfterValues = parseSearchAfter(searchAfter);
    boolean usedSearchAfter = !searchAfterValues.isEmpty();
    long start = System.currentTimeMillis();

    ElasticsearchOptimizedService.SearchResultDTO optimizedResult;
    if (!searchAfterValues.isEmpty()) {
      optimizedResult =
          elasticsearchOptimizedService.smartProductSearchAfter(
              keyword, null, null, null, safeSortBy, safeSortDir, searchAfterValues, safeSize);
    } else {
      int from = safePage * safeSize;
      optimizedResult =
          elasticsearchOptimizedService.smartProductSearch(
              keyword, null, null, null, safeSortBy, safeSortDir, from, safeSize);
    }

    return toSearchResultDTO(
        optimizedResult, safePage, safeSize, System.currentTimeMillis() - start, usedSearchAfter);
  }

  public SearchResultDTO<ProductDocument> searchByCategory(
      Long categoryId, String keyword, int page, int size, String searchAfter) {
    ProductSearchRequest request = buildSearchRequest(page, size, "hotScore", "desc");
    request.setKeyword(keyword);
    request.setCategoryId(categoryId);
    return searchProducts(request, searchAfter);
  }

  public SearchResultDTO<ProductDocument> searchByShop(
      Long shopId, String keyword, int page, int size, String searchAfter) {
    ProductSearchRequest request = buildSearchRequest(page, size, "hotScore", "desc");
    request.setKeyword(keyword);
    request.setShopId(shopId);
    return searchProducts(request, searchAfter);
  }

  public SearchResultDTO<ProductDocument> advancedSearch(
      String keyword,
      BigDecimal minPrice,
      BigDecimal maxPrice,
      int page,
      int size,
      String searchAfter) {
    ProductSearchRequest request = buildSearchRequest(page, size, "hotScore", "desc");
    request.setKeyword(keyword);
    request.setMinPrice(minPrice != null ? minPrice : BigDecimal.ZERO);
    request.setMaxPrice(maxPrice != null ? maxPrice : new BigDecimal("999999"));
    return searchProducts(request, searchAfter);
  }

  public ElasticsearchOptimizedService.SearchResultDTO smartSearch(
      String keyword,
      Long categoryId,
      Double minPrice,
      Double maxPrice,
      String sortField,
      String sortOrder,
      int page,
      int size,
      String searchAfter) {
    int safePage = Math.max(page, 1);
    int safeSize = size <= 0 ? 20 : size;
    List<Object> searchAfterValues = parseSearchAfter(searchAfter);
    if (!searchAfterValues.isEmpty()) {
      return elasticsearchOptimizedService.smartProductSearchAfter(
          keyword,
          categoryId,
          minPrice,
          maxPrice,
          sortField,
          sortOrder,
          searchAfterValues,
          safeSize);
    }
    int from = (safePage - 1) * safeSize;
    return elasticsearchOptimizedService.smartProductSearch(
        keyword, categoryId, minPrice, maxPrice, sortField, sortOrder, from, safeSize);
  }

  public SearchResultDTO<ProductDocument> getRecommendedProducts(
      int page, int size, String searchAfter) {
    List<Object> searchAfterValues = parseSearchAfter(searchAfter);
    if (searchAfterValues.isEmpty()) {
      return productSearchService.getRecommendedProducts(page, size);
    }
    ProductSearchRequest request = new ProductSearchRequest();
    request.setRecommended(true);
    request.setPage(page);
    request.setSize(size);
    request.setSortBy("hotScore");
    request.setSortOrder("desc");
    long start = System.currentTimeMillis();
    ElasticsearchOptimizedService.SearchResultDTO esResult =
        elasticsearchOptimizedService.productSearchAfter(request, searchAfterValues);
    return toSearchResultDTO(
        esResult, resolvePage(page), resolveSize(size), System.currentTimeMillis() - start, true);
  }

  public SearchResultDTO<ProductDocument> getNewProducts(int page, int size, String searchAfter) {
    List<Object> searchAfterValues = parseSearchAfter(searchAfter);
    if (searchAfterValues.isEmpty()) {
      return productSearchService.getNewProducts(page, size);
    }
    ProductSearchRequest request = new ProductSearchRequest();
    request.setIsNew(true);
    request.setPage(page);
    request.setSize(size);
    request.setSortBy("createdAt");
    request.setSortOrder("desc");
    long start = System.currentTimeMillis();
    ElasticsearchOptimizedService.SearchResultDTO esResult =
        elasticsearchOptimizedService.productSearchAfter(request, searchAfterValues);
    return toSearchResultDTO(
        esResult, resolvePage(page), resolveSize(size), System.currentTimeMillis() - start, true);
  }

  public SearchResultDTO<ProductDocument> getHotProducts(int page, int size, String searchAfter) {
    List<Object> searchAfterValues = parseSearchAfter(searchAfter);
    if (searchAfterValues.isEmpty()) {
      return productSearchService.getHotProducts(page, size);
    }
    ProductSearchRequest request = new ProductSearchRequest();
    request.setIsHot(true);
    request.setPage(page);
    request.setSize(size);
    request.setSortBy("hotScore");
    request.setSortOrder("desc");
    long start = System.currentTimeMillis();
    ElasticsearchOptimizedService.SearchResultDTO esResult =
        elasticsearchOptimizedService.productSearchAfter(request, searchAfterValues);
    return toSearchResultDTO(
        esResult, resolvePage(page), resolveSize(size), System.currentTimeMillis() - start, true);
  }

  public SearchResultDTO<ProductDocument> getTodayHotSellingProducts(int page, int size) {
    return productSearchService.getTodayHotSellingProducts(page, size);
  }

  public SearchResultDTO<ProductDocument> basicSearch(
      String keyword, Integer page, Integer size, String searchAfter) {
    List<Object> searchAfterValues = parseSearchAfter(searchAfter);
    if (searchAfterValues.isEmpty()) {
      return productSearchService.basicSearch(keyword, page, size);
    }
    ProductSearchRequest request = new ProductSearchRequest();
    request.setKeyword(keyword);
    request.setPage(page);
    request.setSize(size);
    request.setSortBy("hotScore");
    request.setSortOrder("desc");
    long start = System.currentTimeMillis();
    ElasticsearchOptimizedService.SearchResultDTO esResult =
        elasticsearchOptimizedService.productSearchAfter(request, searchAfterValues);
    return toSearchResultDTO(
        esResult, resolvePage(page), resolveSize(size), System.currentTimeMillis() - start, true);
  }

  public SearchResultDTO<ProductDocument> filterSearch(
      ProductFilterRequest request, String searchAfter) {
    return searchProducts(searchRequestMapper.toSearchRequest(request), searchAfter);
  }

  public SearchResultDTO<ProductDocument> searchByCategoryFilter(
      Long categoryId, Integer page, Integer size, String searchAfter) {
    ProductSearchRequest request = buildSearchRequest(page, size, "hotScore", "desc");
    request.setCategoryId(categoryId);
    return searchProducts(request, searchAfter);
  }

  public SearchResultDTO<ProductDocument> searchByBrandFilter(
      Long brandId, Integer page, Integer size, String searchAfter) {
    ProductSearchRequest request = buildSearchRequest(page, size, "hotScore", "desc");
    request.setBrandId(brandId);
    return searchProducts(request, searchAfter);
  }

  public SearchResultDTO<ProductDocument> searchByPriceFilter(
      BigDecimal minPrice, BigDecimal maxPrice, Integer page, Integer size, String searchAfter) {
    ProductSearchRequest request = buildSearchRequest(page, size, "hotScore", "desc");
    request.setMinPrice(minPrice);
    request.setMaxPrice(maxPrice);
    return searchProducts(request, searchAfter);
  }

  public SearchResultDTO<ProductDocument> searchByShopFilter(
      Long shopId, Integer page, Integer size, String searchAfter) {
    ProductSearchRequest request = buildSearchRequest(page, size, "hotScore", "desc");
    request.setShopId(shopId);
    return searchProducts(request, searchAfter);
  }

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
      Integer size,
      String searchAfter) {
    ProductSearchRequest request = buildSearchRequest(page, size, sortBy, sortOrder);
    request.setKeyword(keyword);
    request.setCategoryId(categoryId);
    request.setBrandId(brandId);
    request.setMinPrice(minPrice);
    request.setMaxPrice(maxPrice);
    request.setShopId(shopId);
    return searchProducts(request, searchAfter);
  }

  private ProductSearchRequest buildSearchRequest(
      Integer page, Integer size, String sortBy, String sortOrder) {
    ProductSearchRequest request = new ProductSearchRequest();
    request.setPage(page);
    request.setSize(size);
    request.setSortBy(sortBy);
    request.setSortOrder(sortOrder);
    return request;
  }

  private int resolvePage(Integer page) {
    return page == null || page < 0 ? 0 : page;
  }

  private int resolveSize(Integer size) {
    return size == null || size <= 0 ? 20 : size;
  }

  private SearchResultDTO<ProductDocument> toSearchResultDTO(
      ElasticsearchOptimizedService.SearchResultDTO esResult,
      int page,
      int size,
      long took,
      boolean usedSearchAfter) {
    int safePage = resolvePage(page);
    int safeSize = resolveSize(size);
    List<ProductDocument> list =
        esResult == null || esResult.getDocuments() == null
            ? Collections.emptyList()
            : esResult.getDocuments().stream()
                .map(document -> objectMapper.convertValue(document, ProductDocument.class))
                .toList();
    long total = esResult == null ? list.size() : esResult.getTotal();
    int totalPages = safeSize <= 0 ? 0 : (int) Math.ceil((double) total / safeSize);
    boolean hasPrevious = usedSearchAfter || safePage > 0;
    boolean hasNext =
        usedSearchAfter
            ? esResult != null
                && esResult.getSearchAfter() != null
                && !esResult.getSearchAfter().isEmpty()
            : safePage < totalPages - 1;
    return SearchResultDTO.<ProductDocument>builder()
        .list(list)
        .total(total)
        .page(safePage)
        .size(safeSize)
        .totalPages(totalPages)
        .hasNext(hasNext)
        .hasPrevious(hasPrevious)
        .took(took)
        .aggregations(esResult == null ? Map.of() : esResult.getAggregations())
        .highlights(esResult == null ? Map.of() : esResult.getHighlights())
        .searchAfter(esResult == null ? List.of() : esResult.getSearchAfter())
        .build();
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
    if (rawBuckets instanceof Map<?, ?> bucketMap) {
      Map<String, Long> normalized = new LinkedHashMap<>();
      for (Map.Entry<?, ?> entry : bucketMap.entrySet()) {
        String key = entry.getKey() == null ? "" : String.valueOf(entry.getKey()).trim();
        if (key.isEmpty() || entry.getValue() == null) {
          continue;
        }
        if (entry.getValue() instanceof Number number) {
          normalized.put(key, number.longValue());
        }
      }
      return normalized;
    }
    if (!(rawBuckets instanceof List<?> bucketList)) {
      return Map.of();
    }
    Map<String, Long> normalized = new LinkedHashMap<>();
    for (Object bucket : bucketList) {
      if (!(bucket instanceof Map<?, ?> bucketMap)) {
        continue;
      }
      Object keyValue = bucketMap.get("key");
      Object countValue = bucketMap.get("count");
      if (keyValue == null || countValue == null) {
        continue;
      }
      String key = String.valueOf(keyValue).trim();
      if (key.isEmpty()) {
        continue;
      }
      long count;
      if (countValue instanceof Number number) {
        count = number.longValue();
      } else {
        try {
          count = Long.parseLong(String.valueOf(countValue));
        } catch (NumberFormatException ex) {
          continue;
        }
      }
      normalized.put(key, count);
    }
    return normalized;
  }

  private boolean requiresOptimizedProductSearch(ProductSearchRequest request) {
    if (request == null) {
      return false;
    }
    return request.getStockStatus() != null
        || request.getRecommended() != null
        || request.getIsNew() != null
        || request.getIsHot() != null
        || request.getMinSalesCount() != null
        || request.getMinRating() != null
        || request.getHighlight() != null && request.getHighlight()
        || request.getIncludeAggregations() != null && request.getIncludeAggregations()
        || request.getShopName() != null && !request.getShopName().isBlank()
        || request.getCategoryName() != null && !request.getCategoryName().isBlank()
        || request.getBrandName() != null && !request.getBrandName().isBlank()
        || request.getTags() != null && !request.getTags().isEmpty();
  }

  private ProductSearchRequest copyRequest(ProductSearchRequest request) {
    if (request == null) {
      return new ProductSearchRequest();
    }
    return objectMapper.convertValue(request, ProductSearchRequest.class);
  }

  private List<Object> parseSearchAfter(String searchAfter) {
    if (searchAfter == null || searchAfter.isBlank()) {
      return List.of();
    }
    String raw = searchAfter.trim();
    if (raw.startsWith("[") && raw.endsWith("]")) {
      try {
        return objectMapper.readValue(
            raw, new com.fasterxml.jackson.core.type.TypeReference<List<Object>>() {});
      } catch (Exception e) {
        log.warn("Parse searchAfter json failed, raw={}", raw, e);
      }
    }
    String[] tokens = raw.split(",");
    List<Object> values = new java.util.ArrayList<>();
    for (String token : tokens) {
      if (token == null) {
        continue;
      }
      String value = token.trim();
      if (value.isBlank()) {
        continue;
      }
      Object parsed = parseScalar(value);
      values.add(parsed);
    }
    return values;
  }

  private Object parseScalar(String value) {
    String lowered = value.toLowerCase();
    if ("true".equals(lowered)) {
      return Boolean.TRUE;
    }
    if ("false".equals(lowered)) {
      return Boolean.FALSE;
    }
    try {
      if (value.contains(".")) {
        return Double.parseDouble(value);
      }
      return Long.parseLong(value);
    } catch (Exception ignored) {
      return value;
    }
  }
}
