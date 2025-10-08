package com.cloud.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * JWT令牌黑名单服务
 * 用于管理被撤销的JWT令牌，防止已撤销令牌继续被使用
 *
 * @author what's up
 */
@Slf4j
@Service
public class TokenBlacklistService {

    private static final String BLACKLIST_KEY_PREFIX = "oauth2:blacklist:";
    private static final String BLACKLIST_STATS_KEY = "oauth2:blacklist:stats";
    private final RedisTemplate<String, Object> redisTemplate;

    public TokenBlacklistService(@Qualifier("oauth2MainRedisTemplate") RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 将JWT令牌加入黑名单
     *
     * @param jwt JWT令牌对象
     */
    public void addToBlacklist(Jwt jwt) {
        if (jwt == null || jwt.getTokenValue() == null) {
            log.warn("尝试将空令牌加入黑名单");
            return;
        }

        String tokenId = extractTokenId(jwt);
        String blacklistKey = BLACKLIST_KEY_PREFIX + tokenId;

        // 计算令牌剩余有效期，设置Redis过期时间
        Instant expiresAt = jwt.getExpiresAt();
        if (expiresAt != null && expiresAt.isAfter(Instant.now())) {
            long ttlSeconds = Duration.between(Instant.now(), expiresAt).getSeconds();

            // 存储令牌基本信息到黑名单
            TokenBlacklistInfo info = new TokenBlacklistInfo(
                    tokenId,
                    jwt.getSubject(),
                    jwt.getIssuer().toString(),
                    Instant.now(),
                    expiresAt,
                    "manual_revocation"
            );

            redisTemplate.opsForValue().set(blacklistKey, info, ttlSeconds, TimeUnit.SECONDS);

            // 更新统计信息
            redisTemplate.opsForHash().increment(BLACKLIST_STATS_KEY, "total_blacklisted", 1);
            redisTemplate.opsForHash().increment(BLACKLIST_STATS_KEY, "active_blacklisted", 1);

            log.info("JWT令牌已加入黑名单: tokenId={}, subject={}, ttl={}秒",
                    tokenId, jwt.getSubject(), ttlSeconds);
        } else {
            log.debug("令牌已过期，无需加入黑名单: tokenId={}", tokenId);
        }
    }

    /**
     * 将令牌值加入黑名单（用于非JWT令牌）
     *
     * @param tokenValue 令牌值
     * @param subject    令牌主体
     * @param ttlSeconds 过期时间（秒）
     * @param reason     撤销原因
     */
    public void addToBlacklist(String tokenValue, String subject, long ttlSeconds, String reason) {
        if (tokenValue == null || tokenValue.trim().isEmpty()) {
            log.warn("尝试将空令牌值加入黑名单");
            return;
        }

        String tokenId = generateTokenId(tokenValue);
        String blacklistKey = BLACKLIST_KEY_PREFIX + tokenId;

        TokenBlacklistInfo info = new TokenBlacklistInfo(
                tokenId,
                subject,
                "auth-service",
                Instant.now(),
                Instant.now().plusSeconds(ttlSeconds),
                reason
        );

        redisTemplate.opsForValue().set(blacklistKey, info, ttlSeconds, TimeUnit.SECONDS);

        // 更新统计信息
        redisTemplate.opsForHash().increment(BLACKLIST_STATS_KEY, "total_blacklisted", 1);
        redisTemplate.opsForHash().increment(BLACKLIST_STATS_KEY, "active_blacklisted", 1);

        log.info("令牌已加入黑名单: tokenId={}, subject={}, reason={}, ttl={}秒",
                tokenId, subject, reason, ttlSeconds);
    }

    /**
     * 检查JWT令牌是否在黑名单中
     *
     * @param jwt JWT令牌对象
     * @return true如果在黑名单中，false否则
     */
    public boolean isBlacklisted(Jwt jwt) {
        if (jwt == null || jwt.getTokenValue() == null) {
            return false;
        }

        String tokenId = extractTokenId(jwt);
        String blacklistKey = BLACKLIST_KEY_PREFIX + tokenId;

        boolean exists = Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));

        if (exists) {
            log.debug("检测到黑名单令牌: tokenId={}, subject={}", tokenId, jwt.getSubject());
        }

        return exists;
    }

    /**
     * 检查令牌值是否在黑名单中
     *
     * @param tokenValue 令牌值
     * @return true如果在黑名单中，false否则
     */
    public boolean isBlacklisted(String tokenValue) {
        if (tokenValue == null || tokenValue.trim().isEmpty()) {
            return false;
        }

        String tokenId = generateTokenId(tokenValue);
        String blacklistKey = BLACKLIST_KEY_PREFIX + tokenId;

        return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
    }

    /**
     * 获取黑名单统计信息
     *
     * @return 黑名单统计信息
     */
    public BlacklistStats getBlacklistStats() {
        try {
            Long totalBlacklisted = (Long) redisTemplate.opsForHash().get(BLACKLIST_STATS_KEY, "total_blacklisted");
            Long activeBlacklisted = (Long) redisTemplate.opsForHash().get(BLACKLIST_STATS_KEY, "active_blacklisted");

            // 统计当前活跃的黑名单条目
            Set<String> activeKeys = redisTemplate.keys(BLACKLIST_KEY_PREFIX + "*");
            int actualActive = activeKeys != null ? activeKeys.size() : 0;

            // 更新活跃数量（可能因为TTL过期而不准确）
            if (activeBlacklisted != null && actualActive != activeBlacklisted.intValue()) {
                redisTemplate.opsForHash().put(BLACKLIST_STATS_KEY, "active_blacklisted", actualActive);
            }

            return new BlacklistStats(
                    totalBlacklisted != null ? totalBlacklisted : 0L,
                    actualActive,
                    Instant.now()
            );
        } catch (Exception e) {
            log.error("获取黑名单统计信息失败", e);
            return new BlacklistStats(0L, 0, Instant.now());
        }
    }

    /**
     * 清理过期的黑名单条目（通常由Redis TTL自动处理）
     *
     * @return 清理的条目数量
     */
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
                log.info("清理过期黑名单条目: {} 个", cleanedCount);
            }

            return cleanedCount;
        } catch (Exception e) {
            log.error("清理过期黑名单条目失败", e);
            return 0;
        }
    }

    /**
     * 从JWT中提取令牌ID
     */
    private String extractTokenId(Jwt jwt) {
        // 优先使用jti声明
        String jti = jwt.getClaimAsString("jti");
        if (jti != null && !jti.trim().isEmpty()) {
            return jti;
        }

        // 如果没有jti，使用令牌值的哈希
        return generateTokenId(jwt.getTokenValue());
    }

    /**
     * 生成令牌ID（用于非JWT令牌或没有jti的JWT）
     */
    private String generateTokenId(String tokenValue) {
        // 使用令牌值的SHA-256哈希作为ID
        return String.valueOf(tokenValue.hashCode());
    }

    /**
     * 令牌黑名单信息
     */
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

    /**
     * 黑名单统计信息
     */
    public static class BlacklistStats {
        public final long totalBlacklisted;
        public final int activeBlacklisted;
        public final Instant lastUpdated;

        public BlacklistStats(long totalBlacklisted, int activeBlacklisted, Instant lastUpdated) {
            this.totalBlacklisted = totalBlacklisted;
            this.activeBlacklisted = activeBlacklisted;
            this.lastUpdated = lastUpdated;
        }
        
        /**
         * 获取总黑名单数量
         */
        public long totalCount() {
            return totalBlacklisted;
        }
        
        /**
         * 获取活跃黑名单数量
         */
        public int activeCount() {
            return activeBlacklisted;
        }
    }
}
