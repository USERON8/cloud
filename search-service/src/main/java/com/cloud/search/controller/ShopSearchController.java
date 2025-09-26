package com.cloud.search.controller;

import com.cloud.common.result.Result;
import com.cloud.search.document.ShopDocument;
import com.cloud.search.dto.SearchResult;
import com.cloud.search.dto.ShopSearchRequest;
import com.cloud.search.service.ShopSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 店铺搜索控制器
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/search/shops")
@RequiredArgsConstructor
@Tag(name = "店铺搜索", description = "店铺搜索相关接口")
@Validated
public class ShopSearchController {

    private final ShopSearchService shopSearchService;

    @Operation(summary = "复杂店铺搜索", description = "支持多条件组合的复杂店铺搜索，包含聚合、高亮、排序等功能")
    @PostMapping("/complex-search")
    public Result<SearchResult<ShopDocument>> complexSearch(@Valid @RequestBody ShopSearchRequest request) {
        try {
            log.info("复杂店铺搜索请求 - 关键字: {}, 商家ID: {}, 状态: {}",
                    request.getKeyword(), request.getMerchantId(), request.getStatus());

            SearchResult<ShopDocument> result = shopSearchService.searchShops(request);

            log.info("✅ 复杂店铺搜索完成 - 总数: {}, 耗时: {}ms", result.getTotal(), result.getTook());
            return Result.success("搜索成功", result);

        } catch (Exception e) {
            log.error("❌ 复杂店铺搜索失败 - 错误: {}", e.getMessage(), e);
            return Result.error("搜索失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取店铺筛选聚合信息", description = "获取店铺搜索的筛选聚合信息，用于构建筛选条件")
    @PostMapping("/filters")
    public Result<SearchResult<ShopDocument>> getShopFilters(@Valid @RequestBody ShopSearchRequest request) {
        try {
            log.info("获取店铺筛选聚合信息请求");

            SearchResult<ShopDocument> result = shopSearchService.getShopFilters(request);

            log.info("✅ 获取店铺筛选聚合信息完成");
            return Result.success("获取筛选信息成功", result);

        } catch (Exception e) {
            log.error("❌ 获取店铺筛选聚合信息失败 - 错误: {}", e.getMessage(), e);
            return Result.error("获取筛选信息失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取店铺搜索建议", description = "根据输入关键字获取店铺搜索建议")
    @GetMapping("/suggestions")
    public Result<List<String>> getSearchSuggestions(
            @Parameter(description = "搜索关键字") @RequestParam String keyword,
            @Parameter(description = "建议数量") @RequestParam(defaultValue = "10") Integer size) {
        try {
            log.info("获取店铺搜索建议请求 - 关键字: {}, 数量: {}", keyword, size);

            List<String> suggestions = shopSearchService.getSearchSuggestions(keyword, size);

            log.info("✅ 获取店铺搜索建议完成 - 数量: {}", suggestions.size());
            return Result.success("获取建议成功", suggestions);

        } catch (Exception e) {
            log.error("❌ 获取店铺搜索建议失败 - 错误: {}", e.getMessage(), e);
            return Result.error("获取建议失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取热门店铺", description = "获取当前热门的店铺")
    @GetMapping("/hot-shops")
    public Result<List<ShopDocument>> getHotShops(
            @Parameter(description = "店铺数量") @RequestParam(defaultValue = "10") Integer size) {
        try {
            log.info("获取热门店铺请求 - 数量: {}", size);

            List<ShopDocument> hotShops = shopSearchService.getHotShops(size);

            log.info("✅ 获取热门店铺完成 - 数量: {}", hotShops.size());
            return Result.success("获取热门店铺成功", hotShops);

        } catch (Exception e) {
            log.error("❌ 获取热门店铺失败 - 错误: {}", e.getMessage(), e);
            return Result.error("获取热门店铺失败: " + e.getMessage());
        }
    }

    @Operation(summary = "根据店铺ID查询", description = "根据店铺ID查询店铺详情")
    @GetMapping("/{shopId}")
    public Result<ShopDocument> getShopById(@Parameter(description = "店铺ID") @PathVariable Long shopId) {
        try {
            log.info("根据店铺ID查询 - 店铺ID: {}", shopId);

            ShopDocument shop = shopSearchService.findByShopId(shopId);

            if (shop != null) {
                log.info("✅ 店铺查询成功 - 店铺ID: {}, 店铺名称: {}", shopId, shop.getShopName());
                return Result.success("查询成功", shop);
            } else {
                log.warn("⚠️ 店铺不存在 - 店铺ID: {}", shopId);
                return Result.error("店铺不存在");
            }

        } catch (Exception e) {
            log.error("❌ 店铺查询失败 - 店铺ID: {}, 错误: {}", shopId, e.getMessage(), e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @Operation(summary = "推荐店铺", description = "获取推荐店铺列表")
    @GetMapping("/recommended")
    public Result<SearchResult<ShopDocument>> getRecommendedShops(
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size) {
        try {
            log.info("获取推荐店铺请求 - 页码: {}, 大小: {}", page, size);

            ShopSearchRequest request = new ShopSearchRequest();
            request.setRecommended(true);
            request.setStatus(1); // 只查询营业中的店铺
            request.setPage(page);
            request.setSize(size);
            request.setSortBy("hotScore");
            request.setSortOrder("desc");

            SearchResult<ShopDocument> result = shopSearchService.searchShops(request);

            log.info("✅ 获取推荐店铺完成 - 总数: {}", result.getTotal());
            return Result.success("获取推荐店铺成功", result);

        } catch (Exception e) {
            log.error("❌ 获取推荐店铺失败 - 错误: {}", e.getMessage(), e);
            return Result.error("获取推荐店铺失败: " + e.getMessage());
        }
    }

    @Operation(summary = "按地区搜索店铺", description = "根据地区关键字搜索店铺")
    @GetMapping("/by-location")
    public Result<SearchResult<ShopDocument>> searchShopsByLocation(
            @Parameter(description = "地区关键字") @RequestParam String location,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size) {
        try {
            log.info("按地区搜索店铺请求 - 地区: {}, 页码: {}, 大小: {}", location, page, size);

            ShopSearchRequest request = new ShopSearchRequest();
            request.setAddressKeyword(location);
            request.setStatus(1); // 只查询营业中的店铺
            request.setPage(page);
            request.setSize(size);
            request.setSortBy("rating");
            request.setSortOrder("desc");

            SearchResult<ShopDocument> result = shopSearchService.searchShops(request);

            log.info("✅ 按地区搜索店铺完成 - 地区: {}, 总数: {}", location, result.getTotal());
            return Result.success("搜索成功", result);

        } catch (Exception e) {
            log.error("❌ 按地区搜索店铺失败 - 地区: {}, 错误: {}", location, e.getMessage(), e);
            return Result.error("搜索失败: " + e.getMessage());
        }
    }
}
