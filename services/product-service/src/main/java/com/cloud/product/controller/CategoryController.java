package com.cloud.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.domain.dto.product.CategoryDTO;
import com.cloud.common.result.PageResult;
import com.cloud.common.result.Result;
import com.cloud.product.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
@Tag(name = "Category API", description = "Category management APIs")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Get categories", description = "Get category page")
    public Result<PageResult<CategoryDTO>> getCategories(
            @Parameter(description = "Page index") @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "Page index must be >= 1") Integer page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "Page size must be >= 1")
            @Max(value = 100, message = "Page size must be <= 100") Integer size,
            @Parameter(description = "Parent id") @RequestParam(required = false) Long parentId,
            @Parameter(description = "Category level") @RequestParam(required = false) Integer level) {

        Page<CategoryDTO> pageResult = categoryService.getCategoriesPage(page, size, parentId, level);
        PageResult<CategoryDTO> result = PageResult.of(
                pageResult.getCurrent(),
                pageResult.getSize(),
                pageResult.getTotal(),
                pageResult.getRecords()
        );
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category", description = "Get category by id")
    public Result<CategoryDTO> getCategoryById(
            @Parameter(description = "Category id") @PathVariable
            @NotNull(message = "Category id cannot be null")
            @Positive(message = "Category id must be positive") Long id) {

        CategoryDTO category = categoryService.getCategoryById(id);
        if (category == null) {
            return Result.error("Category not found");
        }
        return Result.success("Query success", category);
    }

    @GetMapping("/tree")
    @Operation(summary = "Get category tree", description = "Get category tree data")
    public Result<List<CategoryDTO>> getCategoryTree(
            @Parameter(description = "Only enabled") @RequestParam(defaultValue = "false") Boolean enabledOnly) {

        List<CategoryDTO> tree = categoryService.getCategoryTree(enabledOnly);
        return Result.success("Query success", tree);
    }

    @GetMapping("/{id}/children")
    @Operation(summary = "Get children", description = "Get child categories")
    public Result<List<CategoryDTO>> getChildrenCategories(
            @Parameter(description = "Parent id") @PathVariable Long id,
            @Parameter(description = "Only enabled") @RequestParam(defaultValue = "false") Boolean enabledOnly) {

        List<CategoryDTO> children = categoryService.getChildrenCategories(id, enabledOnly);
        return Result.success("Query success", children);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "Create category", description = "Create a category")
    public Result<CategoryDTO> createCategory(
            @Parameter(description = "Category payload") @RequestBody
            @Valid @NotNull(message = "Category payload cannot be null") CategoryDTO categoryDTO) {

        CategoryDTO created = categoryService.createCategory(categoryDTO);
        return Result.success("Create success", created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "Update category", description = "Update category by id")
    public Result<Boolean> updateCategory(
            @Parameter(description = "Category id") @PathVariable Long id,
            @Parameter(description = "Category payload") @RequestBody
            @Valid @NotNull(message = "Category payload cannot be null") CategoryDTO categoryDTO) {

        categoryDTO.setId(id);
        boolean updated = Boolean.TRUE.equals(categoryService.updateCategory(categoryDTO));
        return Result.success("Update success", updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "Delete category", description = "Delete category by id")
    public Result<Boolean> deleteCategory(
            @Parameter(description = "Category id") @PathVariable
            @NotNull(message = "Category id cannot be null") Long id,
            @Parameter(description = "Cascade delete") @RequestParam(defaultValue = "false") Boolean cascade) {

        boolean deleted = Boolean.TRUE.equals(categoryService.deleteCategory(id, cascade));
        return Result.success("Delete success", deleted);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "Update status", description = "Update category status")
    public Result<Boolean> updateCategoryStatus(
            @Parameter(description = "Category id") @PathVariable Long id,
            @Parameter(description = "Status") @RequestParam Integer status) {

        boolean updated = Boolean.TRUE.equals(categoryService.updateCategoryStatus(id, status));
        return Result.success("Update status success", updated);
    }

    @PatchMapping("/{id}/sort")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "Update sort", description = "Update category sort order")
    public Result<Boolean> updateCategorySort(
            @Parameter(description = "Category id") @PathVariable Long id,
            @Parameter(description = "Sort order") @RequestParam Integer sort) {

        boolean updated = Boolean.TRUE.equals(categoryService.updateCategorySort(id, sort));
        return Result.success("Update sort success", updated);
    }

    @PatchMapping("/{id}/move")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "Move category", description = "Move category to new parent")
    public Result<Boolean> moveCategory(
            @Parameter(description = "Category id") @PathVariable Long id,
            @Parameter(description = "New parent id") @RequestParam Long newParentId) {

        boolean moved = Boolean.TRUE.equals(categoryService.moveCategory(id, newParentId));
        return Result.success("Move success", moved);
    }

    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "Batch delete", description = "Delete categories by ids")
    public Result<Boolean> deleteCategoriesBatch(
            @Parameter(description = "Category ids") @RequestBody
            @NotNull(message = "Category ids cannot be null")
            @NotEmpty(message = "Category ids cannot be empty") List<Long> ids) {

        boolean deleted = Boolean.TRUE.equals(categoryService.deleteCategoriesBatch(ids));
        return Result.success("Batch delete success", deleted);
    }

    @PatchMapping("/batch/status")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "Batch update status", description = "Update status for categories")
    public Result<Integer> updateCategoryStatusBatch(
            @Parameter(description = "Category ids") @RequestParam
            @NotNull(message = "Category ids cannot be null") List<Long> ids,
            @Parameter(description = "Status") @RequestParam
            @NotNull(message = "Status cannot be null") Integer status) {

        if (ids.isEmpty()) {
            return Result.badRequest("Category ids cannot be empty");
        }
        if (ids.size() > 100) {
            return Result.badRequest("Batch size cannot exceed 100");
        }

        int successCount = 0;
        for (Long id : ids) {
            try {
                if (Boolean.TRUE.equals(categoryService.updateCategoryStatus(id, status))) {
                    successCount++;
                }
            } catch (Exception e) {
                log.warn("Batch update category status failed: id={}", id, e);
            }
        }
        return Result.success(String.format("Batch update status done: %d/%d", successCount, ids.size()), successCount);
    }

    @PostMapping("/batch")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "Batch create", description = "Create categories in batch")
    public Result<Integer> createCategoriesBatch(
            @Parameter(description = "Category payload list") @RequestBody
            @Valid @NotEmpty(message = "Category payload list cannot be empty") List<CategoryDTO> categoryList) {

        if (categoryList.size() > 100) {
            return Result.badRequest("Batch size cannot exceed 100");
        }

        int successCount = 0;
        for (CategoryDTO categoryDTO : categoryList) {
            try {
                categoryService.createCategory(categoryDTO);
                successCount++;
            } catch (Exception e) {
                log.warn("Batch create category failed: name={}", categoryDTO.getName(), e);
            }
        }

        return Result.success(String.format("Batch create done: %d/%d", successCount, categoryList.size()), successCount);
    }
}
