package com.cloud.search.service;

import com.cloud.search.document.ProductDocument;
import com.cloud.search.dto.ProductFilterRequest;
import com.cloud.search.dto.ProductSearchRequest;
import com.cloud.search.dto.SearchResult;
import com.cloud.search.mapper.SearchRequestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchFacadeService {

    private final ProductSearchService productSearchService;
    private final ElasticsearchOptimizedService elasticsearchOptimizedService;
    private final SearchRequestMapper searchRequestMapper;

    public SearchResult<ProductDocument> searchProducts(ProductSearchRequest request) {
        return productSearchService.searchProducts(request);
    }

    public SearchResult<ProductDocument> getProductFilters(ProductSearchRequest request) {
        return productSearchService.getProductFilters(request);
    }

    public List<String> getSearchSuggestions(String keyword, Integer size) {
        int safeSize = size == null ? 10 : size;
        return elasticsearchOptimizedService.getSearchSuggestions(keyword, safeSize);
    }

    public List<String> getHotSearchKeywords(Integer size) {
        int safeSize = size == null ? 10 : size;
        return elasticsearchOptimizedService.getHotSearchKeywords(safeSize);
    }

    public List<String> getKeywordRecommendations(String keyword, Integer size) {
        int safeSize = size == null ? 10 : size;
        return elasticsearchOptimizedService.getKeywordRecommendations(keyword, safeSize);
    }

    public Page<ProductDocument> searchByKeyword(String keyword, int page, int size, String sortBy, String sortDir) {
        SearchResult<ProductDocument> result = productSearchService.combinedSearch(
                keyword,
                null,
                null,
                null,
                null,
                null,
                sortBy,
                sortDir,
                page,
                size
        );
        return toPage(result, page, size, sortBy, sortDir);
    }

    public Page<ProductDocument> searchByCategory(Long categoryId, String keyword, int page, int size) {
        SearchResult<ProductDocument> result = productSearchService.combinedSearch(
                keyword,
                categoryId,
                null,
                null,
                null,
                null,
                "hotScore",
                "desc",
                page,
                size
        );
        return toPage(result, page, size, "hotScore", "desc");
    }

    public Page<ProductDocument> searchByShop(Long shopId, String keyword, int page, int size) {
        SearchResult<ProductDocument> result = productSearchService.combinedSearch(
                keyword,
                null,
                null,
                null,
                null,
                shopId,
                "hotScore",
                "desc",
                page,
                size
        );
        return toPage(result, page, size, "hotScore", "desc");
    }

    public Page<ProductDocument> advancedSearch(String keyword, BigDecimal minPrice, BigDecimal maxPrice, int page, int size) {
        SearchResult<ProductDocument> result = productSearchService.combinedSearch(
                keyword,
                null,
                null,
                minPrice != null ? minPrice : BigDecimal.ZERO,
                maxPrice != null ? maxPrice : new BigDecimal("999999"),
                null,
                "hotScore",
                "desc",
                page,
                size
        );
        return toPage(result, page, size, "hotScore", "desc");
    }

    public ElasticsearchOptimizedService.SearchResult smartSearch(String keyword, Long categoryId, Double minPrice, Double maxPrice,
                                                                  String sortField, String sortOrder, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = size <= 0 ? 20 : size;
        int from = (safePage - 1) * safeSize;
        return elasticsearchOptimizedService.smartProductSearch(
                keyword,
                categoryId,
                minPrice,
                maxPrice,
                sortField,
                sortOrder,
                from,
                safeSize
        );
    }

    public Page<ProductDocument> getRecommendedProducts(int page, int size) {
        SearchResult<ProductDocument> result = productSearchService.getRecommendedProducts(page, size);
        return toPage(result, page, size, "hotScore", "desc");
    }

    public Page<ProductDocument> getNewProducts(int page, int size) {
        SearchResult<ProductDocument> result = productSearchService.getNewProducts(page, size);
        return toPage(result, page, size, "createdAt", "desc");
    }

    public Page<ProductDocument> getHotProducts(int page, int size) {
        SearchResult<ProductDocument> result = productSearchService.getHotProducts(page, size);
        return toPage(result, page, size, "salesCount", "desc");
    }

    public SearchResult<ProductDocument> basicSearch(String keyword, Integer page, Integer size) {
        return productSearchService.basicSearch(keyword, page, size);
    }

    public SearchResult<ProductDocument> filterSearch(ProductFilterRequest request) {
        ProductSearchRequest searchRequest = searchRequestMapper.toSearchRequest(request);
        return productSearchService.filterSearch(searchRequest);
    }

    public SearchResult<ProductDocument> searchByCategoryFilter(Long categoryId, Integer page, Integer size) {
        return productSearchService.searchByCategory(categoryId, page, size);
    }

    public SearchResult<ProductDocument> searchByBrandFilter(Long brandId, Integer page, Integer size) {
        return productSearchService.searchByBrand(brandId, page, size);
    }

    public SearchResult<ProductDocument> searchByPriceFilter(BigDecimal minPrice, BigDecimal maxPrice, Integer page, Integer size) {
        return productSearchService.searchByPriceRange(minPrice, maxPrice, page, size);
    }

    public SearchResult<ProductDocument> searchByShopFilter(Long shopId, Integer page, Integer size) {
        return productSearchService.searchByShop(shopId, page, size);
    }

    public SearchResult<ProductDocument> combinedSearch(String keyword, Long categoryId, Long brandId,
                                                        BigDecimal minPrice, BigDecimal maxPrice, Long shopId,
                                                        String sortBy, String sortOrder, Integer page, Integer size) {
        return productSearchService.combinedSearch(
                keyword, categoryId, brandId, minPrice, maxPrice, shopId, sortBy, sortOrder, page, size
        );
    }

    private Page<ProductDocument> toPage(SearchResult<ProductDocument> result, int page, int size, String sortBy, String sortDir) {
        int safePage = page < 0 ? 0 : page;
        int safeSize = size <= 0 ? 20 : size;
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String safeSortBy = (sortBy == null || sortBy.isBlank()) ? "hotScore" : sortBy;
        List<ProductDocument> list = result == null || result.getList() == null ? Collections.emptyList() : result.getList();
        long total = result == null || result.getTotal() == null ? list.size() : result.getTotal();
        return new PageImpl<>(list, PageRequest.of(safePage, safeSize, Sort.by(direction, safeSortBy)), total);
    }
}
