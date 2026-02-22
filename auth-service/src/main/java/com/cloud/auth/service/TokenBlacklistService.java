package com.cloud.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class TokenBlacklistService {

    private static final String BLACKLIST_KEY_PREFIX = "oauth2:blacklist:";
    private static final String BLACKLIST_STATS_KEY = "oauth2:blacklist:stats";

    private final RedisTemplate<String, Object> redisTemplate;

    public TokenBlacklistService(@Qualifier("oauth2MainRedisTemplate") RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void addToBlacklist(Jwt jwt) {
        if (jwt == null || jwt.getTokenValue() == null) {
            log.warn("Skip blacklisting because token is null");
            return;
        }

        Instant expiresAt = jwt.getExpiresAt();
        if (expiresAt == null || !expiresAt.isAfter(Instant.now())) {
            return;
        }

        long ttlSeconds = Duration.between(Instant.now(), expiresAt).getSeconds();
        addToBlacklist(jwt.getTokenValue(), jwt.getSubject(), ttlSeconds, "manual_revocation");
    }

    public void addToBlacklist(String tokenValue, String subject, long ttlSeconds, String reason) {
        if (tokenValue == null || tokenValue.trim().isEmpty()) {
            log.warn("Skip blacklisting because token value is empty");
            return;
        }

        long safeTtlSeconds = Math.max(ttlSeconds, 60);
        String tokenId = generateTokenId(tokenValue);
        String blacklistKey = BLACKLIST_KEY_PREFIX + tokenId;

        TokenBlacklistInfo info = new TokenBlacklistInfo(
                tokenId,
                subject,
                "auth-service",
                Instant.now(),
                Instant.now().plusSeconds(safeTtlSeconds),
                reason
        );

        redisTemplate.opsForValue().set(blacklistKey, info, safeTtlSeconds, TimeUnit.SECONDS);
        redisTemplate.opsForHash().increment(BLACKLIST_STATS_KEY, "total_blacklisted", 1);
        redisTemplate.opsForHash().increment(BLACKLIST_STATS_KEY, "active_blacklisted", 1);
        redisTemplate.opsForHash().put(BLACKLIST_STATS_KEY, "last_updated", Instant.now().toString());
    }

    public boolean isBlacklisted(Jwt jwt) {
        if (jwt == null || jwt.getTokenValue() == null) {
            return false;
        }
        return isBlacklisted(jwt.getTokenValue());
    }

    public boolean isBlacklisted(String tokenValue) {
        if (tokenValue == null || tokenValue.trim().isEmpty()) {
            return false;
        }

        String tokenId = generateTokenId(tokenValue);
        String blacklistKey = BLACKLIST_KEY_PREFIX + tokenId;
        Boolean exists = redisTemplate.hasKey(blacklistKey);
        return Boolean.TRUE.equals(exists);
    }

    public int cleanupExpiredEntries() {
        try {
            Set<String> allKeys = redisTemplate.keys(BLACKLIST_KEY_PREFIX + "*");
            if (allKeys == null || allKeys.isEmpty()) {
                return 0;
            }

            int cleanedCount = 0;
            for (String key : allKeys) {
                Long ttl = redisTemplate.getExpire(key);
                if (ttl != null && ttl <= 0) {
                    redisTemplate.delete(key);
                    cleanedCount++;
                }
            }

            if (cleanedCount > 0) {
                redisTemplate.opsForHash().increment(BLACKLIST_STATS_KEY, "active_blacklisted", -cleanedCount);
                redisTemplate.opsForHash().put(BLACKLIST_STATS_KEY, "last_updated", Instant.now().toString());
            }

            return cleanedCount;
        } catch (Exception e) {
            log.error("Failed to cleanup expired blacklist entries", e);
            return 0;
        }
    }

    public BlacklistStats getBlacklistStats() {
        try {
            Map<Object, Object> stats = redisTemplate.opsForHash().entries(BLACKLIST_STATS_KEY);
            long totalBlacklisted = parseLong(stats.get("total_blacklisted"));
            int activeBlacklisted = (int) parseLong(stats.get("active_blacklisted"));
            String lastUpdatedRaw = stats.get("last_updated") != null ? stats.get("last_updated").toString() : null;
            Instant lastUpdated = lastUpdatedRaw != null ? Instant.parse(lastUpdatedRaw) : Instant.now();
            return new BlacklistStats(totalBlacklisted, activeBlacklisted, lastUpdated);
        } catch (Exception e) {
            return new BlacklistStats(0L, 0, Instant.now());
        }
    }

    private long parseLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private String generateTokenId(String tokenValue) {
        return Integer.toString(tokenValue.hashCode());
    }

    public static class TokenBlacklistInfo {
        public final String tokenId;
        public final String subject;
        public final String issuer;
        public final Instant blacklistedAt;
        public final Instant expiresAt;
        public final String reason;

        public TokenBlacklistInfo(String tokenId, String subject, String issuer,
                                  Instant blacklistedAt, Instant expiresAt, String reason) {
            this.tokenId = tokenId;
            this.subject = subject;
            this.issuer = issuer;
            this.blacklistedAt = blacklistedAt;
            this.expiresAt = expiresAt;
            this.reason = reason;
        }
    }

    public static class BlacklistStats {
        public final long totalBlacklisted;
        public final int activeBlacklisted;
        public final Instant lastUpdated;

        public BlacklistStats(long totalBlacklisted, int activeBlacklisted, Instant lastUpdated) {
            this.totalBlacklisted = totalBlacklisted;
            this.activeBlacklisted = activeBlacklisted;
            this.lastUpdated = lastUpdated;
        }

        public long totalCount() {
            return totalBlacklisted;
        }

        public int activeCount() {
            return activeBlacklisted;
        }
    }
}
