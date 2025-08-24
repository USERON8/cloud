package com.cloud.product.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
 * 商品管理控制器
 * 提供商品的增删改查及分页查询功能
 */
@Slf4j
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Tag(name = "商品管理", description = "商品管理接口")
public class ProductController {

    private final ProductService productService;
    private final ProductConverter productConverter = ProductConverter.INSTANCE;

    /**
     * 创建商品
     *
     * @param productDTO 商品信息
     * @return 创建结果
     */
    @PostMapping
    @Operation(summary = "创建商品", description = "创建新的商品")
    @ApiResponse(responseCode = "200", description = "创建成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<ProductDTO> createProduct(
            @Parameter(description = "商品信息") @RequestBody ProductDTO productDTO) {
        try {
            log.info("创建商品: {}", productDTO.getName());

            Product product = productConverter.toEntity(productDTO);
            boolean saved = productService.save(product);

            if (saved) {
                Product savedProduct = productService.getById(product.getId());
                ProductDTO savedProductDTO = productConverter.toDTO(savedProduct);
                log.info("创建商品成功，商品ID: {}", savedProduct.getId());
                return Result.success(savedProductDTO);
            } else {
                log.error("创建商品失败: {}", productDTO.getName());
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "创建商品失败");
            }
        } catch (Exception e) {
            log.error("创建商品失败", e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "创建商品失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取商品
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
     * 更新商品
     *
     * @param id         商品ID
     * @param productDTO 商品信息
     * @return 更新结果
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新商品", description = "更新商品信息")
    @ApiResponse(responseCode = "200", description = "更新成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<ProductDTO> updateProduct(
            @Parameter(description = "商品ID") @PathVariable Long id,
            @Parameter(description = "商品信息") @RequestBody ProductDTO productDTO) {
        try {
            log.info("更新商品，商品ID: {}", id);

            Product existingProduct = productService.getById(id);
            if (existingProduct == null) {
                log.warn("商品不存在，商品ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "商品不存在");
            }

            Product product = productConverter.toEntity(productDTO);
            product.setId(id); // 确保ID一致
            boolean updated = productService.updateById(product);

            if (updated) {
                Product updatedProduct = productService.getById(id);
                ProductDTO updatedProductDTO = productConverter.toDTO(updatedProduct);
                log.info("更新商品成功，商品ID: {}", id);
                return Result.success(updatedProductDTO);
            } else {
                log.error("更新商品失败，商品ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "更新商品失败");
            }
        } catch (Exception e) {
            log.error("更新商品失败，商品ID: {}", id, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "更新商品失败: " + e.getMessage());
        }
    }

    /**
     * 删除商品
     *
     * @param id 商品ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除商品", description = "删除商品")
    @ApiResponse(responseCode = "200", description = "删除成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<Void> deleteProduct(
            @Parameter(description = "商品ID") @PathVariable Long id) {
        try {
            log.info("删除商品，商品ID: {}", id);

            Product existingProduct = productService.getById(id);
            if (existingProduct == null) {
                log.warn("商品不存在，商品ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "商品不存在");
            }

            boolean removed = productService.removeById(id);
            if (removed) {
                log.info("删除商品成功，商品ID: {}", id);
                return Result.success();
            } else {
                log.error("删除商品失败，商品ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "删除商品失败");
            }
        } catch (Exception e) {
            log.error("删除商品失败，商品ID: {}", id, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "删除商品失败: " + e.getMessage());
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
    @GetMapping
    @Operation(summary = "分页查询商品", description = "分页查询商品列表")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<Page<ProductDTO>> getProducts(
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
            Page<ProductDTO> dtoPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
            List<ProductDTO> dtoList = productConverter.toDTOList(resultPage.getRecords());
            dtoPage.setRecords(dtoList);

            log.info("分页查询商品成功，共{}条记录", dtoPage.getTotal());
            return Result.success(dtoPage);
        } catch (Exception e) {
            log.error("分页查询商品失败", e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "分页查询商品失败: " + e.getMessage());
        }
    }
}