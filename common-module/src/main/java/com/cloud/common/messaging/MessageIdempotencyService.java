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

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${app.message.idempotent-enabled:true}")
    private boolean idempotentEnabled;

    @Value("${app.message.idempotent-expire-seconds:86400}")
    private long idempotentExpireSeconds;

    






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
                    "1",
                    Duration.ofSeconds(Math.max(60, idempotentExpireSeconds))
            );
            return Boolean.TRUE.equals(acquired);
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

    private String buildKey(String namespace, String eventId) {
        return KEY_PREFIX + ":" + namespace + ":" + eventId;
    }
}
