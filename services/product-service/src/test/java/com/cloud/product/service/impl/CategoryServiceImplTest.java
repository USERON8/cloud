package com.cloud.product.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.cloud.common.domain.dto.product.CategoryDTO;
import com.cloud.product.converter.CategoryConverter;
import com.cloud.product.messaging.ProductSyncMessageProducer;
import com.cloud.product.module.entity.Category;
import com.cloud.product.service.ProductCatalogService;
import com.cloud.product.service.cache.CategoryRedisCacheService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

  @Mock private CategoryRedisCacheService categoryRedisCacheService;
  @Mock private CategoryConverter categoryConverter;
  @Mock private ProductCatalogService productCatalogService;
  @Mock private ProductSyncMessageProducer productSyncMessageProducer;

  @Test
  void getCategoryTree_buildsHierarchy() {
    when(categoryRedisCacheService.getDtoTree(true)).thenReturn(null);
    lenient()
        .when(categoryConverter.toDTO(org.mockito.ArgumentMatchers.any(Category.class)))
        .thenAnswer(
            invocation -> {
              Category source = invocation.getArgument(0);
              CategoryDTO dto = new CategoryDTO();
              dto.setId(source.getId());
              dto.setParentId(source.getParentId());
              dto.setLevel(source.getLevel());
              dto.setStatus(source.getStatus());
              dto.setName(source.getName());
              return dto;
            });
    CategoryServiceImpl service =
        spy(
            new CategoryServiceImpl(
                categoryConverter,
                categoryRedisCacheService,
                productCatalogService,
                productSyncMessageProducer));

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

    doReturn(List.of(level1, level2, level3))
        .when(service)
        .list(
            org.mockito.ArgumentMatchers
                .<com.baomidou.mybatisplus.core.conditions.Wrapper<Category>>any());

    List<CategoryDTO> tree = service.getCategoryTree(true);

    assertThat(tree).hasSize(1);
    CategoryDTO first = tree.get(0);
    assertThat(first.getChildren()).hasSize(1);
    CategoryDTO second = first.getChildren().get(0);
    assertThat(second.getChildren()).hasSize(1);
  }
}
