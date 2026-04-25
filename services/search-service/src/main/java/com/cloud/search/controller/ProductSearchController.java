package com.cloud.search.controller;

import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.common.result.Result;
import com.cloud.search.document.ProductDocument;
import com.cloud.search.dto.ProductSearchRequest;
import com.cloud.search.dto.SearchResultDTO;
import com.cloud.search.service.SearchFacadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "Product Search", description = "Product search and filter APIs")
@Validated
@ApiResponses({
  @ApiResponse(responseCode = "400", description = "Invalid search parameters"),
  @ApiResponse(responseCode = "401", description = "Authentication required when applicable"),
  @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
  @ApiResponse(responseCode = "404", description = "Search resource not found"),
  @ApiResponse(responseCode = "500", description = "Search service internal error")
})
public class ProductSearchController {

  private final SearchFacadeService searchFacadeService;

  @Operation(
      summary = "Search products by request payload",
      description = "Run full search by request payload")
  @PostMapping("/products")
  public Result<SearchResultDTO<ProductDocument>> complexSearch(
      @Valid @RequestBody ProductSearchRequest request,
      @Parameter(description = "search_after values, json array or comma separated")
          @RequestParam(required = false)
          String searchAfter) {
    normalizePublicStatus(request);
    return Result.success(
        "Search success", searchFacadeService.searchProducts(request, searchAfter));
  }

  @Operation(
      summary = "Get filter data",
      description = "Return available filter data for current search")
  @PostMapping("/products/filters")
  public Result<SearchResultDTO<ProductDocument>> getProductFilters(
      @Valid @RequestBody ProductSearchRequest request,
      @Parameter(description = "search_after values, json array or comma separated")
          @RequestParam(required = false)
          String searchAfter) {
    normalizePublicStatus(request);
    return Result.success(
        "Get filters success", searchFacadeService.getProductFilters(request, searchAfter));
  }

  @Operation(summary = "Search suggestions", description = "Get search suggestions by keyword")
  @GetMapping("/products/suggestions")
  public Result<List<String>> getSearchSuggestions(
      @Parameter(description = "Keyword") @RequestParam String keyword,
      @Parameter(description = "Result size") @RequestParam(defaultValue = "10") Integer size) {
    return Result.success(
        "Get suggestions success", searchFacadeService.getSearchSuggestions(keyword, size));
  }

  @Operation(summary = "Hot keywords", description = "Get hot keywords")
  @GetMapping("/products/keywords/hot")
  public Result<List<String>> getHotSearchKeywords(
      @Parameter(description = "Result size") @RequestParam(defaultValue = "10") Integer size) {
    return Result.success(
        "Get hot keywords success", searchFacadeService.getHotSearchKeywords(size));
  }

  @Operation(
      summary = "Keyword recommendations",
      description = "Get recommended keywords for search bar")
  @GetMapping("/products/keywords/recommendations")
  public Result<List<String>> getKeywordRecommendations(
      @Parameter(description = "Keyword prefix") @RequestParam(required = false) String keyword,
      @Parameter(description = "Result size") @RequestParam(defaultValue = "10") Integer size) {
    return Result.success(
        "Get keyword recommendations success",
        searchFacadeService.getKeywordRecommendations(keyword, size));
  }

  @Operation(summary = "Basic search", description = "Search products by keyword and pagination")
  @GetMapping("/products")
  public Result<SearchResultDTO<ProductDocument>> searchProducts(
      @Parameter(description = "Keyword") @RequestParam(required = false) String keyword,
      @Parameter(description = "Page number, starts from 0") @RequestParam(defaultValue = "0")
          int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
      @Parameter(description = "Sort field") @RequestParam(defaultValue = "hotScore") String sortBy,
      @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc")
          String sortDir,
      @Parameter(description = "search_after values, json array or comma separated")
          @RequestParam(required = false)
          String searchAfter) {
    return Result.success(
        "Search success",
        searchFacadeService.searchByKeyword(keyword, page, size, sortBy, sortDir, searchAfter));
  }

  @Operation(
      summary = "Search by category",
      description = "Search products by category and optional keyword")
  @GetMapping("/categories/{categoryId}/products")
  public Result<SearchResultDTO<ProductDocument>> searchByCategory(
      @Parameter(description = "Category id") @PathVariable Long categoryId,
      @Parameter(description = "Keyword") @RequestParam(required = false) String keyword,
      @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
      @Parameter(description = "search_after values, json array or comma separated")
          @RequestParam(required = false)
          String searchAfter) {
    return Result.success(
        "Search success",
        searchFacadeService.searchByCategory(categoryId, keyword, page, size, searchAfter));
  }

  @Operation(
      summary = "Search by shop",
      description = "Search products by shop and optional keyword")
  @GetMapping("/shops/{shopId}/products")
  public Result<SearchResultDTO<ProductDocument>> searchByShop(
      @Parameter(description = "Shop id") @PathVariable Long shopId,
      @Parameter(description = "Keyword") @RequestParam(required = false) String keyword,
      @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
      @Parameter(description = "search_after values, json array or comma separated")
          @RequestParam(required = false)
          String searchAfter) {
    return Result.success(
        "Search success",
        searchFacadeService.searchByShop(shopId, keyword, page, size, searchAfter));
  }

  @Operation(summary = "Recommended products", description = "Get recommended products")
  @GetMapping("/products/recommendations")
  public Result<SearchResultDTO<ProductDocument>> getRecommendedProducts(
      @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
      @Parameter(description = "search_after values, json array or comma separated")
          @RequestParam(required = false)
          String searchAfter) {
    return Result.success(
        "Query recommended products success",
        searchFacadeService.getRecommendedProducts(page, size, searchAfter));
  }

  @Operation(summary = "New products", description = "Get new products")
  @GetMapping("/products/latest")
  public Result<SearchResultDTO<ProductDocument>> getNewProducts(
      @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
      @Parameter(description = "search_after values, json array or comma separated")
          @RequestParam(required = false)
          String searchAfter) {
    return Result.success(
        "Query new products success", searchFacadeService.getNewProducts(page, size, searchAfter));
  }

  @Operation(summary = "Flagged hot products", description = "Get products marked as hot")
  @GetMapping("/products/popular")
  public Result<SearchResultDTO<ProductDocument>> getHotProducts(
      @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
      @Parameter(description = "search_after values, json array or comma separated")
          @RequestParam(required = false)
          String searchAfter) {
    return Result.success(
        "Query hot products success", searchFacadeService.getHotProducts(page, size, searchAfter));
  }

  @Operation(
      summary = "Today hot selling products",
      description = "Get products ranked by today's completed sales")
  @GetMapping("/products/popular/today")
  public Result<SearchResultDTO<ProductDocument>> getTodayHotSellingProducts(
      @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
    return Result.success(
        "Query today hot selling products success",
        searchFacadeService.getTodayHotSellingProducts(page, size));
  }

  private void normalizePublicStatus(ProductSearchRequest request) {
    if (request == null) {
      return;
    }
    Integer status = request.getStatus();
    if (status == null) {
      request.setStatus(1);
      return;
    }
    if (!Integer.valueOf(1).equals(status)) {
      throw new BizException(ResultCode.BAD_REQUEST, "public search only supports active status");
    }
  }
}
