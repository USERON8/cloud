package com.cloud.product.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.domain.PageResult;
import com.cloud.common.domain.Result;
import com.cloud.common.domain.dto.product.ProductDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.product.converter.ProductConverter;
import com.cloud.product.module.entity.Product;
import com.cloud.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品查询控制器
 * 提供商品查询相关功能
 */
@Slf4j
@RestController
@RequestMapping("/product/query")
@RequiredArgsConstructor
@Tag(name = "商品查询", description = "商品查询接口")
public class ProductQueryController {

    private final ProductService productService;
    private final ProductConverter productConverter = ProductConverter.INSTANCE;

    /**
     * 根据ID获取商品详情
     *
     * @param id 商品ID
     * @return 商品信息
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取商品详情", description = "根据ID获取商品详细信息")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<ProductDTO> getProductById(
            @Parameter(description = "商品ID") @PathVariable Long id) {
        try {
            log.info("获取商品详情，商品ID: {}", id);

            Product product = productService.getById(id);
            if (product == null) {
                log.warn("商品不存在，商品ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "商品不存在");
            }

            ProductDTO productDTO = productConverter.toDTO(product);
            log.info("获取商品详情成功，商品ID: {}", id);
            return Result.success(productDTO);
        } catch (Exception e) {
            log.error("获取商品详情失败，商品ID: {}", id, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "获取商品详情失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询商品
     *
     * @param page   页码
     * @param size   每页数量
     * @param name   商品名称（可选）
     * @param status 商品状态（可选）
     * @return 商品列表
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询商品", description = "分页查询商品列表")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PageResult.class)))
    public PageResult<ProductDTO> getProducts(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "商品名称") @RequestParam(required = false) String name,
            @Parameter(description = "商品状态") @RequestParam(required = false) Integer status) {
        try {
            log.info("分页查询商品，页码: {}，每页数量: {}，名称: {}，状态: {}", page, size, name, status);

            Page<Product> productPage = new Page<>(page, size);
            LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();

            // 添加查询条件
            if (name != null && !name.isEmpty()) {
                queryWrapper.like(Product::getName, name);
            }
            if (status != null) {
                queryWrapper.eq(Product::getStatus, status);
            }

            // 按创建时间倒序排列
            queryWrapper.orderByDesc(Product::getCreatedAt);

            Page<Product> resultPage = productService.page(productPage, queryWrapper);

            // 转换为DTO
            List<ProductDTO> dtoList = productConverter.toDTOList(resultPage.getRecords());

            log.info("分页查询商品成功，共{}条记录", resultPage.getTotal());
            return PageResult.of(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal(), dtoList);
        } catch (Exception e) {
            log.error("分页查询商品失败", e);
            return PageResult.of(page.longValue(), size.longValue(), 0L, null);
        }
    }
}