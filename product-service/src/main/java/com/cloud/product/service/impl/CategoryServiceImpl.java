package com.cloud.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.product.mapper.CategoryMapper;
import com.cloud.product.module.entity.Category;
import com.cloud.product.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 商品分类服务实现类
 * 针对表【category(商品分类表)】的数据库操作Service实现
 * 使用多级缓存提升性能，遵循事务管理规范
 *
 * @author what's up
 * @since 1.0.0
 */
@Slf4j
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category>
        implements CategoryService {

    /**
     * 获取分类树结构
     * 使用多级缓存，缓存时间2小时（分类变动较少）
     *
     * @return 分类树列表
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "categoryTreeCache", key = "'tree'")
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
     * 使用多级缓存，缓存时间90分钟
     *
     * @param parentId 父分类ID
     * @return 子分类列表
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "categoryCache", key = "'children:' + #parentId")
    public List<Category> getChildrenByParentId(Long parentId) {
        log.debug("从数据库获取父分类ID为{}的子分类", parentId);
        return this.list(new LambdaQueryWrapper<Category>()
                .eq(Category::getParentId, parentId)
                .eq(Category::getStatus, 1)
                .orderByAsc(Category::getSortOrder));
    }

    /**
     * 获取指定层级的分类
     * 使用多级缓存，缓存时间90分钟
     *
     * @param level 层级 1-一级分类 2-二级分类 3-三级分类
     * @return 分类列表
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "categoryCache", key = "'level:' + #level")
    public List<Category> getCategoriesByLevel(Integer level) {
        log.debug("从数据库获取{}级分类", level);
        return this.list(new LambdaQueryWrapper<Category>()
                .eq(Category::getLevel, level)
                .eq(Category::getStatus, 1)
                .orderByAsc(Category::getSortOrder));
    }

    // ================= 写操作方法 =================

    /**
     * 保存分类（重写父类方法以添加缓存管理）
     *
     * @param entity 分类实体
     * @return 是否保存成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = {"categoryCache", "categoryTreeCache"}, allEntries = true)
    public boolean save(Category entity) {
        log.info("保存分类: {}", entity.getName());
        return super.save(entity);
    }

    /**
     * 更新分类（重写父类方法以添加缓存管理）
     *
     * @param entity 分类实体
     * @return 是否更新成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = {"categoryCache", "categoryTreeCache"}, allEntries = true)
    public boolean updateById(Category entity) {
        log.info("更新分类: ID={}, Name={}", entity.getId(), entity.getName());
        return super.updateById(entity);
    }

    /**
     * 删除分类（重写父类方法以添加缓存管理）
     *
     * @param id 分类ID
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = {"categoryCache", "categoryTreeCache"}, allEntries = true)
    public boolean removeById(java.io.Serializable id) {
        log.info("删除分类: {}", id);
        return super.removeById(id);
    }

    /**
     * 批量删除分类（重写父类方法以添加缓存管理）
     *
     * @param idList 分类ID列表
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = {"categoryCache", "categoryTreeCache"}, allEntries = true)
    public boolean removeByIds(java.util.Collection<?> idList) {
        log.info("批量删除分类: {}", idList);
        return super.removeByIds(idList);
    }

    // ================= 缓存管理方法 =================

    /**
     * 清除分类缓存
     */
    @CacheEvict(cacheNames = {"categoryCache", "categoryTreeCache"}, allEntries = true)
    public void clearCategoryCache() {
        log.info("清除所有分类缓存");
    }

    /**
     * 清除指定分类的缓存
     *
     * @param categoryId 分类ID
     */
    @CacheEvict(cacheNames = "categoryCache", key = "'children:' + #categoryId")
    public void evictCategoryCache(Long categoryId) {
        log.info("清除分类缓存: {}", categoryId);
    }
}