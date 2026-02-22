package com.cloud.search.controller;

import com.cloud.common.exception.ResourceNotFoundException;
import com.cloud.common.result.Result;
import com.cloud.search.document.ShopDocument;
import com.cloud.search.dto.SearchResult;
import com.cloud.search.dto.ShopSearchRequest;
import com.cloud.search.service.ShopSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search/shops")
@RequiredArgsConstructor
@Tag(name = "Shop Search", description = "Shop search APIs")
@Validated
public class ShopSearchController {

    private final ShopSearchService shopSearchService;

    @Operation(summary = "Complex shop search", description = "Search shops with rich conditions")
    @PostMapping("/complex-search")
    public Result<SearchResult<ShopDocument>> complexSearch(@Valid @RequestBody ShopSearchRequest request) {
        SearchResult<ShopDocument> result = shopSearchService.searchShops(request);
        return Result.success("Search success", result);
    }

    @Operation(summary = "Shop filter data", description = "Get shop filters by request")
    @PostMapping("/filters")
    public Result<SearchResult<ShopDocument>> getShopFilters(@Valid @RequestBody ShopSearchRequest request) {
        SearchResult<ShopDocument> result = shopSearchService.getShopFilters(request);
        return Result.success("Get filters success", result);
    }

    @Operation(summary = "Shop suggestions", description = "Get shop suggestions by keyword")
    @GetMapping("/suggestions")
    public Result<List<String>> getSearchSuggestions(
            @Parameter(description = "Keyword") @RequestParam String keyword,
            @Parameter(description = "Size") @RequestParam(defaultValue = "10") Integer size) {

        List<String> suggestions = shopSearchService.getSearchSuggestions(keyword, size);
        return Result.success("Get suggestions success", suggestions);
    }

    @Operation(summary = "Hot shops", description = "Get hot shops")
    @GetMapping("/hot-shops")
    public Result<List<ShopDocument>> getHotShops(
            @Parameter(description = "Size") @RequestParam(defaultValue = "10") Integer size) {

        List<ShopDocument> hotShops = shopSearchService.getHotShops(size);
        return Result.success("Get hot shops success", hotShops);
    }

    @Operation(summary = "Get shop by id", description = "Get shop detail by id")
    @GetMapping("/{shopId}")
    public Result<ShopDocument> getShopById(@Parameter(description = "Shop id") @PathVariable Long shopId) {
        ShopDocument shop = shopSearchService.findByShopId(shopId);
        if (shop == null) {
            throw new ResourceNotFoundException("Shop", String.valueOf(shopId));
        }
        return Result.success("Query success", shop);
    }

    @Operation(summary = "Recommended shops", description = "Get recommended shops")
    @GetMapping("/recommended")
    public Result<SearchResult<ShopDocument>> getRecommendedShops(
            @Parameter(description = "Page") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Size") @RequestParam(defaultValue = "20") Integer size) {

        ShopSearchRequest request = new ShopSearchRequest();
        request.setRecommended(true);
        request.setStatus(1);
        request.setPage(page);
        request.setSize(size);
        request.setSortBy("hotScore");
        request.setSortOrder("desc");

        SearchResult<ShopDocument> result = shopSearchService.searchShops(request);
        return Result.success("Query recommended shops success", result);
    }

    @Operation(summary = "Search shops by location", description = "Search shops by address keyword")
    @GetMapping("/by-location")
    public Result<SearchResult<ShopDocument>> searchShopsByLocation(
            @Parameter(description = "Location keyword") @RequestParam String location,
            @Parameter(description = "Page") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Size") @RequestParam(defaultValue = "20") Integer size) {

        ShopSearchRequest request = new ShopSearchRequest();
        request.setAddressKeyword(location);
        request.setStatus(1);
        request.setPage(page);
        request.setSize(size);
        request.setSortBy("rating");
        request.setSortOrder("desc");

        SearchResult<ShopDocument> result = shopSearchService.searchShops(request);
        return Result.success("Search success", result);
    }
}
