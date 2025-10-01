package com.cloud.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.common.domain.dto.product.CategoryDTO;
import com.cloud.product.exception.CategoryException;
import com.cloud.product.module.entity.Category;

import java.util.List;

/**
 * 商品分类服务接口
 * 提供商品分类相关的业务操作，包括CRUD、树形结构、状态管理等
 *
 * @author what's up
 * @since 1.0.0
 */
public interface CategoryServiceStandard extends IService<Category> {

    // ================= 查询操作 =================

    /**
     * 根据ID获取分类详情
     *
     * @param id 分类ID
     * @return 分类DTO
     * @throws CategoryException.CategoryNotFoundException 分类不存在异常
     */
    CategoryDTO getCategoryById(Long id) throws CategoryException.CategoryNotFoundException;

    /**
     * 根据名称获取分类
     *
     * @param name 分类名称
     * @return 分类DTO
     * @throws CategoryException.CategoryNotFoundException 分类不存在异常
     */
    CategoryDTO getCategoryByName(String name) throws CategoryException.CategoryNotFoundException;

    /**
     * 根据ID列表批量获取分类
     *
     * @param ids 分类ID列表
     * @return 分类DTO列表
     */
    List<CategoryDTO> getCategoriesByIds(List<Long> ids);

    /**
     * 分页查询分类
     *
     * @param page     页码
     * @param size     每页数量
     * @param parentId 父分类ID
     * @param status   分类状态
     * @return 分页结果
     */
    Page<CategoryDTO> getCategoriesPage(Integer page, Integer size, Long parentId, Integer status);

    /**
     * 获取树形分类结构
     *
     * @param enabledOnly 是否只返回启用的分类
     * @return 树形分类列表
     */
    List<CategoryDTO> getCategoryTree(Boolean enabledOnly);

    /**
     * 获取子分类列表
     *
     * @param parentId  父分类ID
     * @param recursive 是否递归获取所有子分类
     * @return 子分类列表
     */
    List<CategoryDTO> getChildrenCategories(Long parentId, Boolean recursive);

    /**
     * 获取根分类列表
     *
     * @return 根分类列表
     */
    List<CategoryDTO> getRootCategories();

    // ================= 创建和更新操作 =================

    /**
     * 创建分类
     *
     * @param categoryDTO 分类信息
     * @return 创建的分类DTO
     * @throws CategoryException.CategoryAlreadyExistsException 分类已存在异常
     * @throws CategoryException.CategoryHierarchyException    分类层级异常
     */
    CategoryDTO createCategory(CategoryDTO categoryDTO)
            throws CategoryException.CategoryAlreadyExistsException, CategoryException.CategoryHierarchyException;

    /**
     * 更新分类
     *
     * @param categoryDTO 分类信息
     * @return 是否更新成功
     * @throws CategoryException.CategoryNotFoundException 分类不存在异常
     */
    boolean updateCategory(CategoryDTO categoryDTO) throws CategoryException.CategoryNotFoundException;

    /**
     * 删除分类
     *
     * @param id      分类ID
     * @param cascade 是否级联删除子分类
     * @return 是否删除成功
     * @throws CategoryException.CategoryNotFoundException     分类不存在异常
     * @throws CategoryException.CategoryHasChildrenException  分类包含子分类异常
     * @throws CategoryException.CategoryHasProductsException  分类包含商品异常
     */
    boolean deleteCategory(Long id, Boolean cascade)
            throws CategoryException.CategoryNotFoundException,
            CategoryException.CategoryHasChildrenException,
            CategoryException.CategoryHasProductsException;

    /**
     * 批量删除分类
     *
     * @param ids 分类ID列表
     * @return 是否删除成功
     */
    boolean deleteCategoriesBatch(List<Long> ids);

    // ================= 状态管理 =================

    /**
     * 更新分类状态
     *
     * @param id     分类ID
     * @param status 状态
     * @return 是否更新成功
     * @throws CategoryException.CategoryNotFoundException 分类不存在异常
     */
    boolean updateCategoryStatus(Long id, Integer status) throws CategoryException.CategoryNotFoundException;

    /**
     * 启用分类
     *
     * @param id 分类ID
     * @return 是否启用成功
     * @throws CategoryException.CategoryNotFoundException 分类不存在异常
     */
    boolean enableCategory(Long id) throws CategoryException.CategoryNotFoundException;

    /**
     * 禁用分类
     *
     * @param id 分类ID
     * @return 是否禁用成功
     * @throws CategoryException.CategoryNotFoundException 分类不存在异常
     */
    boolean disableCategory(Long id) throws CategoryException.CategoryNotFoundException;

    // ================= 排序管理 =================

    /**
     * 更新分类排序
     *
     * @param id   分类ID
     * @param sort 排序值
     * @return 是否更新成功
     * @throws CategoryException.CategoryNotFoundException 分类不存在异常
     */
    boolean updateCategorySort(Long id, Integer sort) throws CategoryException.CategoryNotFoundException;

    /**
     * 移动分类到新的父分类
     *
     * @param id          分类ID
     * @param newParentId 新父分类ID
     * @return 是否移动成功
     * @throws CategoryException.CategoryNotFoundException    分类不存在异常
     * @throws CategoryException.CategoryHierarchyException 分类层级异常
     */
    boolean moveCategory(Long id, Long newParentId)
            throws CategoryException.CategoryNotFoundException, CategoryException.CategoryHierarchyException;

    // ================= 统计信息 =================

    /**
     * 获取分类下的商品数量
     *
     * @param id 分类ID
     * @return 商品数量
     * @throws CategoryException.CategoryNotFoundException 分类不存在异常
     */
    Long getProductCountByCategoryId(Long id) throws CategoryException.CategoryNotFoundException;

    /**
     * 检查分类是否有子分类
     *
     * @param id 分类ID
     * @return 是否有子分类
     * @throws CategoryException.CategoryNotFoundException 分类不存在异常
     */
    boolean hasChildren(Long id) throws CategoryException.CategoryNotFoundException;

    // ================= 缓存管理 =================

    /**
     * 清除分类缓存
     *
     * @param id 分类ID
     */
    void evictCategoryCache(Long id);

    /**
     * 清除所有分类缓存
     */
    void evictAllCategoryCache();

    /**
     * 预热分类树缓存
     */
    void warmupCategoryTreeCache();
}
