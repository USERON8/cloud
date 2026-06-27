package com.cloud.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.domain.dto.product.CategoryDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
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
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
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

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Validated
@Tag(name = "分类接口", description = "商品分类管理接口")
public class CategoryController {

  private final CategoryService categoryService;

  @GetMapping
  @Operation(summary = "分页查询分类", description = "获取分类分页数据")
  public Result<PageResult<CategoryDTO>> getCategories(
      @Parameter(description = "页码")
          @RequestParam(defaultValue = "1")
          @Min(value = 1, message = "Page index must be >= 1")
          Integer page,
      @Parameter(description = "每页数量")
          @RequestParam(defaultValue = "10")
          @Min(value = 1, message = "Page size must be >= 1")
          @Max(value = 100, message = "Page size must be <= 100")
          Integer size,
      @Parameter(description = "父分类 ID") @RequestParam(required = false) Long parentId,
      @Parameter(description = "分类层级") @RequestParam(required = false) Integer level) {

    Page<CategoryDTO> pageResult = categoryService.getCategoriesPage(page, size, parentId, level);
    PageResult<CategoryDTO> result =
        PageResult.of(
            pageResult.getCurrent(),
            pageResult.getSize(),
            pageResult.getTotal(),
            pageResult.getRecords());
    return Result.success(result);
  }

  @GetMapping("/{id}")
  @Operation(summary = "查询分类详情", description = "按 ID 查询分类")
  public Result<CategoryDTO> getCategoryById(
      @Parameter(description = "分类 ID")
          @PathVariable
          @NotNull(message = "Category id cannot be null")
          @Positive(message = "Category id must be positive")
          Long id) {

    CategoryDTO category = categoryService.getCategoryById(id);
    if (category == null) {
      throw new BizException(ResultCode.CATEGORY_NOT_FOUND, "Category not found");
    }
    return Result.success("Query success", category);
  }

  @GetMapping("/tree")
  @Operation(summary = "查询分类树", description = "获取分类树数据")
  public Result<List<CategoryDTO>> getCategoryTree(
      @Parameter(description = "仅启用") @RequestParam(defaultValue = "false")
          Boolean enabledOnly) {

    List<CategoryDTO> tree = categoryService.getCategoryTree(enabledOnly);
    return Result.success("Query success", tree);
  }

  @GetMapping("/{id}/children")
  @Operation(summary = "查询子分类", description = "获取子分类")
  public Result<List<CategoryDTO>> getChildrenCategories(
      @Parameter(description = "父分类 ID") @PathVariable Long id,
      @Parameter(description = "仅启用") @RequestParam(defaultValue = "false")
          Boolean enabledOnly) {

    List<CategoryDTO> children = categoryService.getChildrenCategories(id, enabledOnly);
    return Result.success("Query success", children);
  }

  @PostMapping
  @PreAuthorize("hasAuthority('product:create')")
  @Operation(summary = "创建分类", description = "创建一个分类")
  public Result<CategoryDTO> createCategory(
      @Parameter(description = "分类请求体")
          @RequestBody
          @Valid
          @NotNull(message = "Category payload cannot be null")
          CategoryDTO categoryDTO) {

    CategoryDTO created = categoryService.createCategory(categoryDTO);
    return Result.success("Create success", created);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('product:edit')")
  @Operation(summary = "更新分类", description = "按 ID 更新分类")
  public Result<Boolean> updateCategory(
      @Parameter(description = "分类 ID") @PathVariable Long id,
      @Parameter(description = "分类请求体")
          @RequestBody
          @Valid
          @NotNull(message = "Category payload cannot be null")
          CategoryDTO categoryDTO) {

    categoryDTO.setId(id);
    boolean updated = Boolean.TRUE.equals(categoryService.updateCategory(categoryDTO));
    return Result.success("Update success", updated);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('product:delete')")
  @Operation(summary = "删除分类", description = "按 ID 删除分类")
  public Result<Boolean> deleteCategory(
      @Parameter(description = "分类 ID")
          @PathVariable
          @NotNull(message = "Category id cannot be null")
          Long id,
      @Parameter(description = "级联删除") @RequestParam(defaultValue = "false")
          Boolean cascade) {

    boolean deleted = Boolean.TRUE.equals(categoryService.deleteCategory(id, cascade));
    return Result.success("Delete success", deleted);
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasAuthority('product:edit')")
  @Operation(summary = "更新状态", description = "更新分类状态")
  public Result<Boolean> updateCategoryStatus(
      @Parameter(description = "分类 ID") @PathVariable Long id,
      @Parameter(description = "状态") @RequestParam Integer status) {

    boolean updated = Boolean.TRUE.equals(categoryService.updateCategoryStatus(id, status));
    return Result.success("Update status success", updated);
  }

  @PatchMapping("/{id}/sort")
  @PreAuthorize("hasAuthority('product:edit')")
  @Operation(summary = "更新排序", description = "更新分类排序")
  public Result<Boolean> updateCategorySort(
      @Parameter(description = "分类 ID") @PathVariable Long id,
      @Parameter(description = "排序值") @RequestParam Integer sort) {

    boolean updated = Boolean.TRUE.equals(categoryService.updateCategorySort(id, sort));
    return Result.success("Update sort success", updated);
  }

  @PatchMapping("/{id}/move")
  @PreAuthorize("hasAuthority('product:edit')")
  @Operation(summary = "移动分类", description = "移动分类到新的父级")
  public Result<Boolean> moveCategory(
      @Parameter(description = "分类 ID") @PathVariable Long id,
      @Parameter(description = "新父分类 ID") @RequestParam Long newParentId) {

    boolean moved = Boolean.TRUE.equals(categoryService.moveCategory(id, newParentId));
    return Result.success("Move success", moved);
  }

  @DeleteMapping("/batch")
  @PreAuthorize("hasAuthority('product:delete')")
  @Operation(summary = "批量删除", description = "按 ID 批量删除分类")
  public Result<Boolean> deleteCategoriesBatch(
      @Parameter(description = "分类 ID 列表")
          @RequestBody
          @NotNull(message = "Category ids cannot be null")
          @NotEmpty(message = "Category ids cannot be empty")
          List<Long> ids) {

    boolean deleted = Boolean.TRUE.equals(categoryService.deleteCategoriesBatch(ids));
    return Result.success("Batch delete success", deleted);
  }

  @PatchMapping("/batch/status")
  @PreAuthorize("hasAuthority('product:edit')")
  @Operation(summary = "批量更新状态", description = "批量更新分类状态")
  public Result<Integer> updateCategoryStatusBatch(
      @Parameter(description = "分类 ID 列表")
          @RequestParam
          @NotNull(message = "Category ids cannot be null")
          List<Long> ids,
      @Parameter(description = "状态") @RequestParam @NotNull(message = "Status cannot be null")
          Integer status) {

    if (ids.isEmpty()) {
      throw new BizException(ResultCode.BAD_REQUEST, "Category ids cannot be empty");
    }
    if (ids.size() > 100) {
      throw new BizException(ResultCode.BAD_REQUEST, "Batch size cannot exceed 100");
    }

    int successCount = categoryService.updateCategoryStatusBatch(ids, status);
    return Result.success(
        String.format("Batch update status done: %d/%d", successCount, ids.size()), successCount);
  }

  @PostMapping("/batch")
  @PreAuthorize("hasAuthority('product:create')")
  @Operation(summary = "批量创建", description = "批量创建分类")
  public Result<Integer> createCategoriesBatch(
      @Parameter(description = "分类请求体列表")
          @RequestBody
          @Valid
          @NotEmpty(message = "Category payload list cannot be empty")
          List<CategoryDTO> categoryList) {

    if (categoryList.size() > 100) {
      throw new BizException(ResultCode.BAD_REQUEST, "Batch size cannot exceed 100");
    }

    int successCount = categoryService.createCategoriesBatch(categoryList);
    return Result.success(
        String.format("Batch create done: %d/%d", successCount, categoryList.size()), successCount);
  }
}
