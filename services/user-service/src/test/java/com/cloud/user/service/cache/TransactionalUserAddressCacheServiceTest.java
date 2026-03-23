package com.cloud.user.service.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.user.module.entity.UserAddress;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TransactionalUserAddressCacheServiceTest {

  @Mock private StringRedisTemplate redisTemplate;

  @Mock private HashOperations<String, Object, Object> hashOperations;

  @InjectMocks private TransactionalUserAddressCacheService transactionalUserAddressCacheService;

  @BeforeEach
  void setUp() {
    when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    ReflectionTestUtils.setField(transactionalUserAddressCacheService, "ttlSeconds", 120L);
  }

  @Test
  void getById_parsesCache() {
    when(hashOperations.entries("user:address:1"))
        .thenReturn(
            Map.of(
                "id", "1",
                "userId", "9",
                "consignee", "Alice",
                "phone", "13800138000",
                "detailAddress", "Road 1",
                "isDefault", "1"));

    TransactionalUserAddressCacheService.UserAddressCache cache =
        transactionalUserAddressCacheService.getById(1L);

    assertThat(cache).isNotNull();
    assertThat(cache.id()).isEqualTo(1L);
    assertThat(cache.userId()).isEqualTo(9L);
    assertThat(cache.detailAddress()).isEqualTo("Road 1");
    verify(redisTemplate).expire("user:address:1", Duration.ofSeconds(120));
  }

  @Test
  void getByUserId_returnsCachedAddressList() {
    when(hashOperations.entries("user:address:user:9")).thenReturn(Map.of("ids", "1,2"));
    when(hashOperations.entries("user:address:1"))
        .thenReturn(Map.of("id", "1", "userId", "9", "consignee", "A", "phone", "13800138000"));
    when(hashOperations.entries("user:address:2"))
        .thenReturn(Map.of("id", "2", "userId", "9", "consignee", "B", "phone", "13900139000"));

    List<TransactionalUserAddressCacheService.UserAddressCache> caches =
        transactionalUserAddressCacheService.getByUserId(9L);

    assertThat(caches).hasSize(2);
    assertThat(caches).extracting("id").containsExactly(1L, 2L);
  }

  @Test
  void putUserList_writesDetailAndListKeys() {
    UserAddress first = new UserAddress();
    first.setId(1L);
    first.setUserId(9L);
    first.setConsignee("A");
    first.setPhone("13800138000");

    UserAddress second = new UserAddress();
    second.setId(2L);
    second.setUserId(9L);
    second.setConsignee("B");
    second.setPhone("13900139000");

    transactionalUserAddressCacheService.putUserList(9L, List.of(first, second));

    verify(hashOperations, times(3)).putAll(any(), any());
    verify(redisTemplate, times(3)).expire(any(), any(Duration.class));
  }
}
