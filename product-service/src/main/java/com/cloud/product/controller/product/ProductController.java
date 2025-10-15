package com.cloud.product.controller.product;

import com.cloud.common.domain.dto.product.ProductRequestDTO;
import com.cloud.common.domain.vo.product.ProductVO;
import com.cloud.common.exception.ResourceNotFoundException;
import com.cloud.common.exception.ValidationException;
import com.cloud.common.result.PageResult;
import com.cloud.common.result.Result;
import com.cloud.common.validation.BatchValidationUtils;
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
@RequestMapping("/api/product")
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
            log.warn("商品不存在，商品ID: {}", id);
            throw new ResourceNotFoundException("Product", String.valueOf(id));
        }
        log.info("查询商品成功，商品ID: {}", id);
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

        Long productId = productService.createProduct(requestDTO);
        log.info("商品创建成功，商品ID: {}, 商品名称: {}", productId, requestDTO.getName());
        return Result.success("商品创建成功", productId);
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

        boolean result = productService.updateProduct(id, requestDTO);
        log.info("商品更新成功 - 商品ID: {}, 操作人: {}", id, authentication.getName());
        return Result.success("商品更新成功", result);
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

        boolean result = productService.updateProduct(id, requestDTO);
        log.info("商品部分更新成功 - 商品ID: {}, 操作人: {}", id, authentication.getName());
        return Result.success("商品更新成功", result);
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

        boolean result = productService.deleteProduct(id);
        log.info("商品删除成功 - 商品ID: {}", id);
        return Result.success("商品删除成功", result);
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

        BatchValidationUtils.validateIdList(ids, "批量查询商品");
        List<ProductVO> products = productService.getProductsByIds(ids);
        log.info("批量查询商品成功 - 数量: {}", products.size());
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

        ProductVO productProfile = productService.getProductById(id);
        log.info("查询商品档案成功 - 商品ID: {}", id);
        return Result.success("查询成功", productProfile);
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

        boolean result = productService.updateProduct(id, profileDTO);
        log.info("商品档案更新成功 - 商品ID: {}, 操作人: {}", id, authentication.getName());
        return Result.success("商品档案更新成功", result);
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

        boolean result;
        if (status == 1) {
            result = productService.enableProduct(id);
            log.info("商品上架成功 - 商品ID: {}", id);
        } else {
            result = productService.disableProduct(id);
            log.info("商品下架成功 - 商品ID: {}", id);
        }
        return Result.success("商品状态更新成功", result);
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

        log.info("查询分类商品 - 分类ID: {}, 状态: {}", categoryId, status);
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

        log.info("查询品牌商品 - 品牌ID: {}, 状态: {}", brandId, status);
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

        BatchValidationUtils.validateIdList(ids, "批量删除商品");
        log.info("开始批量删除商品 - 数量: {}", ids.size());
        Boolean success = productService.batchDeleteProducts(ids);
        log.info("批量删除商品完成 - 成功: {}/{}", ids.size(), ids.size());
        return Result.success("批量删除商品成功", success);
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

        if (ids.size() > 100) {
            return Result.error("批量操作数量不能超过100个");
        }

        log.info("开始批量上架商品 - 数量: {}", ids.size());
        Boolean success = productService.batchEnableProducts(ids);
        log.info("批量上架商品完成 - 成功: {}/{}", ids.size(), ids.size());
        return Result.success("批量上架商品成功", success);
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

        if (ids.size() > 100) {
            return Result.error("批量操作数量不能超过100个");
        }

        log.info("开始批量下架商品 - 数量: {}", ids.size());
        Boolean success = productService.batchDisableProducts(ids);
        log.info("批量下架商品完成 - 成功: {}/{}", ids.size(), ids.size());
        return Result.success("批量下架商品成功", success);
    }

    /**
     * 批量创建商品
     */
    @PostMapping("/batch")
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN') and hasAuthority('SCOPE_product:create')")
    @Operation(summary = "批量创建商品", description = "批量创建多个商品")
    public Result<Integer> batchCreateProducts(
            @Parameter(description = "商品信息列表", required = true) @RequestBody
            @Valid @NotEmpty(message = "商品信息列表不能为空") List<ProductRequestDTO> productList) {

        if (productList.size() > 100) {
            return Result.error("批量创建数量不能超过100个");
        }

        log.info("开始批量创建商品 - 数量: {}", productList.size());
        int successCount = 0;
        for (ProductRequestDTO requestDTO : productList) {
            try {
                productService.createProduct(requestDTO);
                successCount++;
            } catch (Exception e) {
                log.error("创建商品失败 - 商品名称: {}", requestDTO.getName(), e);
            }
        }

        log.info("批量创建商品完成 - 成功: {}/{}", successCount, productList.size());
        return Result.success(String.format("批量创建商品成功: %d/%d", successCount, productList.size()), successCount);
    }

    /**
     * 批量更新商品
     */
    @PutMapping("/batch")
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN') and hasAuthority('SCOPE_product:write')")
    @Operation(summary = "批量更新商品", description = "批量更新多个商品信息")
    public Result<Integer> batchUpdateProducts(
            @Parameter(description = "商品更新信息列表", required = true) @RequestBody
            @Valid @NotEmpty(message = "商品信息列表不能为空") List<ProductUpdateRequest> productList) {

        if (productList.size() > 100) {
            return Result.error("批量更新数量不能超过100个");
        }

        log.info("开始批量更新商品 - 数量: {}", productList.size());
        int successCount = 0;
        for (ProductUpdateRequest request : productList) {
            try {
                if (request.getId() != null && request.getRequestDTO() != null) {
                    productService.updateProduct(request.getId(), request.getRequestDTO());
                    successCount++;
                }
            } catch (Exception e) {
                log.error("更新商品失败 - 商品ID: {}", request.getId(), e);
            }
        }

        log.info("批量更新商品完成 - 成功: {}/{}", successCount, productList.size());
        return Result.success(String.format("批量更新商品成功: %d/%d", successCount, productList.size()), successCount);
    }

    /**
     * 商品批量更新请求
     */
    public static class ProductUpdateRequest {
        private Long id;
        private ProductRequestDTO requestDTO;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public ProductRequestDTO getRequestDTO() {
            return requestDTO;
        }

        public void setRequestDTO(ProductRequestDTO requestDTO) {
            this.requestDTO = requestDTO;
        }
    }
}
