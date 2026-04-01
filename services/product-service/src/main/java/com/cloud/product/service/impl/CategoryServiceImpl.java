package com.cloud.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.common.domain.dto.product.CategoryDTO;
import com.cloud.product.mapper.CategoryMapper;
import com.cloud.product.messaging.ProductSyncMessageProducer;
import com.cloud.product.module.entity.Category;
import com.cloud.product.service.CategoryService;
import com.cloud.product.service.ProductCatalogService;
import com.cloud.product.service.cache.CategoryRedisCacheService;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category>
    implements CategoryService {

  private final CategoryRedisCacheService categoryRedisCacheService;
  private final ProductCatalogService productCatalogService;
  private final ProductSyncMessageProducer productSyncMessageProducer;

  @Override
  @Transactional(readOnly = true)
  public List<Category> getCategoryTree() {
    List<Category> cached = categoryRedisCacheService.getEntityTree();
    if (cached != null) {
      return cached;
    }
    List<Category> categories =
        this.list(
            new LambdaQueryWrapper<Category>()
                .eq(Category::getLevel, 1)
                .eq(Category::getStatus, 1)
                .orderByAsc(Category::getSortOrder));
    categoryRedisCacheService.putEntityTree(categories);
    return categories;
  }

  @Override
  @Transactional(readOnly = true)
  public List<Category> getChildrenByParentId(Long parentId) {
    return this.list(
        new LambdaQueryWrapper<Category>()
            .eq(Category::getParentId, parentId)
            .orderByAsc(Category::getSortOrder));
  }

  @Override
  @Transactional(readOnly = true)
  public List<Category> getCategoriesByLevel(Integer level) {
    return this.list(
        new LambdaQueryWrapper<Category>()
            .eq(Category::getLevel, level)
            .orderByAsc(Category::getSortOrder));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public boolean save(Category entity) {
    boolean saved = super.save(entity);
    if (saved) {
      categoryRedisCacheService.clearAllAfterCommit();
    }
    return saved;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public boolean updateById(Category entity) {
    boolean updated = super.updateById(entity);
    if (updated) {
      categoryRedisCacheService.clearAllAfterCommit();
    }
    return updated;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public boolean removeById(java.io.Serializable id) {
    boolean removed = super.removeById(id);
    if (removed) {
      categoryRedisCacheService.clearAllAfterCommit();
    }
    return removed;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public boolean removeByIds(java.util.Collection<?> idList) {
    boolean removed = super.removeByIds(idList);
    if (removed) {
      categoryRedisCacheService.clearAllAfterCommit();
    }
    return removed;
  }

  public void clearCategoryCache() {
    categoryRedisCacheService.clearAllAfterCommit();
  }

  public void evictCategoryCache(Long categoryId) {
    categoryRedisCacheService.clearAllAfterCommit();
  }

  @Override
  @Transactional(readOnly = true)
  public List<CategoryDTO> getCategoryTree(Boolean onlyEnabled) {
    List<CategoryDTO> cached = categoryRedisCacheService.getDtoTree(onlyEnabled);
    if (cached != null) {
      return cached;
    }

    LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
    if (Boolean.TRUE.equals(onlyEnabled)) {
      wrapper.eq(Category::getStatus, 1);
    }
    wrapper.orderByAsc(Category::getSortOrder);
    List<Category> allCategories = this.list(wrapper);

    if (CollectionUtils.isEmpty(allCategories)) {
      return List.of();
    }

    List<Category> firstLevel =
        allCategories.stream()
            .filter(category -> category.getLevel() == 1)
            .collect(Collectors.toList());

    firstLevel.forEach(
        first -> {
          List<Category> secondLevel =
              allCategories.stream()
                  .filter(category -> category.getParentId().equals(first.getId()))
                  .collect(Collectors.toList());

          secondLevel.forEach(
              second -> {
                List<Category> thirdLevel =
                    allCategories.stream()
                        .filter(category -> category.getParentId().equals(second.getId()))
                        .collect(Collectors.toList());
                second.setChildren(thirdLevel);
              });

          first.setChildren(secondLevel);
        });

    List<CategoryDTO> result = convertToDTO(firstLevel);
    categoryRedisCacheService.putDtoTree(onlyEnabled, result);
    return result;
  }

  @Override
  @Transactional(readOnly = true)
  public Page<CategoryDTO> getCategoriesPage(
      Integer page, Integer size, Long parentId, Integer level) {

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
  public CategoryDTO createCategory(CategoryDTO categoryDTO) {

    Category category = new Category();
    BeanUtils.copyProperties(categoryDTO, category);
    this.save(category);
    categoryDTO.setId(category.getId());
    return categoryDTO;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Boolean updateCategory(CategoryDTO categoryDTO) {

    Category category = new Category();
    BeanUtils.copyProperties(categoryDTO, category);
    boolean updated = this.updateById(category);
    if (updated) {
      syncProductsByCategoryIds(List.of(category.getId()));
    }
    return updated;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Boolean deleteCategory(Long categoryId, Boolean force) {
    Set<Long> affectedCategoryIds = new LinkedHashSet<>();
    affectedCategoryIds.add(categoryId);

    if (Boolean.TRUE.equals(force)) {

      List<Category> children =
          this.list(new LambdaQueryWrapper<Category>().eq(Category::getParentId, categoryId));
      if (!CollectionUtils.isEmpty(children)) {
        List<Long> childIds = children.stream().map(Category::getId).collect(Collectors.toList());
        affectedCategoryIds.addAll(childIds);
        this.removeByIds(childIds);
      }
    }
    boolean deleted = this.removeById(categoryId);
    if (deleted) {
      syncProductsByCategoryIds(affectedCategoryIds);
    }
    return deleted;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Boolean updateCategoryStatus(Long categoryId, Integer status) {

    Category category = new Category();
    category.setId(categoryId);
    category.setStatus(status);
    boolean updated = this.updateById(category);
    if (updated) {
      syncProductsByCategoryIds(List.of(categoryId));
    }
    return updated;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Boolean updateCategorySort(Long categoryId, Integer sort) {

    Category category = new Category();
    category.setId(categoryId);
    category.setSortOrder(sort);
    return this.updateById(category);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Boolean moveCategory(Long categoryId, Long targetParentId) {

    Category category = new Category();
    category.setId(categoryId);
    category.setParentId(targetParentId);
    return this.updateById(category);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Boolean deleteCategoriesBatch(List<Long> categoryIds) {
    boolean deleted = this.removeByIds(categoryIds);
    if (deleted) {
      syncProductsByCategoryIds(categoryIds);
    }
    return deleted;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public int updateCategoryStatusBatch(List<Long> categoryIds, Integer status) {
    if (CollectionUtils.isEmpty(categoryIds) || status == null) {
      return 0;
    }
    int successCount = 0;
    for (Long categoryId : categoryIds) {
      if (categoryId == null) {
        continue;
      }
      try {
        Category category = new Category();
        category.setId(categoryId);
        category.setStatus(status);
        if (super.updateById(category)) {
          successCount++;
        }
      } catch (Exception e) {
        log.warn("Batch update category status failed: id={}", categoryId, e);
      }
    }
    if (successCount > 0) {
      syncProductsByCategoryIds(categoryIds);
    }
    return successCount;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public int createCategoriesBatch(List<CategoryDTO> categoryList) {
    if (CollectionUtils.isEmpty(categoryList)) {
      return 0;
    }
    int successCount = 0;
    for (CategoryDTO categoryDTO : categoryList) {
      if (categoryDTO == null) {
        continue;
      }
      try {
        Category category = new Category();
        BeanUtils.copyProperties(categoryDTO, category);
        if (super.save(category)) {
          successCount++;
        }
      } catch (Exception e) {
        log.warn("Batch create category failed: name={}", categoryDTO.getName(), e);
      }
    }
    return successCount;
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
    return categories.stream().map(this::convertToDTO).collect(Collectors.toList());
  }

  private void syncProductsByCategoryIds(java.util.Collection<Long> categoryIds) {
    if (CollectionUtils.isEmpty(categoryIds)) {
      return;
    }
    for (Long categoryId : categoryIds) {
      if (categoryId == null) {
        continue;
      }
      List<com.cloud.common.domain.vo.product.SpuDetailVO> spus =
          productCatalogService.listSpuByCategory(categoryId, null);
      if (CollectionUtils.isEmpty(spus)) {
        continue;
      }
      spus.stream()
          .map(com.cloud.common.domain.vo.product.SpuDetailVO::getSpuId)
          .filter(id -> id != null)
          .distinct()
          .forEach(productSyncMessageProducer::sendUpsert);
    }
  }
}
