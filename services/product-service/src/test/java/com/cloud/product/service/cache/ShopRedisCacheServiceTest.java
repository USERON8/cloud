package com.cloud.product.service.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.common.result.PageResult;
import com.cloud.product.module.dto.ShopPageDTO;
import com.cloud.product.module.vo.ShopVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class ShopRedisCacheServiceTest {

  @Mock private StringRedisTemplate stringRedisTemplate;
  @Mock private ValueOperations<String, String> valueOperations;

  private ShopRedisCacheService shopRedisCacheService;

  @BeforeEach
  void setUp() {
    shopRedisCacheService = new ShopRedisCacheService(stringRedisTemplate, new ObjectMapper());
    when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
  }

  @Test
  void getById_readsCachedJson() throws Exception {
    ShopVO shopVO = new ShopVO();
    shopVO.setId(9L);
    when(valueOperations.get("product:shop:id:9"))
        .thenReturn(new ObjectMapper().writeValueAsString(shopVO));

    ShopVO result = shopRedisCacheService.getById(9L);

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(9L);
    verify(stringRedisTemplate)
        .expire(
            org.mockito.ArgumentMatchers.eq("product:shop:id:9"),
            org.mockito.ArgumentMatchers.any());
  }

  @Test
  void putPage_serializesPageResult() {
    ShopPageDTO pageDTO = new ShopPageDTO();
    pageDTO.setCurrent(2L);
    pageDTO.setSize(5L);

    PageResult<ShopVO> pageResult = PageResult.of(List.of(new ShopVO()), 1L, 2L, 5L);
    shopRedisCacheService.putPage(pageDTO, pageResult);

    verify(valueOperations)
        .set(
            org.mockito.ArgumentMatchers.eq("product:shop:page:2:5:null:null:null:null:null:null"),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any());
  }
}
