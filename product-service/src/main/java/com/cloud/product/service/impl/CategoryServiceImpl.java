package com.cloud.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.common.domain.dto.product.CategoryDTO;
import com.cloud.product.mapper.CategoryMapper;
import com.cloud.product.module.entity.Category;
import com.cloud.product.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
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

    // ================= 新增的方法 =================

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDTO> getCategoryTree(Boolean onlyEnabled) {
        log.info("从数据库获取分类树，只返回启用: {}", onlyEnabled);
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        if (Boolean.TRUE.equals(onlyEnabled)) {
            wrapper.eq(Category::getStatus, 1);
        }
        wrapper.orderByAsc(Category::getSortOrder);
        List<Category> allCategories = this.list(wrapper);

        if (CollectionUtils.isEmpty(allCategories)) {
            return List.of();
        }

        // 构建三级分类树
        List<Category> firstLevel = allCategories.stream()
                .filter(category -> category.getLevel() == 1)
                .collect(Collectors.toList());

        firstLevel.forEach(first -> {
            List<Category> secondLevel = allCategories.stream()
                    .filter(category -> category.getParentId().equals(first.getId()))
                    .collect(Collectors.toList());

            secondLevel.forEach(second -> {
                List<Category> thirdLevel = allCategories.stream()
                        .filter(category -> category.getParentId().equals(second.getId()))
                        .collect(Collectors.toList());
                second.setChildren(thirdLevel);
            });

            first.setChildren(secondLevel);
        });

        return convertToDTO(firstLevel);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CategoryDTO> getCategoriesPage(Integer page, Integer size, Long parentId, Integer level) {
        log.info("分页查询分类，页码: {}, 大小: {}, 父ID: {}, 层级: {}", page, size, parentId, level);
        Page<Category> entityPage = new Page<>(page, size);
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        if (parentId != null) {
            wrapper.eq(Category::getParentId, parentId);
        }
        if (level != null) {
            wrapper.eq(Category::getLevel, level);
        }
        wrapper.orderByAsc(Category::getSortOrder);
        Page<Category> result = this.page(entityPage, wrapper);

        Page<CategoryDTO> dtoPage = new Page<>();
        dtoPage.setCurrent(result.getCurrent());
        dtoPage.setSize(result.getSize());
        dtoPage.setTotal(result.getTotal());
        dtoPage.setRecords(convertToDTO(result.getRecords()));
        return dtoPage;
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDTO getCategoryById(Long categoryId) {
        log.info("根据ID获取分类: {}", categoryId);
        Category category = this.getById(categoryId);
        return category != null ? convertToDTO(category) : null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDTO> getChildrenCategories(Long parentId, Boolean onlyEnabled) {
        log.info("获取子分类，父ID: {}, 只返回启用: {}", parentId, onlyEnabled);
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getParentId, parentId);
        if (Boolean.TRUE.equals(onlyEnabled)) {
            wrapper.eq(Category::getStatus, 1);
        }
        wrapper.orderByAsc(Category::getSortOrder);
        List<Category> children = this.list(wrapper);
        return convertToDTO(children);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = {"categoryCache", "categoryTreeCache"}, allEntries = true)
    public CategoryDTO createCategory(CategoryDTO categoryDTO) {
        log.info("创建分类: {}", categoryDTO.getName());
        Category category = new Category();
        BeanUtils.copyProperties(categoryDTO, category);
        this.save(category);
        categoryDTO.setId(category.getId());
        return categoryDTO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = {"categoryCache", "categoryTreeCache"}, allEntries = true)
    public Boolean updateCategory(CategoryDTO categoryDTO) {
        log.info("更新分类: ID={}, Name={}", categoryDTO.getId(), categoryDTO.getName());
        Category category = new Category();
        BeanUtils.copyProperties(categoryDTO, category);
        return this.updateById(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = {"categoryCache", "categoryTreeCache"}, allEntries = true)
    public Boolean deleteCategory(Long categoryId, Boolean force) {
        log.info("删除分类: {}, 强制删除: {}", categoryId, force);
        if (Boolean.TRUE.equals(force)) {
            // 强制删除，包括子分类
            List<Category> children = this.list(new LambdaQueryWrapper<Category>()
                    .eq(Category::getParentId, categoryId));
            if (!CollectionUtils.isEmpty(children)) {
                List<Long> childIds = children.stream().map(Category::getId).collect(Collectors.toList());
                this.removeByIds(childIds);
            }
        }
        return this.removeById(categoryId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = {"categoryCache", "categoryTreeCache"}, allEntries = true)
    public Boolean updateCategoryStatus(Long categoryId, Integer status) {
        log.info("更新分类状态: {}, 状态: {}", categoryId, status);
        Category category = new Category();
        category.setId(categoryId);
        category.setStatus(status);
        return this.updateById(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = {"categoryCache", "categoryTreeCache"}, allEntries = true)
    public Boolean updateCategorySort(Long categoryId, Integer sort) {
        log.info("更新分类排序: {}, 排序: {}", categoryId, sort);
        Category category = new Category();
        category.setId(categoryId);
        category.setSortOrder(sort);
        return this.updateById(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = {"categoryCache", "categoryTreeCache"}, allEntries = true)
    public Boolean moveCategory(Long categoryId, Long targetParentId) {
        log.info("移动分类: {}, 目标父ID: {}", categoryId, targetParentId);
        Category category = new Category();
        category.setId(categoryId);
        category.setParentId(targetParentId);
        return this.updateById(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = {"categoryCache", "categoryTreeCache"}, allEntries = true)
    public Boolean deleteCategoriesBatch(List<Long> categoryIds) {
        log.info("批量删除分类: {}", categoryIds);
        return this.removeByIds(categoryIds);
    }

    // ================= 辅助方法 =================

    /**
     * 将Category实体转换为CategoryDTO
     */
    private CategoryDTO convertToDTO(Category category) {
        if (category == null) {
            return null;
        }
        CategoryDTO dto = new CategoryDTO();
        BeanUtils.copyProperties(category, dto);
        if (!CollectionUtils.isEmpty(category.getChildren())) {
            dto.setChildren(convertToDTO(category.getChildren()));
        }
        return dto;
    }

    /**
     * 将Category实体列表转换为CategoryDTO列表
     */
    private List<CategoryDTO> convertToDTO(List<Category> categories) {
        if (CollectionUtils.isEmpty(categories)) {
            return new ArrayList<>();
        }
        return categories.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
}
