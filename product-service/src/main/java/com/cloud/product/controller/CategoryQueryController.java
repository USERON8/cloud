package com.cloud.product.controller;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品分类查询控制器
 * 提供商品分类的查询功能
 */
@Slf4j
@RestController
@RequestMapping("/category/query")
@RequiredArgsConstructor
@Tag(name = "商品分类查询", description = "商品分类查询接口")
public class CategoryQueryController {

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