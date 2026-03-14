package com.cloud.payment.service.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;

@Slf4j
@Service
public class PaymentSecurityCacheService {

    private static final String IDEMPOTENT_PREFIX = "pay:idempotent:";
    private static final String RESULT_PREFIX = "pay:result:";
    private static final String STATUS_PREFIX = "pay:status:";
    private static final String STATUS_HASH_USER_ID = "userId";
    private static final String STATUS_HASH_STATUS = "status";
    private static final String RATE_PREFIX = "pay:rate:";
    private static final String ALIPAY_TOKEN_KEY = "pay:alipay:config";

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
            Object userIdValue = stringRedisTemplate.opsForHash().get(key, STATUS_HASH_USER_ID);
            Object statusValue = stringRedisTemplate.opsForHash().get(key, STATUS_HASH_STATUS);
            if (userIdValue == null || statusValue == null) {
                return null;
            }
            Long userId = Long.parseLong(String.valueOf(userIdValue));
            String status = String.valueOf(statusValue);
            if (status.isBlank()) {
                return null;
            }
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
        try {
            stringRedisTemplate.delete(key);
            stringRedisTemplate.opsForHash().putAll(key, java.util.Map.of(
                    STATUS_HASH_USER_ID, String.valueOf(userId),
                    STATUS_HASH_STATUS, status
            ));
            stringRedisTemplate.expire(key, Duration.ofSeconds(safeSeconds(statusTtlSeconds, 1)));
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
            Long count = stringRedisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                stringRedisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
            }
            return count == null || count <= limit;
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
