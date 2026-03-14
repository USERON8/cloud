package com.cloud.search.service.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import cn.hutool.core.util.StrUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class HotKeywordSyncService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectProvider<HotKeywordJdbcRepository> repositoryProvider;

    @Value("${search.hot-keyword.db-sync.enabled:true}")
    private boolean dbSyncEnabled;

    @Value("${search.hot-keyword.db-sync.max-sync-size:2000}")
    private int maxSyncSize;

    @Value("${search.hot-keyword.db-sync.restore-size:2000}")
    private int restoreSize;

    public HotKeywordSyncService(StringRedisTemplate redisTemplate,
                                 ObjectProvider<HotKeywordJdbcRepository> repositoryProvider) {
        this.redisTemplate = redisTemplate;
        this.repositoryProvider = repositoryProvider;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void restoreFromDbOnStartup() {
        if (!dbSyncEnabled) {
            return;
        }
        HotKeywordJdbcRepository repository = repositoryProvider.getIfAvailable();
        if (repository == null) {
            log.warn("Hot keyword DB sync disabled: repository missing");
            return;
        }
        try {
            Long size = redisTemplate.opsForZSet().size(HotKeywordKeys.TOTAL_KEY);
            if (size != null && size > 0) {
                return;
            }
            int limit = Math.max(1, restoreSize);
            var records = repository.loadTop(limit);
            if (records == null || records.isEmpty()) {
                return;
            }
            Set<ZSetOperations.TypedTuple<String>> tuples = new HashSet<>(records.size());
            for (var record : records) {
                if (record == null || StrUtil.isBlank(record.keyword())) {
                    continue;
                }
                tuples.add(new DefaultTypedTuple<>(record.keyword(), (double) record.totalScore()));
            }
            if (!tuples.isEmpty()) {
                redisTemplate.opsForZSet().add(HotKeywordKeys.TOTAL_KEY, tuples);
            }
        } catch (Exception ex) {
            log.warn("Restore hot keywords from DB failed", ex);
        }
    }

    @Scheduled(fixedDelayString = "${search.hot-keyword.db-sync.interval-ms:300000}")
    public void syncToDb() {
        if (!dbSyncEnabled) {
            return;
        }
        HotKeywordJdbcRepository repository = repositoryProvider.getIfAvailable();
        if (repository == null) {
            return;
        }
        try {
            int limit = Math.max(1, maxSyncSize);
            Set<ZSetOperations.TypedTuple<String>> tuples =
                    redisTemplate.opsForZSet().reverseRangeWithScores(HotKeywordKeys.TOTAL_KEY, 0, limit - 1L);
            if (tuples == null || tuples.isEmpty()) {
                return;
            }
            Map<String, Long> totals = new HashMap<>();
            for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                if (tuple == null || StrUtil.isBlank(tuple.getValue()) || tuple.getScore() == null) {
                    continue;
                }
                totals.put(tuple.getValue(), Math.round(tuple.getScore()));
            }
            if (totals.isEmpty()) {
                return;
            }
            repository.upsertBatch(totals);
        } catch (Exception ex) {
            log.warn("Sync hot keywords to DB failed", ex);
        }
    }
}
