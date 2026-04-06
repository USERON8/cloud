package com.cloud.product.service.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.common.domain.dto.product.CategoryDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.TaskScheduler;

@ExtendWith(MockitoExtension.class)
class CategoryRedisCacheServiceTest {

  @Mock private StringRedisTemplate stringRedisTemplate;
  @Mock private ValueOperations<String, String> valueOperations;
  @Mock private TaskScheduler taskScheduler;

  private CategoryRedisCacheService categoryRedisCacheService;

  @BeforeEach
  void setUp() {
    categoryRedisCacheService =
        new CategoryRedisCacheService(stringRedisTemplate, new ObjectMapper(), taskScheduler);
    when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
  }

  @Test
  void getDtoTree_readsCachedJson() throws Exception {
    CategoryDTO dto = new CategoryDTO();
    dto.setId(1L);
    String json = new ObjectMapper().writeValueAsString(List.of(dto));
    when(valueOperations.get("product:category:tree:dto:enabled")).thenReturn(json);

    List<CategoryDTO> result = categoryRedisCacheService.getDtoTree(true);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getId()).isEqualTo(1L);
    verify(stringRedisTemplate)
        .expire(
            org.mockito.ArgumentMatchers.eq("product:category:tree:dto:enabled"),
            org.mockito.ArgumentMatchers.any());
  }
}
