package com.cloud.search.controller;

import com.cloud.common.result.Result;
import com.cloud.search.document.ProductDocument;
import com.cloud.search.repository.ProductDocumentRepository;
import com.cloud.search.service.ElasticsearchOptimizedService;
import com.cloud.search.service.ProductSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
public class ProductSearchController {

    private final ProductSearchService productSearchService;
    private final ProductDocumentRepository productDocumentRepository;
    private final ElasticsearchOptimizedService elasticsearchOptimizedService;

    @Operation(summary = "关键词搜索商品", description = "根据关键词搜索商品，支持中文分词和拼音搜索")
    @GetMapping("/search")
    public Result<Page<ProductDocument>> searchProducts(
            @Parameter(description = "搜索关键词") @RequestParam String keyword,
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "hotScore") String sortBy,
            @Parameter(description = "排序方向") @RequestParam(defaultValue = "desc") String sortDir) {

        try {
            log.info("商品搜索请求 - 关键词: {}, 页码: {}, 大小: {}", keyword, page, size);

            Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

            Page<ProductDocument> result = productDocumentRepository.searchByKeyword(keyword, pageable);

            log.info("商品搜索完成 - 关键词: {}, 结果数量: {}", keyword, result.getTotalElements());
            return Result.success("搜索成功", result);

        } catch (Exception e) {
            log.error("商品搜索失败 - 关键词: {}, 错误: {}", keyword, e.getMessage(), e);
            return Result.error("搜索失败: " + e.getMessage());
        }
    }

    @Operation(summary = "分类商品搜索", description = "在指定分类下搜索商品")
    @GetMapping("/search/category/{categoryId}")
    public Result<Page<ProductDocument>> searchByCategory(
            @Parameter(description = "分类ID") @PathVariable Long categoryId,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {

        try {
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

        } catch (Exception e) {
            log.error("分类商品搜索失败 - 分类ID: {}, 关键词: {}, 错误: {}",
                    categoryId, keyword, e.getMessage(), e);
            return Result.error("搜索失败: " + e.getMessage());
        }
    }

    @Operation(summary = "店铺商品搜索", description = "在指定店铺下搜索商品")
    @GetMapping("/search/shop/{shopId}")
    public Result<Page<ProductDocument>> searchByShop(
            @Parameter(description = "店铺ID") @PathVariable Long shopId,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {

        try {
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

        } catch (Exception e) {
            log.error("店铺商品搜索失败 - 店铺ID: {}, 关键词: {}, 错误: {}",
                    shopId, keyword, e.getMessage(), e);
            return Result.error("搜索失败: " + e.getMessage());
        }
    }

    @Operation(summary = "高级搜索", description = "支持多条件组合的高级搜索")
    @GetMapping("/search/advanced")
    public Result<Page<ProductDocument>> advancedSearch(
            @Parameter(description = "搜索关键词") @RequestParam String keyword,
            @Parameter(description = "最低价格") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "最高价格") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "hotScore"));
            BigDecimal min = minPrice != null ? minPrice : BigDecimal.ZERO;
            BigDecimal max = maxPrice != null ? maxPrice : new BigDecimal("999999");

            Page<ProductDocument> result = productDocumentRepository.advancedSearch(keyword, min, max, pageable);

            log.info("高级搜索完成 - 关键词: {}, 价格区间: {}-{}, 结果数量: {}",
                    keyword, min, max, result.getTotalElements());
            return Result.success("搜索成功", result);

        } catch (Exception e) {
            log.error("高级搜索失败 - 关键词: {}, 错误: {}", keyword, e.getMessage(), e);
            return Result.error("搜索失败: " + e.getMessage());
        }
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

        try {
            log.info("智能搜索请求 - 关键词: {}, 分类: {}, 价格区间: [{}, {}], 页码: {}",
                    keyword, categoryId, minPrice, maxPrice, page);

            int from = (page - 1) * size;
            ElasticsearchOptimizedService.SearchResult result = elasticsearchOptimizedService
                    .smartProductSearch(keyword, categoryId, minPrice, maxPrice,
                                      sortField, sortOrder, from, size);

            return Result.success("搜索成功", result);

        } catch (Exception e) {
            log.error("智能搜索失败 - 关键词: {}, 错误: {}", keyword, e.getMessage(), e);
            return Result.error("搜索失败: " + e.getMessage());
        }
    }

    @Operation(summary = "搜索建议", description = "获取搜索关键词建议")
    @GetMapping("/suggestions")
    public Result<List<String>> getSearchSuggestions(
            @Parameter(description = "输入关键词") @RequestParam String keyword,
            @Parameter(description = "建议数量") @RequestParam(defaultValue = "10") int limit) {

        try {
            List<String> suggestions = elasticsearchOptimizedService.getSearchSuggestions(keyword, limit);

            log.info("搜索建议完成 - 关键词: {}, 建议数量: {}", keyword, suggestions.size());
            return Result.success("获取建议成功", suggestions);

        } catch (Exception e) {
            log.error("获取搜索建议失败 - 关键词: {}, 错误: {}", keyword, e.getMessage(), e);
            return Result.error("获取建议失败: " + e.getMessage());
        }
    }

    @Operation(summary = "热门搜索词", description = "获取热门搜索关键词")
    @GetMapping("/hot-keywords")
    public Result<List<String>> getHotSearchKeywords(
            @Parameter(description = "数量限制") @RequestParam(defaultValue = "10") int limit) {

        try {
            List<String> hotKeywords = elasticsearchOptimizedService.getHotSearchKeywords(limit);

            log.info("热门搜索词获取完成 - 数量: {}", hotKeywords.size());
            return Result.success("获取热门搜索词成功", hotKeywords);

        } catch (Exception e) {
            log.error("获取热门搜索词失败 - 错误: {}", e.getMessage(), e);
            return Result.error("获取热门搜索词失败: " + e.getMessage());
        }
    }

    @Operation(summary = "推荐商品", description = "获取推荐商品列表")
    @GetMapping("/recommended")
    public Result<Page<ProductDocument>> getRecommendedProducts(
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "hotScore"));
            Page<ProductDocument> result = productDocumentRepository.findByRecommendedTrue(pageable);

            log.info("推荐商品查询完成 - 结果数量: {}", result.getTotalElements());
            return Result.success("获取推荐商品成功", result);

        } catch (Exception e) {
            log.error("获取推荐商品失败 - 错误: {}", e.getMessage(), e);
            return Result.error("获取推荐商品失败: " + e.getMessage());
        }
    }

    @Operation(summary = "新品推荐", description = "获取新品列表")
    @GetMapping("/new")
    public Result<Page<ProductDocument>> getNewProducts(
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<ProductDocument> result = productDocumentRepository.findByIsNewTrue(pageable);

            log.info("新品查询完成 - 结果数量: {}", result.getTotalElements());
            return Result.success("获取新品成功", result);

        } catch (Exception e) {
            log.error("获取新品失败 - 错误: {}", e.getMessage(), e);
            return Result.error("获取新品失败: " + e.getMessage());
        }
    }

    @Operation(summary = "热销商品", description = "获取热销商品列表")
    @GetMapping("/hot")
    public Result<Page<ProductDocument>> getHotProducts(
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "salesCount"));
            Page<ProductDocument> result = productDocumentRepository.findByIsHotTrue(pageable);

            log.info("热销商品查询完成 - 结果数量: {}", result.getTotalElements());
            return Result.success("获取热销商品成功", result);

        } catch (Exception e) {
            log.error("获取热销商品失败 - 错误: {}", e.getMessage(), e);
            return Result.error("获取热销商品失败: " + e.getMessage());
        }
    }

    @Operation(summary = "重建商品索引", description = "重建商品搜索索引")
    @PostMapping("/rebuild-index")
    public Result<String> rebuildIndex() {
        try {
            productSearchService.rebuildProductIndex();
            log.info("商品索引重建完成");
            return Result.success("索引重建成功", "索引重建成功");

        } catch (Exception e) {
            log.error("重建商品索引失败 - 错误: {}", e.getMessage(), e);
            return Result.error("重建索引失败: " + e.getMessage());
        }
    }
}
