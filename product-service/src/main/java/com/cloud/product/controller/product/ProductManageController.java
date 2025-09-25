package com.cloud.product.controller.product;

import com.cloud.common.annotation.RequireScope;
import com.cloud.common.annotation.RequireUserType;
import com.cloud.common.annotation.RequireUserType.UserType;
import com.cloud.common.domain.dto.product.ProductRequestDTO;
import com.cloud.common.result.Result;
import com.cloud.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品管理控制器
 * 提供商品管理相关的REST API接口
 * 包含权限验证，遵循用户服务标准
 *
 * @author what's up
 */
@Slf4j
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Validated
@Tag(name = "商品管理接口", description = "提供商品管理相关功能，需要管理员权限")
public class ProductManageController {

    private final ProductService productService;

    // ================= 基础CRUD操作 =================

    @PostMapping
    @RequireUserType({UserType.MERCHANT, UserType.ADMIN})
    @RequireScope("product:create")
    @Operation(summary = "创建商品", description = "创建新的商品信息")
    @ApiResponse(responseCode = "200", description = "创建成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Long.class)))
    public Result<Long> createProduct(
            @Parameter(description = "商品信息", required = true)
            @RequestBody @Valid ProductRequestDTO requestDTO) {

        log.info("创建商品: {}", requestDTO.getName());
        Long productId = productService.createProduct(requestDTO);
        return Result.success(productId);
    }

    @PutMapping("/{id}")
    @RequireUserType({UserType.MERCHANT, UserType.ADMIN})
    @RequireScope("product:write")
    @Operation(summary = "更新商品", description = "更新商品信息")
    @ApiResponse(responseCode = "200", description = "更新成功")
    public Result<Boolean> updateProduct(
            @Parameter(description = "商品ID", required = true)
            @PathVariable
            @NotNull
            @Positive(message = "商品ID必须为正整数") Long id,

            @Parameter(description = "商品信息", required = true)
            @RequestBody @Valid ProductRequestDTO requestDTO) {

        log.info("更新商品: ID={}, Name={}", id, requestDTO.getName());
        Boolean success = productService.updateProduct(id, requestDTO);
        return Result.success(success);
    }

    @DeleteMapping("/{id}")
    @RequireUserType({UserType.MERCHANT, UserType.ADMIN})
    @RequireScope("product:write")
    @Operation(summary = "删除商品", description = "根据ID删除商品")
    @ApiResponse(responseCode = "200", description = "删除成功")
    public Result<Boolean> deleteProduct(
            @Parameter(description = "商品ID", required = true)
            @PathVariable
            @NotNull(message = "商品ID不能为空")
            @Positive(message = "商品ID必须为正整数") Long id) {

        log.info("删除商品: {}", id);
        Boolean success = productService.deleteProduct(id);
        return Result.success(success);
    }

    @DeleteMapping("/batch")
    @RequireUserType({UserType.MERCHANT, UserType.ADMIN})
    @RequireScope("product:write")
    @Operation(summary = "批量删除商品", description = "根据ID列表批量删除商品")
    @ApiResponse(responseCode = "200", description = "删除成功")
    public Result<Boolean> batchDeleteProducts(
            @Parameter(description = "商品ID列表", required = true)
            @RequestBody @NotEmpty(message = "ID列表不能为空") List<Long> ids) {

        log.info("批量删除商品: {}", ids);

        if (ids.size() > 100) {
            return Result.error();
        }

        Boolean success = productService.batchDeleteProducts(ids);
        return Result.success(success);
    }

    // ================= 状态管理 =================

    @PutMapping("/{id}/enable")
    @RequireUserType({UserType.MERCHANT, UserType.ADMIN})
    @RequireScope("product:write")
    @Operation(summary = "上架商品", description = "将商品设为上架状态")
    @ApiResponse(responseCode = "200", description = "上架成功")
    public Result<Boolean> enableProduct(
            @Parameter(description = "商品ID", required = true)
            @PathVariable
            @NotNull(message = "商品ID不能为空")
            @Positive(message = "商品ID必须为正整数") Long id) {

        log.info("上架商品: {}", id);
        Boolean success = productService.enableProduct(id);
        return Result.success(success);
    }

    @PutMapping("/{id}/disable")
    @RequireUserType({UserType.MERCHANT, UserType.ADMIN})
    @RequireScope("product:write")
    @Operation(summary = "下架商品", description = "将商品设为下架状态")
    @ApiResponse(responseCode = "200", description = "下架成功")
    public Result<Boolean> disableProduct(
            @Parameter(description = "商品ID", required = true)
            @PathVariable
            @NotNull(message = "商品ID不能为空")
            @Positive(message = "商品ID必须为正整数") Long id) {

        log.info("下架商品: {}", id);
        Boolean success = productService.disableProduct(id);
        return Result.success(success);
    }

    @PutMapping("/batch/enable")
    @RequireUserType({UserType.MERCHANT, UserType.ADMIN})
    @RequireScope("product:write")
    @Operation(summary = "批量上架商品", description = "批量将商品设为上架状态")
    @ApiResponse(responseCode = "200", description = "批量上架成功")
    public Result<Boolean> batchEnableProducts(
            @Parameter(description = "商品ID列表", required = true)
            @RequestBody @NotEmpty(message = "ID列表不能为空") List<Long> ids) {

        log.info("批量上架商品: {}", ids);

        if (ids.size() > 100) {
            return Result.error();
        }

        Boolean success = productService.batchEnableProducts(ids);
        return Result.success(success);
    }

    @PutMapping("/batch/disable")
    @RequireUserType({UserType.MERCHANT, UserType.ADMIN})
    @RequireScope("product:write")
    @Operation(summary = "批量下架商品", description = "批量将商品设为下架状态")
    @ApiResponse(responseCode = "200", description = "批量下架成功")
    public Result<Boolean> batchDisableProducts(
            @Parameter(description = "商品ID列表", required = true)
            @RequestBody @NotEmpty(message = "ID列表不能为空") List<Long> ids) {

        log.info("批量下架商品: {}", ids);

        if (ids.size() > 100) {
            return Result.error();
        }

        Boolean success = productService.batchDisableProducts(ids);
        return Result.success(success);
    }

    // ================= 库存管理 =================

    @PutMapping("/{id}/stock")
    @RequireUserType({UserType.MERCHANT, UserType.ADMIN})
    @RequireScope("stock:manage")
    @Operation(summary = "更新商品库存", description = "设置商品库存数量")
    @ApiResponse(responseCode = "200", description = "更新成功")
    public Result<Boolean> updateStock(
            @Parameter(description = "商品ID", required = true)
            @PathVariable
            @NotNull(message = "商品ID不能为空")
            @Positive(message = "商品ID必须为正整数") Long id,

            @Parameter(description = "库存数量", required = true)
            @RequestParam("stock")
            @NotNull(message = "库存数量不能为空") Integer stock) {

        log.info("更新商品库存: ID={}, Stock={}", id, stock);
        Boolean success = productService.updateStock(id, stock);
        return Result.success(success);
    }

    @PutMapping("/{id}/stock/increase")
    @RequireUserType({UserType.MERCHANT, UserType.ADMIN})
    @RequireScope("stock:manage")
    @Operation(summary = "增加商品库存", description = "增加指定数量的库存")
    @ApiResponse(responseCode = "200", description = "增加成功")
    public Result<Boolean> increaseStock(
            @Parameter(description = "商品ID", required = true)
            @PathVariable
            @NotNull(message = "商品ID不能为空")
            @Positive(message = "商品ID必须为正整数") Long id,

            @Parameter(description = "增加数量", required = true)
            @RequestParam("quantity")
            @NotNull(message = "增加数量不能为空")
            @Positive(message = "增加数量必须为正整数") Integer quantity) {

        log.info("增加商品库存: ID={}, Quantity={}", id, quantity);
        Boolean success = productService.increaseStock(id, quantity);
        return Result.success(success);
    }

    @PutMapping("/{id}/stock/decrease")
    @RequireUserType({UserType.MERCHANT, UserType.ADMIN})
    @RequireScope("stock:manage")
    @Operation(summary = "减少商品库存", description = "减少指定数量的库存")
    @ApiResponse(responseCode = "200", description = "减少成功")
    public Result<Boolean> decreaseStock(
            @Parameter(description = "商品ID", required = true)
            @PathVariable
            @NotNull(message = "商品ID不能为空")
            @Positive(message = "商品ID必须为正整数") Long id,

            @Parameter(description = "减少数量", required = true)
            @RequestParam("quantity")
            @NotNull(message = "减少数量不能为空")
            @Positive(message = "减少数量必须为正整数") Integer quantity) {

        log.info("减少商品库存: ID={}, Quantity={}", id, quantity);
        Boolean success = productService.decreaseStock(id, quantity);
        return Result.success(success);
    }

    @GetMapping("/{id}/stock/check")
    @RequireScope("product:read")
    @Operation(summary = "检查商品库存", description = "检查商品库存是否充足")
    @ApiResponse(responseCode = "200", description = "检查成功")
    public Result<Boolean> checkStock(
            @Parameter(description = "商品ID", required = true)
            @PathVariable
            @NotNull(message = "商品ID不能为空")
            @Positive(message = "商品ID必须为正整数") Long id,

            @Parameter(description = "需要检查的数量", required = true)
            @RequestParam("quantity")
            @NotNull(message = "检查数量不能为空")
            @Positive(message = "检查数量必须为正整数") Integer quantity) {

        log.debug("检查商品库存: ID={}, Quantity={}", id, quantity);
        Boolean sufficient = productService.checkStock(id, quantity);
        return Result.success(sufficient);
    }

    // ================= 缓存管理 =================

    @DeleteMapping("/cache/{id}")
    @RequireUserType(UserType.ADMIN)
    @RequireScope("admin:write")
    @Operation(summary = "清除商品缓存", description = "清除指定商品的缓存")
    @ApiResponse(responseCode = "200", description = "清除成功")
    public Result<String> evictProductCache(
            @Parameter(description = "商品ID", required = true)
            @PathVariable
            @NotNull(message = "商品ID不能为空")
            @Positive(message = "商品ID必须为正整数") Long id) {

        log.info("清除商品缓存: {}", id);
        productService.evictProductCache(id);
        return Result.success("缓存清除成功");
    }

    @DeleteMapping("/cache/all")
    @RequireUserType(UserType.ADMIN)
    @RequireScope("admin:write")
    @Operation(summary = "清除所有商品缓存", description = "清除所有商品相关缓存")
    @ApiResponse(responseCode = "200", description = "清除成功")
    public Result<String> evictAllProductCache() {
        log.info("清除所有商品缓存");
        productService.evictAllProductCache();
        return Result.success("所有缓存清除成功");
    }
}
