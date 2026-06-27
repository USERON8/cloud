package com.cloud.search.controller;

import com.cloud.common.result.Result;
import com.cloud.search.controller.support.SearchPublicStatusSupport;
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
@Tag(name = "商品搜索", description = "商品搜索和筛选接口")
@Validated
@ApiResponses({
  @ApiResponse(responseCode = "400", description = "搜索参数无效"),
  @ApiResponse(responseCode = "401", description = "需要认证时必须登录"),
  @ApiResponse(responseCode = "403", description = "权限不足"),
  @ApiResponse(responseCode = "404", description = "搜索资源不存在"),
  @ApiResponse(responseCode = "500", description = "搜索服务内部错误")
})
public class ProductSearchController {

  private final SearchFacadeService searchFacadeService;

  @Operation(
      summary = "按请求体搜索商品",
      description = "按请求体执行完整商品搜索")
  @PostMapping("/products")
  public Result<SearchResultDTO<ProductDocument>> complexSearch(
      @Valid @RequestBody ProductSearchRequest request,
      @Parameter(description = "search_after 游标值，支持 JSON 数组或逗号分隔")
          @RequestParam(required = false)
          String searchAfter) {
    SearchPublicStatusSupport.normalize(request);
    return Result.success(
        "Search success", searchFacadeService.searchProducts(request, searchAfter));
  }

  @Operation(
      summary = "获取筛选数据",
      description = "返回当前搜索可用筛选项")
  @PostMapping("/products/filters")
  public Result<SearchResultDTO<ProductDocument>> getProductFilters(
      @Valid @RequestBody ProductSearchRequest request,
      @Parameter(description = "search_after 游标值，支持 JSON 数组或逗号分隔")
          @RequestParam(required = false)
          String searchAfter) {
    SearchPublicStatusSupport.normalize(request);
    return Result.success(
        "Get filters success", searchFacadeService.getProductFilters(request, searchAfter));
  }

  @Operation(summary = "搜索建议", description = "按关键词获取搜索建议")
  @GetMapping("/products/suggestions")
  public Result<List<String>> getSearchSuggestions(
      @Parameter(description = "关键词") @RequestParam String keyword,
      @Parameter(description = "结果数量") @RequestParam(defaultValue = "10") Integer size) {
    return Result.success(
        "Get suggestions success", searchFacadeService.getSearchSuggestions(keyword, size));
  }

  @Operation(summary = "热门关键词", description = "获取热门关键词")
  @GetMapping("/products/keywords/hot")
  public Result<List<String>> getHotSearchKeywords(
      @Parameter(description = "结果数量") @RequestParam(defaultValue = "10") Integer size) {
    return Result.success(
        "Get hot keywords success", searchFacadeService.getHotSearchKeywords(size));
  }

  @Operation(
      summary = "关键词推荐",
      description = "获取搜索框推荐关键词")
  @GetMapping("/products/keywords/recommendations")
  public Result<List<String>> getKeywordRecommendations(
      @Parameter(description = "关键词前缀") @RequestParam(required = false) String keyword,
      @Parameter(description = "结果数量") @RequestParam(defaultValue = "10") Integer size) {
    return Result.success(
        "Get keyword recommendations success",
        searchFacadeService.getKeywordRecommendations(keyword, size));
  }

  @Operation(summary = "基础搜索", description = "按关键词分页搜索商品")
  @GetMapping("/products")
  public Result<SearchResultDTO<ProductDocument>> searchProducts(
      @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
      @Parameter(description = "页码，从 0 开始") @RequestParam(defaultValue = "0")
          int page,
      @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size,
      @Parameter(description = "排序字段") @RequestParam(defaultValue = "hotScore") String sortBy,
      @Parameter(description = "排序方向") @RequestParam(defaultValue = "desc")
          String sortDir,
      @Parameter(description = "search_after 游标值，支持 JSON 数组或逗号分隔")
          @RequestParam(required = false)
          String searchAfter) {
    return Result.success(
        "Search success",
        searchFacadeService.searchByKeyword(keyword, page, size, sortBy, sortDir, searchAfter));
  }

  @Operation(
      summary = "按分类搜索",
      description = "按分类和可选关键词搜索商品")
  @GetMapping("/categories/{categoryId}/products")
  public Result<SearchResultDTO<ProductDocument>> searchByCategory(
      @Parameter(description = "分类 ID") @PathVariable Long categoryId,
      @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
      @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size,
      @Parameter(description = "search_after 游标值，支持 JSON 数组或逗号分隔")
          @RequestParam(required = false)
          String searchAfter) {
    return Result.success(
        "Search success",
        searchFacadeService.searchByCategory(categoryId, keyword, page, size, searchAfter));
  }

  @Operation(
      summary = "按店铺搜索",
      description = "按店铺和可选关键词搜索商品")
  @GetMapping("/shops/{shopId}/products")
  public Result<SearchResultDTO<ProductDocument>> searchByShop(
      @Parameter(description = "店铺 ID") @PathVariable Long shopId,
      @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
      @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size,
      @Parameter(description = "search_after 游标值，支持 JSON 数组或逗号分隔")
          @RequestParam(required = false)
          String searchAfter) {
    return Result.success(
        "Search success",
        searchFacadeService.searchByShop(shopId, keyword, page, size, searchAfter));
  }

  @Operation(summary = "推荐商品", description = "获取推荐商品")
  @GetMapping("/products/recommendations")
  public Result<SearchResultDTO<ProductDocument>> getRecommendedProducts(
      @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size,
      @Parameter(description = "search_after 游标值，支持 JSON 数组或逗号分隔")
          @RequestParam(required = false)
          String searchAfter) {
    return Result.success(
        "Query recommended products success",
        searchFacadeService.getRecommendedProducts(page, size, searchAfter));
  }

  @Operation(summary = "新品商品", description = "获取新品商品")
  @GetMapping("/products/latest")
  public Result<SearchResultDTO<ProductDocument>> getNewProducts(
      @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size,
      @Parameter(description = "search_after 游标值，支持 JSON 数组或逗号分隔")
          @RequestParam(required = false)
          String searchAfter) {
    return Result.success(
        "Query new products success", searchFacadeService.getNewProducts(page, size, searchAfter));
  }

  @Operation(summary = "标记热门商品", description = "获取标记为热门的商品")
  @GetMapping("/products/popular")
  public Result<SearchResultDTO<ProductDocument>> getHotProducts(
      @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size,
      @Parameter(description = "search_after 游标值，支持 JSON 数组或逗号分隔")
          @RequestParam(required = false)
          String searchAfter) {
    return Result.success(
        "Query hot products success", searchFacadeService.getHotProducts(page, size, searchAfter));
  }

  @Operation(
      summary = "今日热销商品",
      description = "获取按今日已完成销量排序的商品")
  @GetMapping("/products/popular/today")
  public Result<SearchResultDTO<ProductDocument>> getTodayHotSellingProducts(
      @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size) {
    return Result.success(
        "Query today hot selling products success",
        searchFacadeService.getTodayHotSellingProducts(page, size));
  }
}
