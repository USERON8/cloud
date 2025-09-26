package com.cloud.product.controller.product;

import com.cloud.common.domain.vo.ProductVO;
import com.cloud.common.result.PageResult;
import com.cloud.common.result.Result;
import com.cloud.product.module.dto.ProductPageDTO;
import com.cloud.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品查询控制器
 * 提供商品查询相关的REST API接口
 * 遵循用户服务标准，使用多级缓存提升性能
 *
 * @author what's up
 */
@Slf4j
@RestController
@RequestMapping("/query/products")
@RequiredArgsConstructor
@Validated
@Tag(name = "商品查询接口", description = "提供商品查询相关功能")
public class ProductQueryController {

    private final ProductService productService;

    // ================= 单个查询 =================

    @GetMapping("/{id}")
    @Operation(summary = "获取商品详情", description = "根据ID获取商品详细信息")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ProductVO.class)))
    public Result<ProductVO> getProductById(
            @Parameter(description = "商品ID", required = true)
            @PathVariable
            @NotNull(message = "商品ID不能为空")
            @Positive(message = "商品ID必须为正整数") Long id) {

        log.debug("获取商品详情: {}", id);
        ProductVO productVO = productService.getProductById(id);

        if (productVO == null) {
            return Result.error("商品不存在");
        }

        return Result.success(productVO);
    }

    @GetMapping("/batch")
    @Operation(summary = "批量获取商品", description = "根据ID列表批量获取商品信息")
    public Result<List<ProductVO>> getProductsByIds(
            @Parameter(description = "商品ID列表", required = true)
            @RequestParam("ids") List<Long> ids) {

        log.debug("批量获取商品: {}", ids);

        if (ids == null || ids.isEmpty()) {
            return Result.error("ID列表不能为空");
        }

        if (ids.size() > 100) {
            return Result.error("ID列表数量不能超过100个");
        }

        List<ProductVO> products = productService.getProductsByIds(ids);
        return Result.success(products);
    }

    // ================= 分页查询 =================

    @GetMapping("/page")
    @Operation(summary = "商品分页查询", description = "根据条件分页查询商品列表")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PageResult.class)))
    public Result<PageResult<ProductVO>> getProductsPage(
            @Valid @ModelAttribute ProductPageDTO pageDTO) {

        log.debug("商品分页查询: {}", pageDTO);
        PageResult<ProductVO> pageResult = productService.getProductsPage(pageDTO);
        return Result.success(pageResult);
    }

    // ================= 条件查询 =================

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "根据分类查询商品", description = "获取指定分类下的商品列表")
    public Result<List<ProductVO>> getProductsByCategoryId(
            @Parameter(description = "分类ID", required = true)
            @PathVariable
            @NotNull(message = "分类ID不能为空")
            @Positive(message = "分类ID必须为正整数") Long categoryId,

            @Parameter(description = "商品状态：1-上架，0-下架")
            @RequestParam(value = "status", required = false)
            @Min(value = 0, message = "状态值错误")
            @Max(value = 1, message = "状态值错误") Integer status) {

        log.debug("根据分类查询商品: categoryId={}, status={}", categoryId, status);
        List<ProductVO> products = productService.getProductsByCategoryId(categoryId, status);
        return Result.success(products);
    }

    @GetMapping("/brand/{brandId}")
    @Operation(summary = "根据品牌查询商品", description = "获取指定品牌下的商品列表")
    public Result<List<ProductVO>> getProductsByBrandId(
            @Parameter(description = "品牌ID", required = true)
            @PathVariable
            @NotNull(message = "品牌ID不能为空")
            @Positive(message = "品牌ID必须为正整数") Long brandId,

            @Parameter(description = "商品状态：1-上架，0-下架")
            @RequestParam(value = "status", required = false)
            @Min(value = 0, message = "状态值错误")
            @Max(value = 1, message = "状态值错误") Integer status) {

        log.debug("根据品牌查询商品: brandId={}, status={}", brandId, status);
        List<ProductVO> products = productService.getProductsByBrandId(brandId, status);
        return Result.success(products);
    }

    @GetMapping("/search")
    @Operation(summary = "搜索商品", description = "根据商品名称模糊搜索")
    public Result<List<ProductVO>> searchProductsByName(
            @Parameter(description = "搜索关键字", required = true)
            @RequestParam("keyword") String keyword,

            @Parameter(description = "商品状态：1-上架，0-下架")
            @RequestParam(value = "status", required = false)
            @Min(value = 0, message = "状态值错误")
            @Max(value = 1, message = "状态值错误") Integer status) {

        log.debug("搜索商品: keyword={}, status={}", keyword, status);

        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.error("搜索关键字不能为空");
        }

        List<ProductVO> products = productService.searchProductsByName(keyword.trim(), status);
        return Result.success(products);
    }

    // ================= 统计信息 =================

    @GetMapping("/stats/total")
    @Operation(summary = "获取商品总数", description = "获取系统中所有商品的数量")
    public Result<Long> getTotalProductCount() {
        log.debug("获取商品总数");
        Long count = productService.getTotalProductCount();
        return Result.success(count);
    }

    @GetMapping("/stats/enabled")
    @Operation(summary = "获取上架商品数量", description = "获取已上架商品的数量")
    public Result<Long> getEnabledProductCount() {
        log.debug("获取上架商品数量");
        Long count = productService.getEnabledProductCount();
        return Result.success(count);
    }

    @GetMapping("/stats/disabled")
    @Operation(summary = "获取下架商品数量", description = "获取已下架商品的数量")
    public Result<Long> getDisabledProductCount() {
        log.debug("获取下架商品数量");
        Long count = productService.getDisabledProductCount();
        return Result.success(count);
    }

    @GetMapping("/stats/category/{categoryId}")
    @Operation(summary = "获取分类商品数量", description = "获取指定分类下的商品数量")
    public Result<Long> getProductCountByCategoryId(
            @Parameter(description = "分类ID", required = true)
            @PathVariable
            @NotNull(message = "分类ID不能为空")
            @Positive(message = "分类ID必须为正整数") Long categoryId) {

        log.debug("获取分类商品数量: {}", categoryId);
        Long count = productService.getProductCountByCategoryId(categoryId);
        return Result.success(count);
    }

    @GetMapping("/stats/brand/{brandId}")
    @Operation(summary = "获取品牌商品数量", description = "获取指定品牌下的商品数量")
    public Result<Long> getProductCountByBrandId(
            @Parameter(description = "品牌ID", required = true)
            @PathVariable
            @NotNull(message = "品牌ID不能为空")
            @Positive(message = "品牌ID必须为正整数") Long brandId) {

        log.debug("获取品牌商品数量: {}", brandId);
        Long count = productService.getProductCountByBrandId(brandId);
        return Result.success(count);
    }

    // ================= 缓存管理 =================

    @PostMapping("/cache/warmup")
    @Operation(summary = "预热商品缓存", description = "根据指定ID列表预热商品缓存")
    public Result<String> warmupProductCache(
            @Parameter(description = "需要预热的商品ID列表", required = true)
            @RequestBody List<Long> ids) {

        log.info("预热商品缓存: {}", ids);

        if (ids == null || ids.isEmpty()) {
            return Result.error("ID列表不能为空");
        }

        if (ids.size() > 500) {
            return Result.error("预热数量不能超过500个");
        }

        productService.warmupProductCache(ids);
        return Result.success("缓存预热完成");
    }
}
