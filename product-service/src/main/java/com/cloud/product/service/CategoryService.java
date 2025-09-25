package com.cloud.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
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