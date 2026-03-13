package com.cloud.payment.service.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class PaymentSecurityCacheService {

    private static final String IDEMPOTENT_PREFIX = "pay:idempotent:";
    private static final String RESULT_PREFIX = "pay:result:";
    private static final String STATUS_PREFIX = "pay:status:";
    private static final String RATE_PREFIX = "pay:rate:";
    private static final String ALIPAY_TOKEN_KEY = "pay:alipay:token";
    private static final String STATUS_SEPARATOR = "|";

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${payment.security.idempotent-ttl-seconds:600}")
    private long idempotentTtlSeconds;

    @Value("${payment.security.result-ttl-seconds:600}")
    private long resultTtlSeconds;

    @Value("${payment.security.status-ttl-seconds:3}")
    private long statusTtlSeconds;

    @Value("${payment.security.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${payment.security.rate-limit.window-seconds:60}")
    private long rateLimitWindowSeconds;

    @Value("${payment.security.rate-limit.max-requests:20}")
    private int rateLimitMaxRequests;

    @Value("${payment.security.alipay-token-ttl-seconds:1500}")
    private long alipayTokenTtlSeconds;

    private final RedisScript<List> slidingWindowScript = RedisScript.of("""
            local key = KEYS[1]
            local now = tonumber(ARGV[1])
            local window_start = now - tonumber(ARGV[2])
            local limit = tonumber(ARGV[3])
            local window_size = tonumber(ARGV[2])
            redis.call('ZREMRANGEBYSCORE', key, 0, window_start)
            local current_count = redis.call('ZCARD', key)
            if current_count < limit then
                redis.call('ZADD', key, now, now .. ':' .. math.random())
                redis.call('EXPIRE', key, window_size)
                return {1, limit - current_count - 1, window_size}
            else
                return {0, 0, window_size}
            end
            """, List.class);

    public PaymentSecurityCacheService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public boolean tryAcquireIdempotent(String orderKey, String idempotencyKey) {
        if (orderKey == null || orderKey.isBlank()) {
            return true;
        }
        String key = IDEMPOTENT_PREFIX + orderKey;
        try {
            Boolean ok = stringRedisTemplate.opsForValue()
                    .setIfAbsent(key, safeValue(idempotencyKey), Duration.ofSeconds(safeSeconds(idempotentTtlSeconds, 60)));
            return Boolean.TRUE.equals(ok);
        } catch (Exception ex) {
            log.warn("Acquire idempotent key failed: key={}", key, ex);
            return true;
        }
    }

    public void markIdempotent(String orderKey, String idempotencyKey) {
        if (orderKey == null || orderKey.isBlank()) {
            return;
        }
        String key = IDEMPOTENT_PREFIX + orderKey;
        try {
            stringRedisTemplate.opsForValue()
                    .set(key, safeValue(idempotencyKey), Duration.ofSeconds(safeSeconds(idempotentTtlSeconds, 60)));
        } catch (Exception ex) {
            log.warn("Mark idempotent key failed: key={}", key, ex);
        }
    }

    public Long getCachedResult(String orderKey) {
        if (orderKey == null || orderKey.isBlank()) {
            return null;
        }
        String key = RESULT_PREFIX + orderKey;
        try {
            String value = stringRedisTemplate.opsForValue().get(key);
            if (value == null || value.isBlank()) {
                return null;
            }
            return Long.parseLong(value);
        } catch (Exception ex) {
            log.warn("Read payment result cache failed: key={}", key, ex);
            return null;
        }
    }

    public void cacheResult(String orderKey, Long paymentId) {
        if (orderKey == null || orderKey.isBlank() || paymentId == null) {
            return;
        }
        String key = RESULT_PREFIX + orderKey;
        try {
            stringRedisTemplate.opsForValue()
                    .set(key, String.valueOf(paymentId), Duration.ofSeconds(safeSeconds(resultTtlSeconds, 60)));
        } catch (Exception ex) {
            log.warn("Write payment result cache failed: key={}", key, ex);
        }
    }

    public CachedStatus getCachedStatus(String paymentKey) {
        if (paymentKey == null || paymentKey.isBlank()) {
            return null;
        }
        String key = STATUS_PREFIX + paymentKey;
        try {
            String value = stringRedisTemplate.opsForValue().get(key);
            if (value == null || value.isBlank()) {
                return null;
            }
            int idx = value.indexOf(STATUS_SEPARATOR);
            if (idx <= 0 || idx == value.length() - 1) {
                return null;
            }
            String userIdPart = value.substring(0, idx);
            String status = value.substring(idx + 1);
            Long userId = Long.parseLong(userIdPart);
            return new CachedStatus(userId, status);
        } catch (Exception ex) {
            log.warn("Read payment status cache failed: key={}", key, ex);
            return null;
        }
    }

    public void cacheStatus(String paymentKey, Long userId, String status) {
        if (paymentKey == null || paymentKey.isBlank() || userId == null || status == null || status.isBlank()) {
            return;
        }
        if (isFinalStatus(status)) {
            return;
        }
        String key = STATUS_PREFIX + paymentKey;
        String value = userId + STATUS_SEPARATOR + status;
        try {
            stringRedisTemplate.opsForValue()
                    .set(key, value, Duration.ofSeconds(safeSeconds(statusTtlSeconds, 1)));
        } catch (Exception ex) {
            log.warn("Write payment status cache failed: key={}", key, ex);
        }
    }

    public void evictStatus(String paymentKey) {
        if (paymentKey == null || paymentKey.isBlank()) {
            return;
        }
        try {
            stringRedisTemplate.delete(STATUS_PREFIX + paymentKey);
        } catch (Exception ex) {
            log.warn("Evict payment status cache failed: key={}", paymentKey, ex);
        }
    }

    public boolean allowRateLimit(Long userId) {
        if (!rateLimitEnabled || userId == null) {
            return true;
        }
        long windowSeconds = safeSeconds(rateLimitWindowSeconds, 60);
        int limit = Math.max(1, rateLimitMaxRequests);
        String key = RATE_PREFIX + userId;
        try {
            long now = System.currentTimeMillis() / 1000L;
            List result = stringRedisTemplate.execute(slidingWindowScript, List.of(key), now, windowSeconds, limit);
            boolean allowed = result != null && !result.isEmpty() && ((Number) result.get(0)).longValue() == 1;
            return allowed;
        } catch (Exception ex) {
            log.warn("Rate limit check failed: userId={}", userId, ex);
            return true;
        }
    }

    public String getAlipayToken() {
        try {
            String value = stringRedisTemplate.opsForValue().get(ALIPAY_TOKEN_KEY);
            return value == null || value.isBlank() ? null : value;
        } catch (Exception ex) {
            log.warn("Read alipay token cache failed", ex);
            return null;
        }
    }

    public void cacheAlipayToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue()
                    .set(ALIPAY_TOKEN_KEY, token, Duration.ofSeconds(safeSeconds(alipayTokenTtlSeconds, 60)));
        } catch (Exception ex) {
            log.warn("Write alipay token cache failed", ex);
        }
    }

    public boolean isFinalStatus(String status) {
        if (status == null) {
            return false;
        }
        return Objects.equals("PAID", status) || Objects.equals("FAILED", status);
    }

    private long safeSeconds(long value, long fallback) {
        if (value <= 0) {
            return fallback;
        }
        return value;
    }

    private String safeValue(String value) {
        return value == null ? "" : value;
    }

    public record CachedStatus(Long userId, String status) {
    }
}
