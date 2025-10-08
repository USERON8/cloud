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
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品分类RESTful API控制器
 * 提供商品分类资源的CRUD操作
 *
 * @author what's up
 */
@Slf4j
@RestController
@RequestMapping("/category")
@RequiredArgsConstructor
@Tag(name = "商品分类服务", description = "商品分类资源的RESTful API接口")
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 分页查询商品分类
     */
    @GetMapping
    @Operation(summary = "分页查询商品分类", description = "获取商品分类列表，支持分页")
    public Result<PageResult<CategoryDTO>> getCategories(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "页码必须大于0") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "每页数量必须大于0")
            @Max(value = 100, message = "每页数量不能超过100") Integer size,
            @Parameter(description = "父分类ID") @RequestParam(required = false) Long parentId,
            @Parameter(description = "分类状态") @RequestParam(required = false) Integer status) {

        try {
            Page<CategoryDTO> pageResult = categoryService.getCategoriesPage(page, size, parentId, status);
            PageResult<CategoryDTO> result = PageResult.of(
                    pageResult.getCurrent(),
                    pageResult.getSize(),
                    pageResult.getTotal(),
                    pageResult.getRecords()
            );
            return Result.success(result);
        } catch (Exception e) {
            log.error("分页查询商品分类失败", e);
            return Result.error("分页查询商品分类失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取商品分类详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取商品分类详情", description = "根据分类ID获取详细信息")
    public Result<CategoryDTO> getCategoryById(
            @Parameter(description = "分类ID") @PathVariable
            @NotNull(message = "分类ID不能为空")
            @Positive(message = "分类ID必须为正整数") Long id) {

        try {
            CategoryDTO category = categoryService.getCategoryById(id);
            if (category == null) {
                return Result.error("商品分类不存在");
            }
            return Result.success("查询成功", category);
        } catch (Exception e) {
            log.error("获取商品分类详情失败，分类ID: {}", id, e);
            return Result.error("获取商品分类详情失败: " + e.getMessage());
        }
    }

    /**
     * 获取树形分类结构
     */
    @GetMapping("/tree")
    @Operation(summary = "获取树形分类结构", description = "获取完整的树形商品分类结构")
    public Result<List<CategoryDTO>> getCategoryTree(
            @Parameter(description = "是否只返回启用的分类") @RequestParam(defaultValue = "false") Boolean enabledOnly) {

        try {
            List<CategoryDTO> tree = categoryService.getCategoryTree(enabledOnly);
            return Result.success("查询成功", tree);
        } catch (Exception e) {
            log.error("获取树形分类结构失败", e);
            return Result.error("获取树形分类结构失败: " + e.getMessage());
        }
    }

    /**
     * 获取子分类列表
     */
    @GetMapping("/{id}/children")
    @Operation(summary = "获取子分类列表", description = "获取指定分类下的所有子分类")
    public Result<List<CategoryDTO>> getChildrenCategories(
            @Parameter(description = "父分类ID") @PathVariable Long id,
            @Parameter(description = "是否递归获取") @RequestParam(defaultValue = "false") Boolean recursive) {

        try {
            List<CategoryDTO> children = categoryService.getChildrenCategories(id, recursive);
            return Result.success("查询成功", children);
        } catch (Exception e) {
            log.error("获取子分类列表失败，父分类ID: {}", id, e);
            return Result.error("获取子分类列表失败: " + e.getMessage());
        }
    }

    /**
     * 创建商品分类
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "创建商品分类", description = "创建新的商品分类")
    public Result<CategoryDTO> createCategory(
            @Parameter(description = "分类信息") @RequestBody
            @Valid @NotNull(message = "分类信息不能为空") CategoryDTO categoryDTO) {

        try {
            CategoryDTO created = categoryService.createCategory(categoryDTO);
            return Result.success("商品分类创建成功", created);
        } catch (Exception e) {
            log.error("创建商品分类失败", e);
            return Result.error("创建商品分类失败: " + e.getMessage());
        }
    }

    /**
     * 更新商品分类
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "更新商品分类", description = "更新商品分类信息")
    public Result<Boolean> updateCategory(
            @Parameter(description = "分类ID") @PathVariable Long id,
            @Parameter(description = "分类信息") @RequestBody
            @Valid @NotNull(message = "分类信息不能为空") CategoryDTO categoryDTO) {

        categoryDTO.setId(id);

        try {
            boolean result = categoryService.updateCategory(categoryDTO);
            return Result.success("商品分类更新成功", result);
        } catch (Exception e) {
            log.error("更新商品分类失败，分类ID: {}", id, e);
            return Result.error("更新商品分类失败: " + e.getMessage());
        }
    }

    /**
     * 删除商品分类
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "删除商品分类", description = "删除商品分类（逻辑删除）")
    public Result<Boolean> deleteCategory(
            @Parameter(description = "分类ID") @PathVariable
            @NotNull(message = "分类ID不能为空") Long id,
            @Parameter(description = "是否删除子分类") @RequestParam(defaultValue = "false") Boolean cascade) {

        try {
            boolean result = categoryService.deleteCategory(id, cascade);
            return Result.success("删除成功", result);
        } catch (Exception e) {
            log.error("删除商品分类失败，分类ID: {}", id, e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 更新分类状态
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "更新分类状态", description = "启用或禁用分类")
    public Result<Boolean> updateCategoryStatus(
            @Parameter(description = "分类ID") @PathVariable Long id,
            @Parameter(description = "分类状态") @RequestParam Integer status) {

        try {
            boolean result = categoryService.updateCategoryStatus(id, status);
            return Result.success("状态更新成功", result);
        } catch (Exception e) {
            log.error("更新分类状态失败，分类ID: {}, 状态: {}", id, status, e);
            return Result.error("更新状态失败: " + e.getMessage());
        }
    }

    /**
     * 更新分类排序
     */
    @PatchMapping("/{id}/sort")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "更新分类排序", description = "更新分类的排序值")
    public Result<Boolean> updateCategorySort(
            @Parameter(description = "分类ID") @PathVariable Long id,
            @Parameter(description = "排序值") @RequestParam Integer sort) {

        try {
            boolean result = categoryService.updateCategorySort(id, sort);
            return Result.success("排序更新成功", result);
        } catch (Exception e) {
            log.error("更新分类排序失败，分类ID: {}, 排序: {}", id, sort, e);
            return Result.error("更新排序失败: " + e.getMessage());
        }
    }

    /**
     * 移动分类到新的父分类
     */
    @PatchMapping("/{id}/move")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "移动分类", description = "将分类移动到新的父分类下")
    public Result<Boolean> moveCategory(
            @Parameter(description = "分类ID") @PathVariable Long id,
            @Parameter(description = "新父分类ID") @RequestParam Long newParentId) {

        try {
            boolean result = categoryService.moveCategory(id, newParentId);
            return Result.success("移动成功", result);
        } catch (Exception e) {
            log.error("移动分类失败，分类ID: {}, 新父分类ID: {}", id, newParentId, e);
            return Result.error("移动失败: " + e.getMessage());
        }
    }

    /**
     * 批量删除分类
     */
    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "批量删除分类", description = "批量删除商品分类")
    public Result<Boolean> deleteCategoriesBatch(
            @Parameter(description = "分类ID列表") @RequestBody
            @NotNull(message = "分类ID列表不能为空")
            @NotEmpty(message = "分类ID列表不能为空") List<Long> ids) {

        try {
            boolean result = categoryService.deleteCategoriesBatch(ids);
            return Result.success("批量删除成功", result);
        } catch (Exception e) {
            log.error("批量删除分类失败，分类IDs: {}", ids, e);
            return Result.error("批量删除失败: " + e.getMessage());
        }
    }

    /**
     * 批量更新分类状态
     */
    @PatchMapping("/batch/status")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "批量更新分类状态", description = "批量启用或禁用分类")
    public Result<Integer> updateCategoryStatusBatch(
            @Parameter(description = "分类ID列表") @RequestParam
            @NotNull(message = "分类ID列表不能为空") List<Long> ids,
            @Parameter(description = "分类状态") @RequestParam
            @NotNull(message = "分类状态不能为空") Integer status) {

        if (ids == null || ids.isEmpty()) {
            return Result.badRequest("分类ID列表不能为空");
        }

        if (ids.size() > 100) {
            return Result.badRequest("批量操作数量不能超过100个");
        }

        try {
            int successCount = 0;
            for (Long id : ids) {
                if (categoryService.updateCategoryStatus(id, status)) {
                    successCount++;
                }
            }
            log.info("批量更新分类状态完成, 成功: {}/{}", successCount, ids.size());
            return Result.success(String.format("批量更新分类状态成功: %d/%d", successCount, ids.size()), successCount);
        } catch (Exception e) {
            log.error("批量更新分类状态失败, IDs: {}", ids, e);
            return Result.error("批量更新状态失败: " + e.getMessage());
        }
    }

    /**
     * 批量创建分类
     */
    @PostMapping("/batch")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "批量创建分类", description = "批量创建多个分类")
    public Result<Integer> createCategoriesBatch(
            @Parameter(description = "分类信息列表") @RequestBody
            @Valid @NotEmpty(message = "分类信息列表不能为空") List<CategoryDTO> categoryList) {

        if (categoryList.size() > 100) {
            return Result.badRequest("批量创建数量不能超过100个");
        }

        try {
            int successCount = 0;
            for (CategoryDTO categoryDTO : categoryList) {
                try {
                    categoryService.createCategory(categoryDTO);
                    successCount++;
                } catch (Exception e) {
                    log.error("创建分类失败, name: {}", categoryDTO.getName(), e);
                }
            }
            log.info("批量创建分类完成, 成功: {}/{}", successCount, categoryList.size());
            return Result.success(String.format("批量创建分类成功: %d/%d", successCount, categoryList.size()), successCount);
        } catch (Exception e) {
            log.error("批量创建分类失败", e);
            return Result.error("批量创建失败: " + e.getMessage());
        }
    }
}
