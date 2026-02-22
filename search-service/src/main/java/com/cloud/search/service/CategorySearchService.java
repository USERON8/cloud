package com.cloud.search.service;

import com.cloud.search.document.CategoryDocument;

import java.util.List;








public interface CategorySearchService {

    




    void deleteCategory(Long categoryId);

    





    void updateCategoryStatus(Long categoryId, Integer status);

    





    CategoryDocument findByCategoryId(Long categoryId);


    




    void batchDeleteCategories(List<Long> categoryIds);

    





    boolean isEventProcessed(String traceId);

    




    void markEventProcessed(String traceId);

    


    void rebuildCategoryIndex();

    




    boolean indexExists();

    


    void createCategoryIndex();

    


    void deleteCategoryIndex();
}
