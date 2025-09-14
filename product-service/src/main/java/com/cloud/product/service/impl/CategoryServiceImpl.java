package com.cloud.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.product.mapper.CategoryMapper;
import com.cloud.product.module.entity.Category;
import com.cloud.product.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品分类服务实现类
 * 针对表【category(商品分类表)】的数据库操作Service实现
 *
 * @author 代码规范团队
 * @since 1.0.0
 */
@Slf4j
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category>
        implements CategoryService {

    /**
     * 获取分类树结构
     *
     * @return 分类树列表
     */
    @Cacheable(value = "category", key = "'tree'")
    @Override
    public List<Category> getCategoryTree() {
        log.info("从数据库获取分类树");
        // 获取所有启用的分类
        List<Category> allCategories = this.list(new LambdaQueryWrapper<Category>()
                .eq(Category::getStatus, 1)
                .orderByAsc(Category::getSortOrder));

        if (CollectionUtils.isEmpty(allCategories)) {
            return List.of();
        }

        // 构建三级分类树
        // 一级分类
        List<Category> firstLevel = allCategories.stream()
                .filter(category -> category.getLevel() == 1)
                .collect(Collectors.toList());

        // 为每个一级分类设置二级分类
        firstLevel.forEach(first -> {
            List<Category> secondLevel = allCategories.stream()
                    .filter(category -> category.getParentId().equals(first.getId()))
                    .collect(Collectors.toList());

            // 为每个二级分类设置三级分类
            secondLevel.forEach(second -> {
                List<Category> thirdLevel = allCategories.stream()
                        .filter(category -> category.getParentId().equals(second.getId()))
                        .collect(Collectors.toList());
                second.setChildren(thirdLevel);
            });

            first.setChildren(secondLevel);
        });

        return firstLevel;
    }

    /**
     * 根据父ID获取子分类
     *
     * @param parentId 父分类ID
     * @return 子分类列表
     */
    @Cacheable(value = "category", key = "'children_' + #parentId")
    @Override
    public List<Category> getChildrenByParentId(Long parentId) {
        log.info("从数据库获取父分类ID为{}的子分类", parentId);
        return this.list(new LambdaQueryWrapper<Category>()
                .eq(Category::getParentId, parentId)
                .eq(Category::getStatus, 1)
                .orderByAsc(Category::getSortOrder));
    }

    /**
     * 获取指定层级的分类
     *
     * @param level 层级 1-一级分类 2-二级分类 3-三级分类
     * @return 分类列表
     */
    @Cacheable(value = "category", key = "'level_' + #level")
    @Override
    public List<Category> getCategoriesByLevel(Integer level) {
        log.info("从数据库获取{}级分类", level);
        return this.list(new LambdaQueryWrapper<Category>()
                .eq(Category::getLevel, level)
                .eq(Category::getStatus, 1)
                .orderByAsc(Category::getSortOrder));
    }

    /**
     * 清除分类缓存
     */
    @CacheEvict(value = "category", allEntries = true)
    public void clearCategoryCache() {
        log.info("清除分类缓存");
    }
}