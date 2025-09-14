package com.cloud.product.controller.category;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.common.domain.dto.product.CategoryDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.result.Result;
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

import java.util.List;

/**
 * 商品分类控制器
 * 提供商品分类的增删改查及分类树功能
 *
 * @author 代码规范团队
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Tag(name = "商品分类", description = "商品分类管理接口")
public class CategoryController {

    private final CategoryService categoryService;
    private final CategoryConverter categoryConverter = CategoryConverter.INSTANCE;

    /**
     * 获取分类树
     *
     * @return 分类树
     */
    @GetMapping("/tree")
    @Operation(summary = "获取分类树", description = "获取三级分类树结构")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<List<CategoryDTO>> getCategoryTree() {
        try {
            log.info("获取分类树");
            List<Category> categoryTree = categoryService.getCategoryTree();
            List<CategoryDTO> categoryDTOList = categoryConverter.toDTOList(categoryTree);
            buildCategoryTreeDTO(categoryDTOList, categoryTree);
            log.info("获取分类树成功，共{}个一级分类", categoryDTOList.size());
            return Result.success(categoryDTOList);
        } catch (Exception e) {
            log.error("获取分类树失败", e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "获取分类树失败: " + e.getMessage());
        }
    }

    /**
     * 获取指定层级的分类
     *
     * @param level 层级 1-一级分类 2-二级分类 3-三级分类
     * @return 分类列表
     */
    @GetMapping("/level/{level}")
    @Operation(summary = "获取指定层级分类", description = "根据层级获取分类列表")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<List<CategoryDTO>> getCategoriesByLevel(
            @Parameter(description = "层级 1-一级分类 2-二级分类 3-三级分类") @PathVariable Integer level) {
        try {
            log.info("获取{}级分类", level);
            List<Category> categories = categoryService.getCategoriesByLevel(level);
            List<CategoryDTO> categoryDTOList = categoryConverter.toDTOList(categories);
            log.info("获取{}级分类成功，共{}条记录", level, categoryDTOList.size());
            return Result.success(categoryDTOList);
        } catch (Exception e) {
            log.error("获取{}级分类失败", level, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "获取分类失败: " + e.getMessage());
        }
    }

    /**
     * 根据父ID获取子分类
     *
     * @param parentId 父分类ID
     * @return 子分类列表
     */
    @GetMapping("/children/{parentId}")
    @Operation(summary = "获取子分类", description = "根据父ID获取子分类列表")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<List<CategoryDTO>> getChildrenByParentId(
            @Parameter(description = "父分类ID") @PathVariable Long parentId) {
        try {
            log.info("获取父分类ID为{}的子分类", parentId);
            List<Category> categories = categoryService.getChildrenByParentId(parentId);
            List<CategoryDTO> categoryDTOList = categoryConverter.toDTOList(categories);
            log.info("获取父分类ID为{}的子分类成功，共{}条记录", parentId, categoryDTOList.size());
            return Result.success(categoryDTOList);
        } catch (Exception e) {
            log.error("获取父分类ID为{}的子分类失败", parentId, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "获取子分类失败: " + e.getMessage());
        }
    }

    /**
     * 创建分类
     *
     * @param categoryDTO 分类信息
     * @return 创建结果
     */
    @PostMapping
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
    @PutMapping("/{id}")
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
    @DeleteMapping("/{id}")
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

    /**
     * 递归构建分类树DTO
     *
     * @param dtoList    DTO列表
     * @param entityList 实体列表
     */
    private void buildCategoryTreeDTO(List<CategoryDTO> dtoList, List<Category> entityList) {
        dtoList.forEach(dto -> {
            List<Category> children = entityList.stream()
                    .filter(entity -> entity.getParentId().equals(dto.getId()))
                    .toList();
            if (!children.isEmpty()) {
                List<CategoryDTO> childrenDTO = categoryConverter.toDTOList(children);
                dto.setChildren(childrenDTO);
                buildCategoryTreeDTO(dto.getChildren(), children);
            }
        });
    }
}