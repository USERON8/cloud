package com.cloud.user.service.support;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

@ExtendWith(MockitoExtension.class)
class AuthAuthorityCacheEvictServiceTest {

  @Mock private RedisTemplate<String, Object> redisTemplate;

  @Test
  void evictUserDeletesUserCacheAndPublishesUserId() {
    AuthAuthorityCacheEvictService service = new AuthAuthorityCacheEvictService(redisTemplate);

    service.evictUser(42L);

    verify(redisTemplate).delete("auth:user:42");
    verify(redisTemplate).convertAndSend("auth:cache:evict", "42");
  }

  @Test
  void evictUserIgnoresNullUserId() {
    AuthAuthorityCacheEvictService service = new AuthAuthorityCacheEvictService(redisTemplate);

    service.evictUser(null);

    verifyNoInteractions(redisTemplate);
  }

  @Test
  void evictUsersDeletesDistinctNonNullKeysAndPublishesDeletedKeys() {
    AuthAuthorityCacheEvictService service = new AuthAuthorityCacheEvictService(redisTemplate);

    service.evictUsers(List.of(42L, 43L, 42L));

    verify(redisTemplate)
        .delete(
            org.mockito.ArgumentMatchers.<Collection<String>>argThat(
                keys -> keys.equals(new java.util.LinkedHashSet<>(List.of("auth:user:42", "auth:user:43")))));
    verify(redisTemplate).convertAndSend("auth:cache:evict", "auth:user:42,auth:user:43");
  }

  @Test
  void evictUsersDoesNothingWhenNoValidIdsExist() {
    AuthAuthorityCacheEvictService service = new AuthAuthorityCacheEvictService(redisTemplate);

    service.evictUsers(List.of());

    verify(redisTemplate, never()).delete("auth:user:null");
    verifyNoInteractions(redisTemplate);
  }
}
