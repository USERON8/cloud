package com.cloud.search.repository;

import com.cloud.search.document.CategoryDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;








@Repository
public interface CategoryDocumentRepository extends ElasticsearchRepository<CategoryDocument, String> {

    






    @Query("{\"bool\": {\"must\": [{\"match\": {\"name\": \"?0\"}}]}}")
    Page<CategoryDocument> findByNameContaining(String name, Pageable pageable);

    






    Page<CategoryDocument> findByParentId(Long parentId, Pageable pageable);

    






    Page<CategoryDocument> findByLevel(Integer level, Pageable pageable);

    






    Page<CategoryDocument> findByStatus(Integer status, Pageable pageable);

    





    @Query("{\"bool\": {\"must\": [{\"term\": {\"level\": 1}}, {\"term\": {\"status\": 1}}]}}")
    Page<CategoryDocument> findRootCategories(Pageable pageable);

    







    @Query("{\"bool\": {\"must\": [{\"term\": {\"parentId\": ?0}}, {\"term\": {\"status\": ?1}}]}}")
    Page<CategoryDocument> findByParentIdAndStatus(Long parentId, Integer status, Pageable pageable);

    







    @Query("{\"bool\": {\"must\": [{\"term\": {\"level\": ?0}}, {\"term\": {\"status\": ?1}}]}}")
    Page<CategoryDocument> findByLevelAndStatus(Integer level, Integer status, Pageable pageable);

    







    @Query("{\"bool\": {\"must\": [{\"match\": {\"name\": \"?0\"}}, {\"term\": {\"status\": ?1}}]}}")
    Page<CategoryDocument> searchByNameAndStatus(String name, Integer status, Pageable pageable);

    






    @Query("{\"bool\": {\"must\": [{\"term\": {\"parentId\": ?0}}, {\"term\": {\"status\": ?1}}]}}")
    List<CategoryDocument> findCategoryTree(Long parentId, Integer status);

    







    @Query("{\"bool\": {\"must\": [{\"term\": {\"parentId\": ?0}}, {\"term\": {\"status\": ?1}}]}}")
    Page<CategoryDocument> findByParentIdAndStatusOrderBySortOrder(Long parentId, Integer status, Pageable pageable);

    








    @Query("{\"bool\": {\"must\": [{\"match\": {\"name\": \"?0\"}}, {\"term\": {\"level\": ?1}}, {\"term\": {\"status\": ?2}}]}}")
    Page<CategoryDocument> advancedSearch(String name, Integer level, Integer status, Pageable pageable);
}
