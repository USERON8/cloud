package com.cloud.search.controller;

import com.cloud.common.result.Result;
import com.cloud.search.document.ProductDocument;
import com.cloud.search.dto.ProductFilterRequest;
import com.cloud.search.dto.ProductSearchRequest;
import com.cloud.search.dto.SearchResult;
import com.cloud.search.mapper.SearchRequestMapper;
import com.cloud.search.repository.ProductDocumentRepository;
import com.cloud.search.service.ElasticsearchOptimizedService;
import com.cloud.search.service.ProductSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
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

    private final ProductSearchService productSearchService;
    private final ProductDocumentRepository productDocumentRepository;
    private final ElasticsearchOptimizedService elasticsearchOptimizedService;
    private final SearchRequestMapper searchRequestMapper;

    @Operation(summary = "Complex search", description = "Run full search by request payload")
    @PostMapping("/complex-search")
    @PreAuthorize("hasAuthority('SCOPE_search:read')")
    public Result<SearchResult<ProductDocument>> complexSearch(@Valid @RequestBody ProductSearchRequest request) {
        SearchResult<ProductDocument> result = productSearchService.searchProducts(request);
        return Result.success("Search success", result);
    }

    @Operation(summary = "Get filter data", description = "Return available filter data for current search")
    @PostMapping("/filters")
    public Result<SearchResult<ProductDocument>> getProductFilters(@Valid @RequestBody ProductSearchRequest request) {
        SearchResult<ProductDocument> result = productSearchService.getProductFilters(request);
        return Result.success("Get filters success", result);
    }

    @Operation(summary = "Search suggestions", description = "Get search suggestions by keyword")
    @GetMapping("/suggestions")
    public Result<List<String>> getSearchSuggestions(
            @Parameter(description = "Keyword") @RequestParam String keyword,
            @Parameter(description = "Result size") @RequestParam(defaultValue = "10") Integer size) {

        List<String> suggestions = productSearchService.getSearchSuggestions(keyword, size);
        return Result.success("Get suggestions success", suggestions);
    }

    @Operation(summary = "Hot keywords", description = "Get hot keywords")
    @GetMapping("/hot-keywords")
    public Result<List<String>> getHotSearchKeywords(
            @Parameter(description = "Result size") @RequestParam(defaultValue = "10") Integer size) {

        List<String> hotKeywords = productSearchService.getHotSearchKeywords(size);
        return Result.success("Get hot keywords success", hotKeywords);
    }

    @Operation(summary = "Basic search", description = "Search products by keyword and pagination")
    @GetMapping("/search")
    public Result<Page<ProductDocument>> searchProducts(
            @Parameter(description = "Keyword") @RequestParam String keyword,
            @Parameter(description = "Page number, starts from 0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "hotScore") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc") String sortDir) {

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<ProductDocument> result = productDocumentRepository.searchByKeyword(keyword, pageable);
        return Result.success("Search success", result);
    }

    @Operation(summary = "Search by category", description = "Search products by category and optional keyword")
    @GetMapping("/search/category/{categoryId}")
    public Result<Page<ProductDocument>> searchByCategory(
            @Parameter(description = "Category id") @PathVariable Long categoryId,
            @Parameter(description = "Keyword") @RequestParam(required = false) String keyword,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "hotScore"));
        Page<ProductDocument> result =
                (keyword != null && !keyword.trim().isEmpty())
                        ? productDocumentRepository.searchByKeywordAndCategory(keyword, categoryId, pageable)
                        : productDocumentRepository.findByCategoryId(categoryId, pageable);

        return Result.success("Search success", result);
    }

    @Operation(summary = "Search by shop", description = "Search products by shop and optional keyword")
    @GetMapping("/search/shop/{shopId}")
    public Result<Page<ProductDocument>> searchByShop(
            @Parameter(description = "Shop id") @PathVariable Long shopId,
            @Parameter(description = "Keyword") @RequestParam(required = false) String keyword,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "hotScore"));
        Page<ProductDocument> result =
                (keyword != null && !keyword.trim().isEmpty())
                        ? productDocumentRepository.searchByKeywordAndShop(keyword, shopId, pageable)
                        : productDocumentRepository.findByShopId(shopId, pageable);

        return Result.success("Search success", result);
    }

    @Operation(summary = "Advanced search", description = "Search by keyword and price range")
    @GetMapping("/search/advanced")
    public Result<Page<ProductDocument>> advancedSearch(
            @Parameter(description = "Keyword") @RequestParam String keyword,
            @Parameter(description = "Min price") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Max price") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "hotScore"));
        BigDecimal min = minPrice != null ? minPrice : BigDecimal.ZERO;
        BigDecimal max = maxPrice != null ? maxPrice : new BigDecimal("999999");
        Page<ProductDocument> result = productDocumentRepository.advancedSearch(keyword, min, max, pageable);
        return Result.success("Search success", result);
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

        int from = (page - 1) * size;
        ElasticsearchOptimizedService.SearchResult result = elasticsearchOptimizedService
                .smartProductSearch(keyword, categoryId, minPrice, maxPrice, sortField, sortOrder, from, size);
        return Result.success("Search success", result);
    }

    @Operation(summary = "Recommended products", description = "Get recommended products")
    @GetMapping("/recommended")
    public Result<Page<ProductDocument>> getRecommendedProducts(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "hotScore"));
        Page<ProductDocument> result = productDocumentRepository.findByRecommendedTrue(pageable);
        return Result.success("Query recommended products success", result);
    }

    @Operation(summary = "New products", description = "Get new products")
    @GetMapping("/new")
    public Result<Page<ProductDocument>> getNewProducts(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ProductDocument> result = productDocumentRepository.findByIsNewTrue(pageable);
        return Result.success("Query new products success", result);
    }

    @Operation(summary = "Hot products", description = "Get hot products")
    @GetMapping("/hot")
    public Result<Page<ProductDocument>> getHotProducts(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "salesCount"));
        Page<ProductDocument> result = productDocumentRepository.findByIsHotTrue(pageable);
        return Result.success("Query hot products success", result);
    }

    @Operation(summary = "Rebuild index", description = "Rebuild product index")
    @PostMapping("/rebuild-index")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> rebuildIndex() {
        productSearchService.rebuildProductIndex();
        return Result.success("Rebuild index success", "Rebuild index success");
    }

    @Operation(summary = "Basic API search", description = "Basic paged search API")
    @GetMapping("/basic")
    public Result<SearchResult<ProductDocument>> basicSearch(
            @Parameter(description = "Keyword") @RequestParam(required = false) String keyword,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") Integer size) {

        SearchResult<ProductDocument> result = productSearchService.basicSearch(keyword, page, size);
        return Result.success("Search success", result);
    }

    @Operation(summary = "Filter search", description = "Search products with filter payload")
    @PostMapping("/filter")
    public Result<SearchResult<ProductDocument>> filterSearch(@Valid @RequestBody ProductFilterRequest request) {
        ProductSearchRequest searchRequest = searchRequestMapper.toSearchRequest(request);
        SearchResult<ProductDocument> result = productSearchService.filterSearch(searchRequest);
        return Result.success("Search success", result);
    }

    @Operation(summary = "Filter by category", description = "Filter products by category")
    @GetMapping("/filter/category/{categoryId}")
    public Result<SearchResult<ProductDocument>> filterByCategory(
            @Parameter(description = "Category id") @PathVariable Long categoryId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") Integer size) {

        SearchResult<ProductDocument> result = productSearchService.searchByCategory(categoryId, page, size);
        return Result.success("Filter success", result);
    }

    @Operation(summary = "Filter by brand", description = "Filter products by brand")
    @GetMapping("/filter/brand/{brandId}")
    public Result<SearchResult<ProductDocument>> filterByBrand(
            @Parameter(description = "Brand id") @PathVariable Long brandId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") Integer size) {

        SearchResult<ProductDocument> result = productSearchService.searchByBrand(brandId, page, size);
        return Result.success("Filter success", result);
    }

    @Operation(summary = "Filter by price", description = "Filter products by price range")
    @GetMapping("/filter/price")
    public Result<SearchResult<ProductDocument>> filterByPrice(
            @Parameter(description = "Min price") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Max price") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") Integer size) {

        SearchResult<ProductDocument> result = productSearchService.searchByPriceRange(minPrice, maxPrice, page, size);
        return Result.success("Filter success", result);
    }

    @Operation(summary = "Filter by shop", description = "Filter products by shop")
    @GetMapping("/filter/shop/{shopId}")
    public Result<SearchResult<ProductDocument>> filterByShop(
            @Parameter(description = "Shop id") @PathVariable Long shopId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") Integer size) {

        SearchResult<ProductDocument> result = productSearchService.searchByShop(shopId, page, size);
        return Result.success("Filter success", result);
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

        SearchResult<ProductDocument> result = productSearchService.combinedSearch(
                keyword,
                categoryId,
                brandId,
                minPrice,
                maxPrice,
                shopId,
                sortBy,
                sortOrder,
                page,
                size
        );
        return Result.success("Filter success", result);
    }
}
