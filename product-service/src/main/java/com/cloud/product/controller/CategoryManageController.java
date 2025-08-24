package com.cloud.product.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.common.domain.Result;
import com.cloud.common.domain.dto.product.CategoryDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.product.converter.CategoryConverter;
import com.cloud.product.module.entity.Category;
import com.cloud.product.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 商品分类管理控制器
 * 提供商品分类的增删改功能
 */
@Slf4j
@RestController
@RequestMapping("/category/manage")
@RequiredArgsConstructor
@Tag(name = "商品分类管理", description = "商品分类管理接口")
public class CategoryManageController {

    private final CategoryService categoryService;
    private final CategoryConverter categoryConverter = CategoryConverter.INSTANCE;

    /**
     * 创建分类
     *
     * @param categoryDTO 分类信息
     * @return 创建结果
     */
    @PostMapping("/create")
    @Operation(summary = "创建分类", description = "创建新的商品分类")
    @ApiResponse(responseCode = "200", description = "创建成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<CategoryDTO> createCategory(
            @Parameter(description = "分类信息") @RequestBody CategoryDTO categoryDTO) {
        try {
            log.info("创建分类: {}", categoryDTO.getName());

            Category category = categoryConverter.toEntity(categoryDTO);
            boolean saved = categoryService.save(category);

            if (saved) {
                Category savedCategory = categoryService.getById(category.getId());
                CategoryDTO savedCategoryDTO = categoryConverter.toDTO(savedCategory);
                log.info("创建分类成功，分类ID: {}", savedCategory.getId());
                return Result.success(savedCategoryDTO);
            } else {
                log.error("创建分类失败: {}", categoryDTO.getName());
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "创建分类失败");
            }
        } catch (Exception e) {
            log.error("创建分类失败", e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "创建分类失败: " + e.getMessage());
        }
    }

    /**
     * 更新分类
     *
     * @param id          分类ID
     * @param categoryDTO 分类信息
     * @return 更新结果
     */
    @PutMapping("/update/{id}")
    @Operation(summary = "更新分类", description = "更新商品分类信息")
    @ApiResponse(responseCode = "200", description = "更新成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<CategoryDTO> updateCategory(
            @Parameter(description = "分类ID") @PathVariable Long id,
            @Parameter(description = "分类信息") @RequestBody CategoryDTO categoryDTO) {
        try {
            log.info("更新分类，分类ID: {}", id);

            Category existingCategory = categoryService.getById(id);
            if (existingCategory == null) {
                log.warn("分类不存在，分类ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "分类不存在");
            }

            Category category = categoryConverter.toEntity(categoryDTO);
            category.setId(id);
            boolean updated = categoryService.updateById(category);

            if (updated) {
                Category updatedCategory = categoryService.getById(id);
                CategoryDTO updatedCategoryDTO = categoryConverter.toDTO(updatedCategory);
                log.info("更新分类成功，分类ID: {}", id);
                return Result.success(updatedCategoryDTO);
            } else {
                log.error("更新分类失败，分类ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "更新分类失败");
            }
        } catch (Exception e) {
            log.error("更新分类失败，分类ID: {}", id, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "更新分类失败: " + e.getMessage());
        }
    }

    /**
     * 删除分类
     *
     * @param id 分类ID
     * @return 删除结果
     */
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除分类", description = "删除商品分类")
    @ApiResponse(responseCode = "200", description = "删除成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<Void> deleteCategory(
            @Parameter(description = "分类ID") @PathVariable Long id) {
        try {
            log.info("删除分类，分类ID: {}", id);

            Category existingCategory = categoryService.getById(id);
            if (existingCategory == null) {
                log.warn("分类不存在，分类ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "分类不存在");
            }

            // 检查是否有子分类
            long childCount = categoryService.count(new LambdaQueryWrapper<Category>()
                    .eq(Category::getParentId, id));
            if (childCount > 0) {
                log.warn("分类存在子分类，无法删除，分类ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "该分类存在子分类，无法删除");
            }

            boolean removed = categoryService.removeById(id);
            if (removed) {
                log.info("删除分类成功，分类ID: {}", id);
                return Result.success();
            } else {
                log.error("删除分类失败，分类ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "删除分类失败");
            }
        } catch (Exception e) {
            log.error("删除分类失败，分类ID: {}", id, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "删除分类失败: " + e.getMessage());
        }
    }
}