package com.cloud.search.controller;

import com.cloud.common.result.Result;
import com.cloud.search.document.ProductDocument;
import com.cloud.search.dto.ProductFilterRequest;
import com.cloud.search.dto.ProductSearchRequest;
import com.cloud.search.dto.SearchResult;
import com.cloud.search.service.ElasticsearchOptimizedService;
import com.cloud.search.service.SearchFacadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "Product Search", description = "Product search and filter APIs")
@Validated
public class ProductSearchController {

    private final SearchFacadeService searchFacadeService;

    @Operation(summary = "Complex search", description = "Run full search by request payload")
    @PostMapping("/complex-search")
    public Result<SearchResult<ProductDocument>> complexSearch(@Valid @RequestBody ProductSearchRequest request) {
        return Result.success("Search success", searchFacadeService.searchProducts(request));
    }

    @Operation(summary = "Get filter data", description = "Return available filter data for current search")
    @PostMapping("/filters")
    public Result<SearchResult<ProductDocument>> getProductFilters(@Valid @RequestBody ProductSearchRequest request) {
        return Result.success("Get filters success", searchFacadeService.getProductFilters(request));
    }

    @Operation(summary = "Search suggestions", description = "Get search suggestions by keyword")
    @GetMapping("/suggestions")
    public Result<List<String>> getSearchSuggestions(
            @Parameter(description = "Keyword") @RequestParam String keyword,
            @Parameter(description = "Result size") @RequestParam(defaultValue = "10") Integer size) {
        return Result.success("Get suggestions success", searchFacadeService.getSearchSuggestions(keyword, size));
    }

    @Operation(summary = "Hot keywords", description = "Get hot keywords")
    @GetMapping("/hot-keywords")
    public Result<List<String>> getHotSearchKeywords(
            @Parameter(description = "Result size") @RequestParam(defaultValue = "10") Integer size) {
        return Result.success("Get hot keywords success", searchFacadeService.getHotSearchKeywords(size));
    }

    @Operation(summary = "Keyword recommendations", description = "Get recommended keywords for search bar")
    @GetMapping("/keyword-recommendations")
    public Result<List<String>> getKeywordRecommendations(
            @Parameter(description = "Keyword prefix") @RequestParam(required = false) String keyword,
            @Parameter(description = "Result size") @RequestParam(defaultValue = "10") Integer size) {
        return Result.success("Get keyword recommendations success", searchFacadeService.getKeywordRecommendations(keyword, size));
    }

    @Operation(summary = "Basic search", description = "Search products by keyword and pagination")
    @GetMapping("/search")
    public Result<Page<ProductDocument>> searchProducts(
            @Parameter(description = "Keyword") @RequestParam String keyword,
            @Parameter(description = "Page number, starts from 0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "hotScore") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc") String sortDir) {
        return Result.success("Search success", searchFacadeService.searchByKeyword(keyword, page, size, sortBy, sortDir));
    }

    @Operation(summary = "Search by category", description = "Search products by category and optional keyword")
    @GetMapping("/search/category/{categoryId}")
    public Result<Page<ProductDocument>> searchByCategory(
            @Parameter(description = "Category id") @PathVariable Long categoryId,
            @Parameter(description = "Keyword") @RequestParam(required = false) String keyword,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return Result.success("Search success", searchFacadeService.searchByCategory(categoryId, keyword, page, size));
    }

    @Operation(summary = "Search by shop", description = "Search products by shop and optional keyword")
    @GetMapping("/search/shop/{shopId}")
    public Result<Page<ProductDocument>> searchByShop(
            @Parameter(description = "Shop id") @PathVariable Long shopId,
            @Parameter(description = "Keyword") @RequestParam(required = false) String keyword,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return Result.success("Search success", searchFacadeService.searchByShop(shopId, keyword, page, size));
    }

    @Operation(summary = "Advanced search", description = "Search by keyword and price range")
    @GetMapping("/search/advanced")
    public Result<Page<ProductDocument>> advancedSearch(
            @Parameter(description = "Keyword") @RequestParam String keyword,
            @Parameter(description = "Min price") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Max price") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return Result.success("Search success", searchFacadeService.advancedSearch(keyword, minPrice, maxPrice, page, size));
    }

    @Operation(summary = "Smart search", description = "Search via optimized Elasticsearch query")
    @GetMapping("/smart-search")
    public Result<ElasticsearchOptimizedService.SearchResult> smartSearch(
            @Parameter(description = "Keyword") @RequestParam(required = false) String keyword,
            @Parameter(description = "Category id") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Min price") @RequestParam(required = false) Double minPrice,
            @Parameter(description = "Max price") @RequestParam(required = false) Double maxPrice,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "score") String sortField,
            @Parameter(description = "Sort order") @RequestParam(defaultValue = "desc") String sortOrder,
            @Parameter(description = "Page number, starts from 1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return Result.success("Search success", searchFacadeService.smartSearch(
                keyword, categoryId, minPrice, maxPrice, sortField, sortOrder, page, size
        ));
    }

    @Operation(summary = "Recommended products", description = "Get recommended products")
    @GetMapping("/recommended")
    public Result<Page<ProductDocument>> getRecommendedProducts(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return Result.success("Query recommended products success", searchFacadeService.getRecommendedProducts(page, size));
    }

    @Operation(summary = "New products", description = "Get new products")
    @GetMapping("/new")
    public Result<Page<ProductDocument>> getNewProducts(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return Result.success("Query new products success", searchFacadeService.getNewProducts(page, size));
    }

    @Operation(summary = "Hot products", description = "Get hot products")
    @GetMapping("/hot")
    public Result<Page<ProductDocument>> getHotProducts(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return Result.success("Query hot products success", searchFacadeService.getHotProducts(page, size));
    }

    @Operation(summary = "Basic API search", description = "Basic paged search API")
    @GetMapping("/basic")
    public Result<SearchResult<ProductDocument>> basicSearch(
            @Parameter(description = "Keyword") @RequestParam(required = false) String keyword,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") Integer size) {
        return Result.success("Search success", searchFacadeService.basicSearch(keyword, page, size));
    }

    @Operation(summary = "Filter search", description = "Search products with filter payload")
    @PostMapping("/filter")
    public Result<SearchResult<ProductDocument>> filterSearch(@Valid @RequestBody ProductFilterRequest request) {
        return Result.success("Search success", searchFacadeService.filterSearch(request));
    }

    @Operation(summary = "Filter by category", description = "Filter products by category")
    @GetMapping("/filter/category/{categoryId}")
    public Result<SearchResult<ProductDocument>> filterByCategory(
            @Parameter(description = "Category id") @PathVariable Long categoryId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") Integer size) {
        return Result.success("Filter success", searchFacadeService.searchByCategoryFilter(categoryId, page, size));
    }

    @Operation(summary = "Filter by brand", description = "Filter products by brand")
    @GetMapping("/filter/brand/{brandId}")
    public Result<SearchResult<ProductDocument>> filterByBrand(
            @Parameter(description = "Brand id") @PathVariable Long brandId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") Integer size) {
        return Result.success("Filter success", searchFacadeService.searchByBrandFilter(brandId, page, size));
    }

    @Operation(summary = "Filter by price", description = "Filter products by price range")
    @GetMapping("/filter/price")
    public Result<SearchResult<ProductDocument>> filterByPrice(
            @Parameter(description = "Min price") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Max price") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") Integer size) {
        return Result.success("Filter success", searchFacadeService.searchByPriceFilter(minPrice, maxPrice, page, size));
    }

    @Operation(summary = "Filter by shop", description = "Filter products by shop")
    @GetMapping("/filter/shop/{shopId}")
    public Result<SearchResult<ProductDocument>> filterByShop(
            @Parameter(description = "Shop id") @PathVariable Long shopId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") Integer size) {
        return Result.success("Filter success", searchFacadeService.searchByShopFilter(shopId, page, size));
    }

    @Operation(summary = "Combined filter", description = "Filter with multiple conditions")
    @GetMapping("/filter/combined")
    public Result<SearchResult<ProductDocument>> combinedFilter(
            @Parameter(description = "Keyword") @RequestParam(required = false) String keyword,
            @Parameter(description = "Category id") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Brand id") @RequestParam(required = false) Long brandId,
            @Parameter(description = "Min price") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Max price") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Shop id") @RequestParam(required = false) Long shopId,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "hotScore") String sortBy,
            @Parameter(description = "Sort order") @RequestParam(defaultValue = "desc") String sortOrder,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") Integer size) {
        return Result.success("Filter success", searchFacadeService.combinedSearch(
                keyword, categoryId, brandId, minPrice, maxPrice, shopId, sortBy, sortOrder, page, size
        ));
    }
}

