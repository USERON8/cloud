package com.cloud.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.common.domain.dto.product.CategoryDTO;
import com.cloud.product.exception.CategoryException;
import com.cloud.product.module.entity.Category;

import java.util.List;








public interface CategoryServiceStandard extends IService<Category> {

    

    






    CategoryDTO getCategoryById(Long id) throws CategoryException.CategoryNotFoundException;

    






    CategoryDTO getCategoryByName(String name) throws CategoryException.CategoryNotFoundException;

    





    List<CategoryDTO> getCategoriesByIds(List<Long> ids);

    








    Page<CategoryDTO> getCategoriesPage(Integer page, Integer size, Long parentId, Integer status);

    





    List<CategoryDTO> getCategoryTree(Boolean enabledOnly);

    






    List<CategoryDTO> getChildrenCategories(Long parentId, Boolean recursive);

    




    List<CategoryDTO> getRootCategories();

    

    







    CategoryDTO createCategory(CategoryDTO categoryDTO)
            throws CategoryException.CategoryAlreadyExistsException, CategoryException.CategoryHierarchyException;

    






    boolean updateCategory(CategoryDTO categoryDTO) throws CategoryException.CategoryNotFoundException;

    









    boolean deleteCategory(Long id, Boolean cascade)
            throws CategoryException.CategoryNotFoundException,
            CategoryException.CategoryHasChildrenException,
            CategoryException.CategoryHasProductsException;

    





    boolean deleteCategoriesBatch(List<Long> ids);

    

    







    boolean updateCategoryStatus(Long id, Integer status) throws CategoryException.CategoryNotFoundException;

    






    boolean enableCategory(Long id) throws CategoryException.CategoryNotFoundException;

    






    boolean disableCategory(Long id) throws CategoryException.CategoryNotFoundException;

    

    







    boolean updateCategorySort(Long id, Integer sort) throws CategoryException.CategoryNotFoundException;

    








    boolean moveCategory(Long id, Long newParentId)
            throws CategoryException.CategoryNotFoundException, CategoryException.CategoryHierarchyException;

    

    






    Long getProductCountByCategoryId(Long id) throws CategoryException.CategoryNotFoundException;

    






    boolean hasChildren(Long id) throws CategoryException.CategoryNotFoundException;

    

    




    void evictCategoryCache(Long id);

    


    void evictAllCategoryCache();

    


    void warmupCategoryTreeCache();
}
