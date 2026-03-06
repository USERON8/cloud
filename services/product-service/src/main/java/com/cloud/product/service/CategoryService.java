package com.cloud.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.common.domain.dto.product.CategoryDTO;
import com.cloud.product.module.entity.Category;

import java.util.List;









public interface CategoryService extends IService<Category> {

    

    




    List<Category> getCategoryTree();

    





    List<CategoryDTO> getCategoryTree(Boolean onlyEnabled);

    





    List<Category> getChildrenByParentId(Long parentId);

    





    List<Category> getCategoriesByLevel(Integer level);

    








    Page<CategoryDTO> getCategoriesPage(
            Integer page, Integer size, Long parentId, Integer level);

    





    CategoryDTO getCategoryById(Long categoryId);

    






    List<CategoryDTO> getChildrenCategories(Long parentId, Boolean onlyEnabled);

    





    CategoryDTO createCategory(CategoryDTO categoryDTO);

    





    Boolean updateCategory(CategoryDTO categoryDTO);

    






    Boolean deleteCategory(Long categoryId, Boolean force);

    






    Boolean updateCategoryStatus(Long categoryId, Integer status);

    






    Boolean updateCategorySort(Long categoryId, Integer sort);

    






    Boolean moveCategory(Long categoryId, Long targetParentId);

    





    Boolean deleteCategoriesBatch(List<Long> categoryIds);

    

    


    void clearCategoryCache();

    




    void evictCategoryCache(Long categoryId);
}
