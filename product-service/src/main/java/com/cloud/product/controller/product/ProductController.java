package com.cloud.product.controller.product;

import com.cloud.common.domain.dto.product.ProductRequestDTO;
import com.cloud.common.domain.vo.product.ProductVO;
import com.cloud.common.result.PageResult;
import com.cloud.common.result.Result;
import com.cloud.product.module.dto.ProductPageDTO;
import com.cloud.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品RESTful API控制器
 * 提供商品资源的CRUD操作，参考User服务标准架构
 *
 * @author what's up
 */
@Slf4j
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Tag(name = "商品服务", description = "商品资源的RESTful API接口")
public class ProductController {

    private final ProductService productService;

    /**
     * 获取商品列表（支持查询参数）
     */
    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_product:read')")
    @Operation(summary = "获取商品列表", description = "获取商品列表，支持分页和查询参数")
    public Result<PageResult<ProductVO>> getProducts(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "商品名称") @RequestParam(required = false) String name,
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "品牌ID") @RequestParam(required = false) Long brandId,
            @Parameter(description = "商品状态") @RequestParam(required = false) Integer status) {

        ProductPageDTO productPageDTO = new ProductPageDTO();
        productPageDTO.setCurrent(page.longValue());
        productPageDTO.setSize(size.longValue());
        productPageDTO.setName(name);
        productPageDTO.setCategoryId(categoryId);
        productPageDTO.setBrandId(brandId);
        productPageDTO.setStatus(status);

        return Result.success(productService.getProductsPage(productPageDTO));
    }

    /**
     * 根据商品名称查询商品
     */
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('SCOPE_product:read')")
    @Operation(summary = "根据商品名称查询商品", description = "根据商品名称查询商品信息")
    public Result<List<ProductVO>> findByName(
            @Parameter(description = "商品名称") @RequestParam
            @NotNull(message = "商品名称不能为空") String name) {
        List<ProductVO> products = productService.searchProductsByName(name, null);
        return Result.success("查询成功", products);
    }

    /**
     * 根据ID获取商品详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_product:read')")
    @Operation(summary = "获取商品详情", description = "根据商品ID获取商品详细信息")
    public Result<ProductVO> getProductById(
            @Parameter(description = "商品ID") @PathVariable
            @Positive(message = "商品ID必须为正整数") Long id) {

        ProductVO product = productService.getProductById(id);
        if (product == null) {
            return Result.error("商品不存在");
        }

        return Result.success("查询成功", product);
    }

    /**
     * 创建商品
     */
    @PostMapping
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN') and hasAuthority('SCOPE_product:create')")
    @Operation(summary = "创建商品", description = "创建新商品")
    public Result<Long> createProduct(
            @Parameter(description = "商品信息") @RequestBody
            @Valid @NotNull(message = "商品信息不能为空") ProductRequestDTO requestDTO) {

        try {
            Long productId = productService.createProduct(requestDTO);
            return Result.success("商品创建成功", productId);
        } catch (Exception e) {
            log.error("创建商品失败", e);
            return Result.error("创建商品失败: " + e.getMessage());
        }
    }

    /**
     * 更新商品信息
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN') and hasAuthority('SCOPE_product:write')")
    @Operation(summary = "更新商品信息", description = "更新商品信息")
    public Result<Boolean> updateProduct(
            @Parameter(description = "商品ID") @PathVariable Long id,
            @Parameter(description = "商品信息") @RequestBody
            @Valid @NotNull(message = "商品信息不能为空") ProductRequestDTO requestDTO,
            Authentication authentication) {

        try {
            boolean result = productService.updateProduct(id, requestDTO);
            return Result.success("商品更新成功", result);
        } catch (Exception e) {
            log.error("更新商品信息失败，商品ID: {}", id, e);
            return Result.error("更新商品信息失败: " + e.getMessage());
        }
    }

    /**
     * 部分更新商品信息
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN') and hasAuthority('SCOPE_product:write')")
    @Operation(summary = "部分更新商品信息", description = "部分更新商品信息")
    public Result<Boolean> patchProduct(
            @Parameter(description = "商品ID") @PathVariable Long id,
            @Parameter(description = "商品信息") @RequestBody ProductRequestDTO requestDTO,
            Authentication authentication) {

        try {
            boolean result = productService.updateProduct(id, requestDTO);
            return Result.success("商品更新成功", result);
        } catch (Exception e) {
            log.error("部分更新商品信息失败，商品ID: {}", id, e);
            return Result.error("更新商品信息失败: " + e.getMessage());
        }
    }

    /**
     * 删除商品
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN') and hasAuthority('SCOPE_product:write')")
    @Operation(summary = "删除商品", description = "删除商品")
    public Result<Boolean> deleteProduct(
            @Parameter(description = "商品ID") @PathVariable
            @Positive(message = "商品ID必须为正整数") Long id) {

        try {
            boolean result = productService.deleteProduct(id);
            return Result.success("商品删除成功", result);
        } catch (Exception e) {
            log.error("删除商品失败，商品ID: {}", id, e);
            return Result.error("删除商品失败: " + e.getMessage());
        }
    }

    /**
     * 批量获取商品
     */
    @GetMapping("/batch")
    @PreAuthorize("hasAuthority('SCOPE_product:read')")
    @Operation(summary = "批量获取商品", description = "根据ID列表批量获取商品信息")
    public Result<List<ProductVO>> getProductsByIds(
            @Parameter(description = "商品ID列表") @RequestParam
            @NotNull(message = "商品ID列表不能为空") List<Long> ids) {

        if (ids.size() > 100) {
            return Result.error("ID列表数量不能超过100个");
        }

        List<ProductVO> products = productService.getProductsByIds(ids);
        return Result.success("查询成功", products);
    }

    /**
     * 获取商品档案
     */
    @GetMapping("/{id}/profile")
    @PreAuthorize("hasAuthority('SCOPE_product:read')")
    @Operation(summary = "获取商品档案", description = "获取商品详细档案信息")
    public Result<ProductVO> getProductProfile(
            @Parameter(description = "商品ID") @PathVariable Long id,
            Authentication authentication) {

        try {
            ProductVO productProfile = productService.getProductById(id);
            return Result.success("查询成功", productProfile);
        } catch (Exception e) {
            log.error("获取商品档案失败，商品ID: {}", id, e);
            return Result.error("获取商品档案失败: " + e.getMessage());
        }
    }

    /**
     * 更新商品档案
     */
    @PutMapping("/{id}/profile")
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN') and hasAuthority('SCOPE_product:write')")
    @Operation(summary = "更新商品档案", description = "更新商品详细档案信息")
    public Result<Boolean> updateProductProfile(
            @Parameter(description = "商品ID") @PathVariable Long id,
            @Parameter(description = "商品档案信息") @RequestBody
            @Valid @NotNull(message = "商品档案信息不能为空") ProductRequestDTO profileDTO,
            Authentication authentication) {

        try {
            boolean result = productService.updateProduct(id, profileDTO);
            return Result.success("商品档案更新成功", result);
        } catch (Exception e) {
            log.error("更新商品档案失败，商品ID: {}", id, e);
            return Result.error("更新商品档案失败: " + e.getMessage());
        }
    }

    /**
     * 更新商品状态
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN') and hasAuthority('SCOPE_product:write')")
    @Operation(summary = "更新商品状态", description = "启用或禁用商品")
    public Result<Boolean> updateProductStatus(
            @Parameter(description = "商品ID") @PathVariable Long id,
            @Parameter(description = "商品状态") @RequestParam Integer status) {

        try {
            boolean result;
            if (status == 1) {
                result = productService.enableProduct(id);
            } else {
                result = productService.disableProduct(id);
            }
            return Result.success("商品状态更新成功", result);
        } catch (Exception e) {
            log.error("更新商品状态失败，商品ID: {}, 状态: {}", id, status, e);
            return Result.error("更新商品状态失败: " + e.getMessage());
        }
    }

    /**
     * 根据分类查询商品
     */
    @GetMapping("/category/{categoryId}")
    @Operation(summary = "根据分类查询商品", description = "获取指定分类下的商品列表")
    @PreAuthorize("hasAuthority('SCOPE_product:read')")
    public Result<List<ProductVO>> getProductsByCategoryId(
            @Parameter(description = "分类ID", required = true) @PathVariable
            @NotNull(message = "分类ID不能为空")
            @Positive(message = "分类ID必须为正整数") Long categoryId,

            @Parameter(description = "商品状态：1-上架，0-下架") @RequestParam(value = "status", required = false) Integer status) {

        log.debug("根据分类查询商品: categoryId={}, status={}", categoryId, status);
        List<ProductVO> products = productService.getProductsByCategoryId(categoryId, status);
        return Result.success(products);
    }

    /**
     * 根据品牌查询商品
     */
    @GetMapping("/brand/{brandId}")
    @Operation(summary = "根据品牌查询商品", description = "获取指定品牌下的商品列表")
    @PreAuthorize("hasAuthority('SCOPE_product:read')")
    public Result<List<ProductVO>> getProductsByBrandId(
            @Parameter(description = "品牌ID", required = true) @PathVariable
            @NotNull(message = "品牌ID不能为空")
            @Positive(message = "品牌ID必须为正整数") Long brandId,

            @Parameter(description = "商品状态：1-上架，0-下架") @RequestParam(value = "status", required = false) Integer status) {

        log.debug("根据品牌查询商品: brandId={}, status={}", brandId, status);
        List<ProductVO> products = productService.getProductsByBrandId(brandId, status);
        return Result.success(products);
    }

    /**
     * 批量删除商品
     */
    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN') and hasAuthority('SCOPE_product:write')")
    @Operation(summary = "批量删除商品", description = "根据ID列表批量删除商品")
    public Result<Boolean> batchDeleteProducts(
            @Parameter(description = "商品ID列表", required = true) @RequestBody
            @NotEmpty(message = "ID列表不能为空") List<Long> ids) {

        log.info("批量删除商品: {}", ids);

        if (ids.size() > 100) {
            return Result.error("批量删除数量不能超过100个");
        }

        Boolean success = productService.batchDeleteProducts(ids);
        return Result.success(success);
    }

    /**
     * 批量上架商品
     */
    @PutMapping("/batch/enable")
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN') and hasAuthority('SCOPE_product:write')")
    @Operation(summary = "批量上架商品", description = "批量将商品设为上架状态")
    public Result<Boolean> batchEnableProducts(
            @Parameter(description = "商品ID列表", required = true) @RequestBody
            @NotEmpty(message = "ID列表不能为空") List<Long> ids) {

        log.info("批量上架商品: {}", ids);

        if (ids.size() > 100) {
            return Result.error("批量操作数量不能超过100个");
        }

        Boolean success = productService.batchEnableProducts(ids);
        return Result.success(success);
    }

    /**
     * 批量下架商品
     */
    @PutMapping("/batch/disable")
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN') and hasAuthority('SCOPE_product:write')")
    @Operation(summary = "批量下架商品", description = "批量将商品设为下架状态")
    public Result<Boolean> batchDisableProducts(
            @Parameter(description = "商品ID列表", required = true) @RequestBody
            @NotEmpty(message = "ID列表不能为空") List<Long> ids) {

        log.info("批量下架商品: {}", ids);

        if (ids.size() > 100) {
            return Result.error("批量操作数量不能超过100个");
        }

        Boolean success = productService.batchDisableProducts(ids);
        return Result.success(success);
    }
}
