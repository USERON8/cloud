package com.cloud.search.service;

import com.cloud.search.document.ProductDocument;
import com.cloud.search.dto.ProductSearchRequest;
import com.cloud.search.dto.SearchResult;

import java.math.BigDecimal;
import java.util.List;

public interface ProductSearchService {

    SearchResult<ProductDocument> searchProducts(ProductSearchRequest request);

    List<String> getSearchSuggestions(String keyword, Integer size);

    List<String> getHotSearchKeywords(Integer size);

    SearchResult<ProductDocument> getProductFilters(ProductSearchRequest request);

    SearchResult<ProductDocument> basicSearch(String keyword, Integer page, Integer size);

    SearchResult<ProductDocument> filterSearch(ProductSearchRequest request);

    SearchResult<ProductDocument> searchByCategory(Long categoryId, Integer page, Integer size);

    SearchResult<ProductDocument> searchByBrand(Long brandId, Integer page, Integer size);

    SearchResult<ProductDocument> searchByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Integer page, Integer size);

    SearchResult<ProductDocument> searchByShop(Long shopId, Integer page, Integer size);

    SearchResult<ProductDocument> combinedSearch(String keyword, Long categoryId, Long brandId,
                                                 BigDecimal minPrice, BigDecimal maxPrice, Long shopId,
                                                 String sortBy, String sortOrder, Integer page, Integer size);
}
