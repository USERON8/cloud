package com.cloud.common.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;




@Slf4j
@Component
@RequiredArgsConstructor
public class MessageIdempotencyService {

    private static final String KEY_PREFIX = "mq:idempotent";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_SUCCESS = "SUCCESS";

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${app.message.idempotent-enabled:true}")
    private boolean idempotentEnabled;

    @Value("${app.message.idempotent-expire-seconds:86400}")
    private long idempotentExpireSeconds;

    @Value("${app.message.idempotent-processing-expire-seconds:1800}")
    private long processingExpireSeconds;
    






    public boolean tryAcquire(String namespace, String eventId) {
        if (!idempotentEnabled) {
            return true;
        }
        if (!StringUtils.hasText(namespace) || !StringUtils.hasText(eventId)) {
            return true;
        }

        String key = buildKey(namespace, eventId);
        try {
            Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(
                    key,
                    STATUS_PROCESSING,
                    Duration.ofSeconds(Math.max(30, processingExpireSeconds))
            );
            if (Boolean.TRUE.equals(acquired)) {
                return true;
            }

            String status = stringRedisTemplate.opsForValue().get(key);
            if (STATUS_SUCCESS.equals(status) || STATUS_PROCESSING.equals(status)) {
                return false;
            }

            Boolean reacquired = stringRedisTemplate.opsForValue().setIfAbsent(
                    key,
                    STATUS_PROCESSING,
                    Duration.ofSeconds(Math.max(30, processingExpireSeconds))
            );
            return Boolean.TRUE.equals(reacquired);
        } catch (Exception e) {
            
            log.warn("Idempotent key acquire failed, skip check: namespace={}, eventId={}", namespace, eventId, e);
            return true;
        }
    }

    


    public void release(String namespace, String eventId) {
        if (!idempotentEnabled) {
            return;
        }
        if (!StringUtils.hasText(namespace) || !StringUtils.hasText(eventId)) {
            return;
        }
        String key = buildKey(namespace, eventId);
        try {
            stringRedisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Idempotent key release failed: namespace={}, eventId={}", namespace, eventId, e);
        }
    }

    public void markSuccess(String namespace, String eventId) {
        if (!idempotentEnabled) {
            return;
        }
        if (!StringUtils.hasText(namespace) || !StringUtils.hasText(eventId)) {
            return;
        }

        String key = buildKey(namespace, eventId);
        try {
            stringRedisTemplate.opsForValue().set(
                    key,
                    STATUS_SUCCESS,
                    Duration.ofSeconds(Math.max(60, idempotentExpireSeconds))
            );
        } catch (Exception e) {
            log.warn("Idempotent mark success failed: namespace={}, eventId={}", namespace, eventId, e);
        }
    }

    private String buildKey(String namespace, String eventId) {
        return KEY_PREFIX + ":" + namespace + ":" + eventId;
    }
}
