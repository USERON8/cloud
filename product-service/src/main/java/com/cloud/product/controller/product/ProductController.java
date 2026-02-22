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
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
@Tag(name = "Product API", description = "Product management APIs")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_product:read')")
    @Operation(summary = "Get products", description = "Get products by page and filter")
    public Result<PageResult<ProductVO>> getProducts(
            @Parameter(description = "Page index") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "Name") @RequestParam(required = false) String name,
            @Parameter(description = "Category id") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Brand id") @RequestParam(required = false) Long brandId,
            @Parameter(description = "Status") @RequestParam(required = false) Integer status) {

        ProductPageDTO pageDTO = new ProductPageDTO();
        pageDTO.setCurrent(page.longValue());
        pageDTO.setSize(size.longValue());
        pageDTO.setName(name);
        pageDTO.setCategoryId(categoryId);
        pageDTO.setBrandId(brandId);
        pageDTO.setStatus(status);

        return Result.success(productService.getProductsPage(pageDTO));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('SCOPE_product:read')")
    @Operation(summary = "Search products", description = "Search products by name")
    public Result<List<ProductVO>> findByName(
            @Parameter(description = "Name") @RequestParam @NotNull String name) {

        return Result.success("Query success", productService.searchProductsByName(name, null));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_product:read')")
    @Operation(summary = "Get product", description = "Get product by id")
    public Result<ProductVO> getProductById(
            @Parameter(description = "Product id") @PathVariable @Positive Long id) {

        ProductVO product = productService.getProductById(id);
        if (product == null) {
            return Result.error("Product not found");
        }
        return Result.success("Query success", product);
    }

    @PostMapping
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN')")
    @Operation(summary = "Create product", description = "Create a product")
    public Result<Long> createProduct(
            @Parameter(description = "Product payload") @RequestBody
            @Valid @NotNull ProductRequestDTO requestDTO) {

        Long productId = productService.createProduct(requestDTO);
        return Result.success("Create success", productId);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN')")
    @Operation(summary = "Update product", description = "Update product by id")
    public Result<Boolean> updateProduct(
            @Parameter(description = "Product id") @PathVariable Long id,
            @Parameter(description = "Product payload") @RequestBody
            @Valid @NotNull ProductRequestDTO requestDTO) {

        return Result.success("Update success", Boolean.TRUE.equals(productService.updateProduct(id, requestDTO)));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN')")
    @Operation(summary = "Patch product", description = "Patch product by id")
    public Result<Boolean> patchProduct(
            @Parameter(description = "Product id") @PathVariable Long id,
            @Parameter(description = "Product payload") @RequestBody ProductRequestDTO requestDTO) {

        return Result.success("Patch success", Boolean.TRUE.equals(productService.updateProduct(id, requestDTO)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN')")
    @Operation(summary = "Delete product", description = "Delete product by id")
    public Result<Boolean> deleteProduct(
            @Parameter(description = "Product id") @PathVariable @Positive Long id) {

        return Result.success("Delete success", Boolean.TRUE.equals(productService.deleteProduct(id)));
    }

    @GetMapping("/batch")
    @PreAuthorize("hasAuthority('SCOPE_product:read')")
    @Operation(summary = "Get products by ids", description = "Batch query products")
    public Result<List<ProductVO>> getProductsByIds(@RequestParam List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        return Result.success(productService.getProductsByIds(ids));
    }

    @GetMapping("/{id}/profile")
    @PreAuthorize("hasAuthority('SCOPE_product:read')")
    @Operation(summary = "Get profile", description = "Get product profile")
    public Result<ProductVO> getProductProfile(@PathVariable Long id) {
        return getProductById(id);
    }

    @PutMapping("/{id}/profile")
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN')")
    @Operation(summary = "Update profile", description = "Update product profile")
    public Result<Boolean> updateProductProfile(
            @PathVariable Long id,
            @RequestBody @Valid @NotNull ProductRequestDTO profileDTO) {

        return updateProduct(id, profileDTO);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN')")
    @Operation(summary = "Update status", description = "Enable or disable product")
    public Result<Boolean> updateProductStatus(
            @PathVariable Long id,
            @RequestParam Integer status) {

        Boolean result = (status != null && status == 1)
                ? productService.enableProduct(id)
                : productService.disableProduct(id);
        return Result.success("Update status success", Boolean.TRUE.equals(result));
    }

    @GetMapping("/category/{categoryId}")
    @PreAuthorize("hasAuthority('SCOPE_product:read')")
    @Operation(summary = "Get by category", description = "Get products by category")
    public Result<List<ProductVO>> getProductsByCategoryId(
            @PathVariable @NotNull @Positive Long categoryId,
            @RequestParam(value = "status", required = false) Integer status) {

        return Result.success(productService.getProductsByCategoryId(categoryId, status));
    }

    @GetMapping("/brand/{brandId}")
    @PreAuthorize("hasAuthority('SCOPE_product:read')")
    @Operation(summary = "Get by brand", description = "Get products by brand")
    public Result<List<ProductVO>> getProductsByBrandId(
            @PathVariable @NotNull @Positive Long brandId,
            @RequestParam(value = "status", required = false) Integer status) {

        return Result.success(productService.getProductsByBrandId(brandId, status));
    }

    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN')")
    @Operation(summary = "Batch delete", description = "Batch delete products")
    public Result<Boolean> batchDeleteProducts(
            @RequestBody @NotEmpty List<Long> ids) {

        return Result.success("Batch delete success", Boolean.TRUE.equals(productService.batchDeleteProducts(ids)));
    }

    @PutMapping("/batch/enable")
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN')")
    @Operation(summary = "Batch enable", description = "Batch enable products")
    public Result<Boolean> batchEnableProducts(@RequestBody @NotEmpty List<Long> ids) {
        if (ids.size() > 100) {
            return Result.error("Batch size cannot exceed 100");
        }
        return Result.success("Batch enable success", Boolean.TRUE.equals(productService.batchEnableProducts(ids)));
    }

    @PutMapping("/batch/disable")
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN')")
    @Operation(summary = "Batch disable", description = "Batch disable products")
    public Result<Boolean> batchDisableProducts(@RequestBody @NotEmpty List<Long> ids) {
        if (ids.size() > 100) {
            return Result.error("Batch size cannot exceed 100");
        }
        return Result.success("Batch disable success", Boolean.TRUE.equals(productService.batchDisableProducts(ids)));
    }

    @PostMapping("/batch")
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN')")
    @Operation(summary = "Batch create", description = "Batch create products")
    public Result<Integer> batchCreateProducts(
            @RequestBody @Valid @NotEmpty List<ProductRequestDTO> productList) {

        if (productList.size() > 100) {
            return Result.error("Batch size cannot exceed 100");
        }

        int successCount = 0;
        for (ProductRequestDTO requestDTO : productList) {
            try {
                if (productService.createProduct(requestDTO) != null) {
                    successCount++;
                }
            } catch (Exception e) {
                log.warn("Batch create product failed: name={}", requestDTO.getName(), e);
            }
        }

        return Result.success(String.format("Batch create done: %d/%d", successCount, productList.size()), successCount);
    }

    @PutMapping("/batch")
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN')")
    @Operation(summary = "Batch update", description = "Batch update products")
    public Result<Integer> batchUpdateProducts(
            @RequestBody @Valid @NotEmpty List<ProductUpdateRequest> productList) {

        if (productList.size() > 100) {
            return Result.error("Batch size cannot exceed 100");
        }

        int successCount = 0;
        for (ProductUpdateRequest request : productList) {
            try {
                if (request.getId() != null && request.getRequestDTO() != null
                        && Boolean.TRUE.equals(productService.updateProduct(request.getId(), request.getRequestDTO()))) {
                    successCount++;
                }
            } catch (Exception e) {
                log.warn("Batch update product failed: id={}", request.getId(), e);
            }
        }

        return Result.success(String.format("Batch update done: %d/%d", successCount, productList.size()), successCount);
    }

    @Data
    public static class ProductUpdateRequest {
        private Long id;
        private ProductRequestDTO requestDTO;
    }
}
