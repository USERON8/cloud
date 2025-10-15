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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品搜索控制器
 * 提供商品搜索相关的API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "商品搜索", description = "商品搜索相关接口")
@Validated
public class ProductSearchController {

    private final ProductSearchService productSearchService;
    private final ProductDocumentRepository productDocumentRepository;
    private final ElasticsearchOptimizedService elasticsearchOptimizedService;
    private final SearchRequestMapper searchRequestMapper;


    @Operation(summary = "复杂商品搜索", description = "支持多条件组合的复杂商品搜索，包含聚合、高亮、排序等功能")
    @PostMapping("/complex-search")
    @PreAuthorize("hasAuthority('SCOPE_search:read')")
    public Result<SearchResult<ProductDocument>> complexSearch(@Valid @RequestBody ProductSearchRequest request) {
        log.info("复杂商品搜索请求 - 关键字: {}, 分类: {}, 品牌: {}, 价格范围: {}-{}",
                request.getKeyword(), request.getCategoryName(), request.getBrandName(),
                request.getMinPrice(), request.getMaxPrice());

        SearchResult<ProductDocument> result = productSearchService.searchProducts(request);

        log.info("✅ 复杂商品搜索完成 - 总数: {}, 耗时: {}ms", result.getTotal(), result.getTook());
        return Result.success("搜索成功", result);
    }

    @Operation(summary = "获取商品筛选聚合信息", description = "获取商品搜索的筛选聚合信息，用于构建筛选条件")
    @PostMapping("/filters")
    public Result<SearchResult<ProductDocument>> getProductFilters(@Valid @RequestBody ProductSearchRequest request) {
        log.info("获取商品筛选聚合信息请求");

        SearchResult<ProductDocument> result = productSearchService.getProductFilters(request);

        log.info("✅ 获取商品筛选聚合信息完成");
        return Result.success("获取筛选信息成功", result);
    }

    @Operation(summary = "获取搜索建议", description = "根据输入关键字获取搜索建议")
    @GetMapping("/suggestions")
    public Result<List<String>> getSearchSuggestions(
            @Parameter(description = "搜索关键字") @RequestParam String keyword,
            @Parameter(description = "建议数量") @RequestParam(defaultValue = "10") Integer size) {
        log.info("获取搜索建议请求 - 关键字: {}, 数量: {}", keyword, size);

        List<String> suggestions = productSearchService.getSearchSuggestions(keyword, size);

        log.info("✅ 获取搜索建议完成 - 数量: {}", suggestions.size());
        return Result.success("获取建议成功", suggestions);
    }

    @Operation(summary = "获取热门搜索关键字", description = "获取当前热门的搜索关键字")
    @GetMapping("/hot-keywords")
    public Result<List<String>> getHotSearchKeywords(
            @Parameter(description = "关键字数量") @RequestParam(defaultValue = "10") Integer size) {
        log.info("获取热门搜索关键字请求 - 数量: {}", size);

        List<String> hotKeywords = productSearchService.getHotSearchKeywords(size);

        log.info("✅ 获取热门搜索关键字完成 - 数量: {}", hotKeywords.size());
        return Result.success("获取热门关键字成功", hotKeywords);
    }

    @Operation(summary = "关键词搜索商品", description = "根据关键词搜索商品，支持中文分词和拼音搜索")
    @GetMapping("/search")
    public Result<Page<ProductDocument>> searchProducts(
            @Parameter(description = "搜索关键词") @RequestParam String keyword,
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "hotScore") String sortBy,
            @Parameter(description = "排序方向") @RequestParam(defaultValue = "desc") String sortDir) {

        log.info("商品搜索请求 - 关键词: {}, 页码: {}, 大小: {}", keyword, page, size);

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<ProductDocument> result = productDocumentRepository.searchByKeyword(keyword, pageable);

        log.info("商品搜索完成 - 关键词: {}, 结果数量: {}", keyword, result.getTotalElements());
        return Result.success("搜索成功", result);
    }

    @Operation(summary = "分类商品搜索", description = "在指定分类下搜索商品")
    @GetMapping("/search/category/{categoryId}")
    public Result<Page<ProductDocument>> searchByCategory(
            @Parameter(description = "分类ID") @PathVariable Long categoryId,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "hotScore"));

        Page<ProductDocument> result;
        if (keyword != null && !keyword.trim().isEmpty()) {
            result = productDocumentRepository.searchByKeywordAndCategory(keyword, categoryId, pageable);
        } else {
            result = productDocumentRepository.findByCategoryId(categoryId, pageable);
        }

        log.info("分类商品搜索完成 - 分类ID: {}, 关键词: {}, 结果数量: {}",
                categoryId, keyword, result.getTotalElements());
        return Result.success("搜索成功", result);
    }

    @Operation(summary = "店铺商品搜索", description = "在指定店铺下搜索商品")
    @GetMapping("/search/shop/{shopId}")
    public Result<Page<ProductDocument>> searchByShop(
            @Parameter(description = "店铺ID") @PathVariable Long shopId,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "hotScore"));

        Page<ProductDocument> result;
        if (keyword != null && !keyword.trim().isEmpty()) {
            result = productDocumentRepository.searchByKeywordAndShop(keyword, shopId, pageable);
        } else {
            result = productDocumentRepository.findByShopId(shopId, pageable);
        }

        log.info("店铺商品搜索完成 - 店铺ID: {}, 关键词: {}, 结果数量: {}",
                shopId, keyword, result.getTotalElements());
        return Result.success("搜索成功", result);
    }

    @Operation(summary = "高级搜索", description = "支持多条件组合的高级搜索")
    @GetMapping("/search/advanced")
    public Result<Page<ProductDocument>> advancedSearch(
            @Parameter(description = "搜索关键词") @RequestParam String keyword,
            @Parameter(description = "最低价格") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "最高价格") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "hotScore"));
        BigDecimal min = minPrice != null ? minPrice : BigDecimal.ZERO;
        BigDecimal max = maxPrice != null ? maxPrice : new BigDecimal("999999");

        Page<ProductDocument> result = productDocumentRepository.advancedSearch(keyword, min, max, pageable);

        log.info("高级搜索完成 - 关键词: {}, 价格区间: {}-{}, 结果数量: {}",
                keyword, min, max, result.getTotalElements());
        return Result.success("搜索成功", result);
    }

    @Operation(summary = "智能搜索", description = "使用优化的ES引擎进行智能搜索")
    @GetMapping("/smart-search")
    public Result<ElasticsearchOptimizedService.SearchResult> smartSearch(
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "最低价格") @RequestParam(required = false) Double minPrice,
            @Parameter(description = "最高价格") @RequestParam(required = false) Double maxPrice,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "score") String sortField,
            @Parameter(description = "排序方向") @RequestParam(defaultValue = "desc") String sortOrder,
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {

        log.info("智能搜索请求 - 关键词: {}, 分类: {}, 价格区间: [{}, {}], 页码: {}",
                keyword, categoryId, minPrice, maxPrice, page);

        int from = (page - 1) * size;
        ElasticsearchOptimizedService.SearchResult result = elasticsearchOptimizedService
                .smartProductSearch(keyword, categoryId, minPrice, maxPrice,
                        sortField, sortOrder, from, size);

        return Result.success("搜索成功", result);
    }


    @Operation(summary = "推荐商品", description = "获取推荐商品列表")
    @GetMapping("/recommended")
    public Result<Page<ProductDocument>> getRecommendedProducts(
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "hotScore"));
        Page<ProductDocument> result = productDocumentRepository.findByRecommendedTrue(pageable);

        log.info("推荐商品查询完成 - 结果数量: {}", result.getTotalElements());
        return Result.success("获取推荐商品成功", result);
    }

    @Operation(summary = "新品推荐", description = "获取新品列表")
    @GetMapping("/new")
    public Result<Page<ProductDocument>> getNewProducts(
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ProductDocument> result = productDocumentRepository.findByIsNewTrue(pageable);

        log.info("新品查询完成 - 结果数量: {}", result.getTotalElements());
        return Result.success("获取新品成功", result);
    }

    @Operation(summary = "热销商品", description = "获取热销商品列表")
    @GetMapping("/hot")
    public Result<Page<ProductDocument>> getHotProducts(
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "salesCount"));
        Page<ProductDocument> result = productDocumentRepository.findByIsHotTrue(pageable);

        log.info("热销商品查询完成 - 结果数量: {}", result.getTotalElements());
        return Result.success("获取热销商品成功", result);
    }

    @Operation(summary = "重建商品索引", description = "重建商品搜索索引")
    @PostMapping("/rebuild-index")
    @PreAuthorize("@permissionManager.hasAdminAccess(authentication)")
    public Result<String> rebuildIndex() {
        productSearchService.rebuildProductIndex();
        log.info("商品索引重庺完成");
        return Result.success("索引重庺成功", "索引重庺成功");
    }

    // ==================== 新增的基础搜索和筛选接口 ====================

    @Operation(summary = "基础搜索", description = "根据关键字进行简单搜索")
    @GetMapping("/basic")
    public Result<SearchResult<ProductDocument>> basicSearch(
            @Parameter(description = "搜索关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size) {

        log.info("基础搜索请求 - 关键字: {}, 页码: {}, 大小: {}", keyword, page, size);

        SearchResult<ProductDocument> result = productSearchService.basicSearch(keyword, page, size);

        log.info("基础搜索完成 - 总数: {}, 耗时: {}ms", result.getTotal(), result.getTook());
        return Result.success("搜索成功", result);
    }

    @Operation(summary = "筛选搜索", description = "支持多条件组合的筛选搜索")
    @PostMapping("/filter")
    public Result<SearchResult<ProductDocument>> filterSearch(@Valid @RequestBody ProductFilterRequest request) {
        log.info("筛选搜索请求 - 关键字: {}, 分类: {}, 品牌: {}, 价格: {}-{}",
                request.getKeyword(), request.getCategoryId(), request.getBrandId(),
                request.getMinPrice(), request.getMaxPrice());

        // 使用MapStruct自动转换
        ProductSearchRequest searchRequest = searchRequestMapper.toSearchRequest(request);
        SearchResult<ProductDocument> result = productSearchService.filterSearch(searchRequest);

        log.info("筛选搜索完成 - 总数: {}, 耗时: {}ms", result.getTotal(), result.getTook());
        return Result.success("搜索成功", result);
    }

    @Operation(summary = "按分类筛选", description = "根据分类ID筛选商品")
    @GetMapping("/filter/category/{categoryId}")
    public Result<SearchResult<ProductDocument>> filterByCategory(
            @Parameter(description = "分类ID") @PathVariable Long categoryId,
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size) {

        log.info("按分类筛选请求 - 分类ID: {}, 页码: {}, 大小: {}", categoryId, page, size);

        SearchResult<ProductDocument> result = productSearchService.searchByCategory(categoryId, page, size);

        log.info("按分类筛选完成 - 总数: {}, 耗时: {}ms", result.getTotal(), result.getTook());
        return Result.success("筛选成功", result);
    }

    @Operation(summary = "按品牌筛选", description = "根据品牌ID筛选商品")
    @GetMapping("/filter/brand/{brandId}")
    public Result<SearchResult<ProductDocument>> filterByBrand(
            @Parameter(description = "品牌ID") @PathVariable Long brandId,
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size) {

        log.info("按品牌筛选请求 - 品牌ID: {}, 页码: {}, 大小: {}", brandId, page, size);

        SearchResult<ProductDocument> result = productSearchService.searchByBrand(brandId, page, size);

        log.info("按品牌筛选完成 - 总数: {}, 耗时: {}ms", result.getTotal(), result.getTook());
        return Result.success("筛选成功", result);
    }

    @Operation(summary = "按价格区间筛选", description = "根据价格区间筛选商品")
    @GetMapping("/filter/price")
    public Result<SearchResult<ProductDocument>> filterByPrice(
            @Parameter(description = "最低价格") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "最高价格") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size) {

        log.info("按价格区间筛选请求 - 价格范围: {}-{}, 页码: {}, 大小: {}", minPrice, maxPrice, page, size);

        SearchResult<ProductDocument> result = productSearchService.searchByPriceRange(minPrice, maxPrice, page, size);

        log.info("按价格区间筛选完成 - 总数: {}, 耗时: {}ms", result.getTotal(), result.getTook());
        return Result.success("筛选成功", result);
    }

    @Operation(summary = "按店铺筛选", description = "根据店铺ID筛选商品")
    @GetMapping("/filter/shop/{shopId}")
    public Result<SearchResult<ProductDocument>> filterByShop(
            @Parameter(description = "店铺ID") @PathVariable Long shopId,
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size) {

        log.info("按店铺筛选请求 - 店铺ID: {}, 页码: {}, 大小: {}", shopId, page, size);

        SearchResult<ProductDocument> result = productSearchService.searchByShop(shopId, page, size);

        log.info("按店铺筛选完成 - 总数: {}, 耗时: {}ms", result.getTotal(), result.getTook());
        return Result.success("筛选成功", result);
    }

    @Operation(summary = "组合筛选", description = "支持关键字、分类、品牌、价格、店铺等多条件组合筛选")
    @GetMapping("/filter/combined")
    public Result<SearchResult<ProductDocument>> combinedFilter(
            @Parameter(description = "搜索关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "品牌ID") @RequestParam(required = false) Long brandId,
            @Parameter(description = "最低价格") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "最高价格") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "店铺ID") @RequestParam(required = false) Long shopId,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "hotScore") String sortBy,
            @Parameter(description = "排序方向") @RequestParam(defaultValue = "desc") String sortOrder,
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size) {

        log.info("组合筛选请求 - 关键字: {}, 分类: {}, 品牌: {}, 价格: {}-{}, 店铺: {}",
                keyword, categoryId, brandId, minPrice, maxPrice, shopId);

        SearchResult<ProductDocument> result = productSearchService.combinedSearch(
                keyword, categoryId, brandId, minPrice, maxPrice, shopId,
                sortBy, sortOrder, page, size);

        log.info("组合筛选完成 - 总数: {}, 耗时: {}ms", result.getTotal(), result.getTook());
        return Result.success("筛选成功", result);
    }

}
