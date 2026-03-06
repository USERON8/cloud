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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;









@Slf4j
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category>
        implements CategoryService {

    





    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "categoryTreeCache", key = "'tree'")
    public List<Category> getCategoryTree() {
        

        return this.list(new LambdaQueryWrapper<Category>()
                .eq(Category::getLevel, 1)
                .eq(Category::getStatus, 1)
                .orderByAsc(Category::getSortOrder));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> getChildrenByParentId(Long parentId) {
        return this.list(new LambdaQueryWrapper<Category>()
                .eq(Category::getParentId, parentId)
                .orderByAsc(Category::getSortOrder));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> getCategoriesByLevel(Integer level) {
        return this.list(new LambdaQueryWrapper<Category>()
                .eq(Category::getLevel, level)
                .orderByAsc(Category::getSortOrder));
    }

    

    





    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = {"categoryCache", "categoryTreeCache"}, allEntries = true)
    public boolean save(Category entity) {
        
        return super.save(entity);
    }

    





    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = {"categoryCache", "categoryTreeCache"}, allEntries = true)
    public boolean updateById(Category entity) {
        
        return super.updateById(entity);
    }

    





    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = {"categoryCache", "categoryTreeCache"}, allEntries = true)
    public boolean removeById(java.io.Serializable id) {
        
        return super.removeById(id);
    }

    





    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = {"categoryCache", "categoryTreeCache"}, allEntries = true)
    public boolean removeByIds(java.util.Collection<?> idList) {
        
        return super.removeByIds(idList);
    }

    

    


    @CacheEvict(cacheNames = {"categoryCache", "categoryTreeCache"}, allEntries = true)
    public void clearCategoryCache() {
    }

    




    @CacheEvict(cacheNames = "categoryCache", key = "'children:' + #categoryId")
    public void evictCategoryCache(Long categoryId) {
        
    }

    

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDTO> getCategoryTree(Boolean onlyEnabled) {
        
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        if (Boolean.TRUE.equals(onlyEnabled)) {
            wrapper.eq(Category::getStatus, 1);
        }
        wrapper.orderByAsc(Category::getSortOrder);
        List<Category> allCategories = this.list(wrapper);

        if (CollectionUtils.isEmpty(allCategories)) {
            return List.of();
        }

        
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
        
        Category category = this.getById(categoryId);
        return category != null ? convertToDTO(category) : null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDTO> getChildrenCategories(Long parentId, Boolean onlyEnabled) {
        
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
        
        Category category = new Category();
        BeanUtils.copyProperties(categoryDTO, category);
        return this.updateById(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = {"categoryCache", "categoryTreeCache"}, allEntries = true)
    public Boolean deleteCategory(Long categoryId, Boolean force) {
        
        if (Boolean.TRUE.equals(force)) {
            
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
        
        Category category = new Category();
        category.setId(categoryId);
        category.setStatus(status);
        return this.updateById(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = {"categoryCache", "categoryTreeCache"}, allEntries = true)
    public Boolean updateCategorySort(Long categoryId, Integer sort) {
        
        Category category = new Category();
        category.setId(categoryId);
        category.setSortOrder(sort);
        return this.updateById(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = {"categoryCache", "categoryTreeCache"}, allEntries = true)
    public Boolean moveCategory(Long categoryId, Long targetParentId) {
        
        Category category = new Category();
        category.setId(categoryId);
        category.setParentId(targetParentId);
        return this.updateById(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = {"categoryCache", "categoryTreeCache"}, allEntries = true)
    public Boolean deleteCategoriesBatch(List<Long> categoryIds) {
        
        return this.removeByIds(categoryIds);
    }

    

    


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

    


    private List<CategoryDTO> convertToDTO(List<Category> categories) {
        if (CollectionUtils.isEmpty(categories)) {
            return new ArrayList<>();
        }
        return categories.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
}

