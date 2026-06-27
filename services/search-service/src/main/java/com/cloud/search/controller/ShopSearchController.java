package com.cloud.search.controller;

import com.cloud.common.exception.ResourceNotFoundException;
import com.cloud.common.result.Result;
import com.cloud.search.controller.support.SearchPublicStatusSupport;
import com.cloud.search.document.ShopDocument;
import com.cloud.search.dto.SearchResultDTO;
import com.cloud.search.dto.ShopSearchRequest;
import com.cloud.search.service.ShopSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "店铺搜索", description = "店铺搜索接口")
@Validated
public class ShopSearchController {

  private final ShopSearchService shopSearchService;

  @Operation(summary = "复杂店铺搜索", description = "按多条件搜索店铺")
  @PostMapping("/search/shops")
  public Result<SearchResultDTO<ShopDocument>> complexSearch(
      @Valid @RequestBody ShopSearchRequest request) {
    SearchPublicStatusSupport.normalize(request);
    SearchResultDTO<ShopDocument> result = shopSearchService.searchShops(request);
    return Result.success("Search success", result);
  }

  @Operation(summary = "店铺筛选数据", description = "按请求获取店铺筛选项")
  @PostMapping("/search/shops/filters")
  public Result<SearchResultDTO<ShopDocument>> getShopFilters(
      @Valid @RequestBody ShopSearchRequest request) {
    SearchPublicStatusSupport.normalize(request);
    SearchResultDTO<ShopDocument> result = shopSearchService.getShopFilters(request);
    return Result.success("Get filters success", result);
  }

  @Operation(summary = "店铺搜索建议", description = "按关键词获取店铺建议")
  @GetMapping("/search/shops/suggestions")
  public Result<List<String>> getSearchSuggestions(
      @Parameter(description = "关键词") @RequestParam String keyword,
      @Parameter(description = "数量") @RequestParam(defaultValue = "10") Integer size) {

    List<String> suggestions = shopSearchService.getSearchSuggestions(keyword, size);
    return Result.success("Get suggestions success", suggestions);
  }

  @Operation(summary = "热门店铺", description = "获取热门店铺")
  @GetMapping("/search/shops/popular")
  public Result<List<ShopDocument>> getHotShops(
      @Parameter(description = "数量") @RequestParam(defaultValue = "10") Integer size) {

    List<ShopDocument> hotShops = shopSearchService.getHotShops(size);
    return Result.success("Get hot shops success", hotShops);
  }

  @Operation(summary = "按 ID 查询店铺", description = "按 ID 查询店铺详情")
  @GetMapping("/shops/{shopId}")
  public Result<ShopDocument> getShopById(
      @Parameter(description = "店铺 ID") @PathVariable Long shopId) {
    ShopDocument shop = shopSearchService.findByShopId(shopId);
    if (shop == null || !Integer.valueOf(1).equals(shop.getStatus())) {
      throw new ResourceNotFoundException("Shop", String.valueOf(shopId));
    }
    return Result.success("Query success", shop);
  }

  @Operation(summary = "推荐店铺", description = "获取推荐店铺")
  @GetMapping("/search/shops/recommendations")
  public Result<SearchResultDTO<ShopDocument>> getRecommendedShops(
      @Parameter(description = "页码") @RequestParam(defaultValue = "0") Integer page,
      @Parameter(description = "数量") @RequestParam(defaultValue = "20") Integer size) {

    ShopSearchRequest request = new ShopSearchRequest();
    request.setRecommended(true);
    request.setStatus(1);
    request.setPage(page);
    request.setSize(size);
    request.setSortBy("hotScore");
    request.setSortOrder("desc");

    SearchResultDTO<ShopDocument> result = shopSearchService.searchShops(request);
    return Result.success("Query recommended shops success", result);
  }

  @Operation(summary = "按位置搜索店铺", description = "按地址关键词搜索店铺")
  @GetMapping("/search/shops/nearby")
  public Result<SearchResultDTO<ShopDocument>> searchShopsByLocation(
      @Parameter(description = "位置关键词") @RequestParam String location,
      @Parameter(description = "页码") @RequestParam(defaultValue = "0") Integer page,
      @Parameter(description = "数量") @RequestParam(defaultValue = "20") Integer size) {

    ShopSearchRequest request = new ShopSearchRequest();
    request.setAddressKeyword(location);
    request.setStatus(1);
    request.setPage(page);
    request.setSize(size);
    request.setSortBy("rating");
    request.setSortOrder("desc");

    SearchResultDTO<ShopDocument> result = shopSearchService.searchShops(request);
    return Result.success("Search success", result);
  }
}
