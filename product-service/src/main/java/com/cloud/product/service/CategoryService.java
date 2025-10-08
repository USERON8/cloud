package com.cloud.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.common.domain.dto.product.CategoryDTO;
import com.cloud.product.module.entity.Category;

import java.util.List;

/**
 * 商品分类服务接口
 * 提供分类相关的业务操作，包括CRUD、树形结构查询、缓存管理等
 * 使用多级缓存提升性能，遵循事务管理规范
 *
 * @author what's up
 * @since 1.0.0
 */
public interface CategoryService extends IService<Category> {

    // ================= 查询操作 =================

    /**
     * 获取分类树结构
     *
     * @return 分类树列表
     */
    List<Category> getCategoryTree();

    /**
     * 获取分类树结构
     *
     * @param onlyEnabled 是否只返回启用的分类
     * @return 分类树列表
     */
    List<CategoryDTO> getCategoryTree(Boolean onlyEnabled);

    /**
     * 根据父ID获取子分类
     *
     * @param parentId 父分类ID
     * @return 子分类列表
     */
    List<Category> getChildrenByParentId(Long parentId);

    /**
     * 获取指定层级的分类
     *
     * @param level 层级 1-一级分类 2-二级分类 3-三级分类
     * @return 分类列表
     */
    List<Category> getCategoriesByLevel(Integer level);

    /**
     * 分页查询分类
     *
     * @param page     页码
     * @param size     每页数量
     * @param parentId 父分类ID
     * @param level    层级
     * @return 分页结果
     */
    Page<CategoryDTO> getCategoriesPage(
            Integer page, Integer size, Long parentId, Integer level);

    /**
     * 根据ID获取分类详情
     *
     * @param categoryId 分类ID
     * @return 分类详情
     */
    CategoryDTO getCategoryById(Long categoryId);

    /**
     * 获取子分类
     *
     * @param parentId    父分类ID
     * @param onlyEnabled 是否只返回启用的分类
     * @return 子分类列表
     */
    List<CategoryDTO> getChildrenCategories(Long parentId, Boolean onlyEnabled);

    /**
     * 创建分类
     *
     * @param categoryDTO 分类信息
     * @return 创建的分类DTO
     */
    CategoryDTO createCategory(CategoryDTO categoryDTO);

    /**
     * 更新分类
     *
     * @param categoryDTO 分类信息
     * @return 是否成功
     */
    Boolean updateCategory(CategoryDTO categoryDTO);

    /**
     * 删除分类
     *
     * @param categoryId 分类ID
     * @param force      是否强制删除（包括子分类）
     * @return 是否成功
     */
    Boolean deleteCategory(Long categoryId, Boolean force);

    /**
     * 更新分类状态
     *
     * @param categoryId 分类ID
     * @param status     状态
     * @return 是否成功
     */
    Boolean updateCategoryStatus(Long categoryId, Integer status);

    /**
     * 更新分类排序
     *
     * @param categoryId 分类ID
     * @param sort       排序值
     * @return 是否成功
     */
    Boolean updateCategorySort(Long categoryId, Integer sort);

    /**
     * 移动分类
     *
     * @param categoryId     分类ID
     * @param targetParentId 目标父分类ID
     * @return 是否成功
     */
    Boolean moveCategory(Long categoryId, Long targetParentId);

    /**
     * 批量删除分类
     *
     * @param categoryIds 分类ID列表
     * @return 是否成功
     */
    Boolean deleteCategoriesBatch(List<Long> categoryIds);

    // ================= 缓存管理 =================

    /**
     * 清除所有分类缓存
     */
    void clearCategoryCache();

    /**
     * 清除指定分类的缓存
     *
     * @param categoryId 分类ID
     */
    void evictCategoryCache(Long categoryId);
}