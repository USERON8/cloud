package com.cloud.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.product.module.entity.Category;

import java.util.List;

/**
 * @author what's up
 * @description 针对表【category(商品分类表)】的数据库操作Service
 * @createDate 2025-08-17 20:52:34
 */
public interface CategoryService extends IService<Category> {

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
}