package com.cloud.search.service.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class HotKeywordSyncServiceTest {

  @Mock private StringRedisTemplate redisTemplate;

  @Mock private ZSetOperations<String, String> zSetOperations;

  @Mock private ObjectProvider<HotKeywordJdbcRepository> repositoryProvider;

  @Mock private HotKeywordJdbcRepository hotKeywordJdbcRepository;

  @InjectMocks private HotKeywordSyncService hotKeywordSyncService;

  @Test
  @SuppressWarnings("unchecked")
  void restoreFromDbOnStartup_loadsWhenCacheEmpty() {
    when(repositoryProvider.getIfAvailable()).thenReturn(hotKeywordJdbcRepository);
    when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    when(zSetOperations.size(HotKeywordKeys.TOTAL_KEY)).thenReturn(0L);
    when(hotKeywordJdbcRepository.loadTop(2))
        .thenReturn(
            List.of(
                new HotKeywordJdbcRepository.HotKeywordRecord("phone", 10L),
                new HotKeywordJdbcRepository.HotKeywordRecord("tablet", 5L)));
    ReflectionTestUtils.setField(hotKeywordSyncService, "dbSyncEnabled", true);
    ReflectionTestUtils.setField(hotKeywordSyncService, "restoreSize", 2);
    ReflectionTestUtils.setField(hotKeywordSyncService, "triggerMode", "scheduled");

    hotKeywordSyncService.restoreFromDbOnStartup();

    ArgumentCaptor<Set<ZSetOperations.TypedTuple<String>>> captor =
        ArgumentCaptor.forClass((Class) Set.class);
    verify(zSetOperations).add(eq(HotKeywordKeys.TOTAL_KEY), captor.capture());
    assertThat(captor.getValue()).hasSize(2);
  }

  @Test
  void restoreFromDbOnStartup_skipsWhenXxlModeEnabled() {
    ReflectionTestUtils.setField(hotKeywordSyncService, "dbSyncEnabled", true);
    ReflectionTestUtils.setField(hotKeywordSyncService, "triggerMode", "xxl");

    hotKeywordSyncService.restoreFromDbOnStartup();

    verifyNoInteractions(
        repositoryProvider, redisTemplate, zSetOperations, hotKeywordJdbcRepository);
  }

  @Test
  @SuppressWarnings("unchecked")
  void syncToDb_pushesTopKeywords() {
    when(repositoryProvider.getIfAvailable()).thenReturn(hotKeywordJdbcRepository);
    when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    when(zSetOperations.reverseRangeWithScores(eq(HotKeywordKeys.TOTAL_KEY), eq(0L), anyLong()))
        .thenReturn(
            Set.of(
                new DefaultTypedTuple<>("phone", 12.0D), new DefaultTypedTuple<>("tablet", 6.0D)));
    ReflectionTestUtils.setField(hotKeywordSyncService, "dbSyncEnabled", true);
    ReflectionTestUtils.setField(hotKeywordSyncService, "maxSyncSize", 5);

    hotKeywordSyncService.syncToDb();

    ArgumentCaptor<Map<String, Long>> captor = ArgumentCaptor.forClass((Class) Map.class);
    verify(hotKeywordJdbcRepository).upsertBatch(captor.capture());
    assertThat(captor.getValue()).containsEntry("phone", 12L).containsEntry("tablet", 6L);
  }
}
