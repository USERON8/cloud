package com.cloud.search.service;

import com.cloud.search.document.ProductDocument;
import com.cloud.search.dto.ProductSearchRequest;
import com.cloud.search.dto.SearchResultDTO;

import java.math.BigDecimal;
import java.util.List;

public interface ProductSearchService {

    SearchResultDTO<ProductDocument> searchProducts(ProductSearchRequest request);

    List<String> getSearchSuggestions(String keyword, Integer size);

    List<String> getHotSearchKeywords(Integer size);

    SearchResultDTO<ProductDocument> getProductFilters(ProductSearchRequest request);

    SearchResultDTO<ProductDocument> basicSearch(String keyword, Integer page, Integer size);

    SearchResultDTO<ProductDocument> filterSearch(ProductSearchRequest request);

    SearchResultDTO<ProductDocument> searchByCategory(Long categoryId, Integer page, Integer size);

    SearchResultDTO<ProductDocument> searchByBrand(Long brandId, Integer page, Integer size);

    SearchResultDTO<ProductDocument> searchByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Integer page, Integer size);

    SearchResultDTO<ProductDocument> searchByShop(Long shopId, Integer page, Integer size);

    SearchResultDTO<ProductDocument> getRecommendedProducts(Integer page, Integer size);

    SearchResultDTO<ProductDocument> getNewProducts(Integer page, Integer size);

    SearchResultDTO<ProductDocument> getHotProducts(Integer page, Integer size);

    SearchResultDTO<ProductDocument> combinedSearch(String keyword, Long categoryId, Long brandId,
                                                 BigDecimal minPrice, BigDecimal maxPrice, Long shopId,
                                                 String sortBy, String sortOrder, Integer page, Integer size);
}
