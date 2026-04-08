package com.cloud.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.exception.BizException;
import com.cloud.user.converter.UserConverter;
import com.cloud.user.mapper.UserMapper;
import com.cloud.user.module.entity.User;
import com.cloud.user.service.UserService;
import com.cloud.user.service.cache.TransactionalUserCacheService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

@ExtendWith(MockitoExtension.class)
class UserAsyncServiceImplTest {

  @Mock private UserService userService;

  @Mock private UserMapper userMapper;

  @Mock private UserConverter userConverter;

  @Mock private TransactionalUserCacheService userCacheService;

  @Mock private RedisTemplate<String, Object> redisTemplate;

  private UserAsyncServiceImpl userAsyncService;

  @BeforeEach
  void setUp() {
    userAsyncService =
        new UserAsyncServiceImpl(
            userService, userMapper, userConverter, userCacheService, redisTemplate);
  }

  @Test
  void getUsersByIdsAsync_batchesLargeRequests() {
    List<Long> ids = new ArrayList<>();
    for (long i = 1; i <= 60; i++) {
      ids.add(i);
    }

    when(userService.getUsersByIds(anyCollection()))
        .thenAnswer(
            invocation -> {
              List<Long> batch = new ArrayList<>(invocation.getArgument(0));
              List<UserDTO> result = new ArrayList<>();
              for (Long id : batch) {
                UserDTO dto = new UserDTO();
                dto.setId(id);
                result.add(dto);
              }
              return result;
            });

    CompletableFuture<List<UserDTO>> future = userAsyncService.getUsersByIdsAsync(ids);

    List<UserDTO> result = future.join();
    assertThat(result).hasSize(60);
    verify(userService, times(2)).getUsersByIds(anyCollection());
  }

  @Test
  void checkUsernamesExistAsync_handlesErrors() {
    when(userService.findByUsername("ok")).thenReturn(new UserDTO());
    when(userService.findByUsername("bad")).thenThrow(new BizException("fail"));

    Map<String, Boolean> result =
        userAsyncService.checkUsernamesExistAsync(List.of("ok", " ", "bad")).join();

    assertThat(result).containsEntry("ok", true).containsEntry("bad", false);
    assertThat(result).doesNotContainKey(" ");
  }

  @Test
  void refreshUserCacheAsync_refreshesExplicitRedisCache() {
    User latest = new User();
    latest.setId(9L);
    latest.setUsername("alice");
    when(userMapper.selectById(9L)).thenReturn(latest);

    userAsyncService.refreshUserCacheAsync(9L).join();

    verify(userCacheService).evictTransactional(9L, "alice");
    verify(userCacheService).putTransactional(latest);
    verify(userService, never()).getUserById(9L);
  }
}
