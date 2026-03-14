package com.cloud.product.service.impl;

import com.cloud.common.domain.dto.product.CategoryDTO;
import com.cloud.product.module.entity.Category;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Test
    void getCategoryTree_buildsHierarchy() {
        CategoryServiceImpl service = spy(new CategoryServiceImpl());

        Category level1 = new Category();
        level1.setId(1L);
        level1.setLevel(1);
        level1.setParentId(0L);
        level1.setStatus(1);

        Category level2 = new Category();
        level2.setId(2L);
        level2.setLevel(2);
        level2.setParentId(1L);
        level2.setStatus(1);

        Category level3 = new Category();
        level3.setId(3L);
        level3.setLevel(3);
        level3.setParentId(2L);
        level3.setStatus(1);

        doReturn(List.of(level1, level2, level3)).when(service).list(any());

        List<CategoryDTO> tree = service.getCategoryTree(true);

        assertThat(tree).hasSize(1);
        CategoryDTO first = tree.get(0);
        assertThat(first.getChildren()).hasSize(1);
        CategoryDTO second = first.getChildren().get(0);
        assertThat(second.getChildren()).hasSize(1);
    }
}
