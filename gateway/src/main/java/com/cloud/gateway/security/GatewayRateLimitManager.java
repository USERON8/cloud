package com.cloud.gateway.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 网关限流管理器
 * 基于Redis实现的分布式限流功能
 * 
 * @author what's up
 */
@Slf4j
@Component
public class GatewayRateLimitManager {

    private final ReactiveStringRedisTemplate redisTemplate;
    
    // 限流配置
    @Value("${gateway.ratelimit.default.permits:100}")
    private int defaultPermits;
    
    @Value("${gateway.ratelimit.default.window:60}")
    private int defaultWindowSeconds;
    
    // 不同API的限流配置
    private static final Map<String, RateLimitConfig> RATE_LIMIT_CONFIGS = Map.of(
            "auth:login", new RateLimitConfig(10, 60),      // 登录接口：60秒内最多10次
            "auth:register", new RateLimitConfig(5, 300),   // 注册接口：5分钟内最多5次
            "file:upload", new RateLimitConfig(20, 60),     // 文件上传：60秒内最多20次
            "api:test", new RateLimitConfig(50, 60),        // 测试接口：60秒内最多50次
            "api:access", new RateLimitConfig(200, 60)      // 普通API：60秒内最多200次
    );
    
    // Redis键前缀
    private static final String RATE_LIMIT_KEY_PREFIX = "gateway:ratelimit:";
    
    // 本地缓存（减少Redis访问）
    private final ConcurrentHashMap<String, LocalRateLimit> localLimitCache = new ConcurrentHashMap<>();
    
    // Lua脚本实现原子性限流检查
    private static final String RATE_LIMIT_SCRIPT = """
            local key = KEYS[1]
            local window = tonumber(ARGV[1])
            local limit = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])
            
            -- 清理过期的记录
            redis.call('ZREMRANGEBYSCORE', key, 0, now - window * 1000)
            
            -- 获取当前窗口内的请求数量
            local current = redis.call('ZCARD', key)
            
            if current < limit then
                -- 添加当前请求记录
                redis.call('ZADD', key, now, now)
                redis.call('EXPIRE', key, window + 10)
                return {1, limit - current - 1, now + window * 1000}
            else
                -- 获取最早的请求时间，计算重置时间
                local earliest = redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')[2]
                local resetTime = earliest and (earliest + window * 1000) or (now + window * 1000)
                return {0, 0, resetTime}
            end
            """;
    
    private final RedisScript<List> rateLimitScript = RedisScript.of(RATE_LIMIT_SCRIPT, List.class);
    
    public GatewayRateLimitManager(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * 检查限流
     */
    public Mono<RateLimitResult> checkLimit(String rateLimitKey, String clientIdentifier) {
        RateLimitConfig config = RATE_LIMIT_CONFIGS.getOrDefault(rateLimitKey, 
                new RateLimitConfig(defaultPermits, defaultWindowSeconds));
        
        // 构建Redis键
        String redisKey = RATE_LIMIT_KEY_PREFIX + rateLimitKey + ":" + clientIdentifier;
        
        // 先检查本地缓存（快速拒绝）
        LocalRateLimit localLimit = localLimitCache.get(redisKey);
        if (localLimit != null && localLimit.isBlocked()) {
            log.debug("本地缓存限流: key={}, clientId={}", rateLimitKey, clientIdentifier);
            return Mono.just(new RateLimitResult(false, 0, localLimit.resetTime, "本地缓存限流"));
        }
        
        // 执行Redis限流检查
        long now = Instant.now().toEpochMilli();
        
        return redisTemplate.execute(rateLimitScript, 
                List.of(redisKey),
                String.valueOf(config.windowSeconds),
                String.valueOf(config.permits),
                String.valueOf(now))
                .cast(List.class)
                .map(result -> {
                    boolean allowed = ((Number) result.get(0)).intValue() == 1;
                    int remaining = ((Number) result.get(1)).intValue();
                    long resetTime = ((Number) result.get(2)).longValue();
                    
                    if (!allowed) {
                        // 更新本地缓存，避免频繁访问Redis
                        localLimitCache.put(redisKey, new LocalRateLimit(resetTime));
                        log.warn("限流触发: key={}, clientId={}, resetTime={}", 
                                rateLimitKey, clientIdentifier, Instant.ofEpochMilli(resetTime));
                    }
                    
                    return new RateLimitResult(allowed, remaining, resetTime, 
                            allowed ? "限流检查通过" : "超过限流阈值");
                })
                .doOnError(error -> {
                    log.error("限流检查异常: key={}, clientId={}", rateLimitKey, clientIdentifier, error);
                })
                .onErrorReturn(new RateLimitResult(true, config.permits - 1, 
                        now + config.windowSeconds * 1000L, "限流检查异常，默认放行"))
                .single();
    }
    
    /**
     * 获取限流统计信息
     */
    public Mono<RateLimitStats> getLimitStats(String rateLimitKey, String clientIdentifier) {
        RateLimitConfig config = RATE_LIMIT_CONFIGS.getOrDefault(rateLimitKey, 
                new RateLimitConfig(defaultPermits, defaultWindowSeconds));
        
        String redisKey = RATE_LIMIT_KEY_PREFIX + rateLimitKey + ":" + clientIdentifier;
        long now = Instant.now().toEpochMilli();
        long windowStart = now - config.windowSeconds * 1000L;
        
        return redisTemplate.opsForZSet()
                .count(redisKey, org.springframework.data.domain.Range.closed((double)windowStart, (double)now))
                .map(count -> new RateLimitStats(
                        rateLimitKey,
                        clientIdentifier,
                        count.intValue(),
                        config.permits,
                        config.windowSeconds,
                        now + config.windowSeconds * 1000L
                ))
                .onErrorReturn(new RateLimitStats(rateLimitKey, clientIdentifier, 0, 
                        config.permits, config.windowSeconds, now));
    }
    
    /**
     * 清理过期的限流记录
     */
    public Mono<Void> cleanupExpiredRecords() {
        // 清理本地缓存
        long now = Instant.now().toEpochMilli();
        localLimitCache.entrySet().removeIf(entry -> entry.getValue().resetTime <= now);
        
        log.debug("清理限流缓存，本地缓存大小: {}", localLimitCache.size());
        
        return Mono.empty();
    }
    
    /**
     * 手动重置限流
     */
    public Mono<Void> resetLimit(String rateLimitKey, String clientIdentifier) {
        String redisKey = RATE_LIMIT_KEY_PREFIX + rateLimitKey + ":" + clientIdentifier;
        localLimitCache.remove(redisKey);
        
        return redisTemplate.delete(redisKey).then()
                .doOnSuccess(unused -> 
                        log.info("重置限流: key={}, clientId={}", rateLimitKey, clientIdentifier));
    }
    
    /**
     * 限流配置
     */
    private static class RateLimitConfig {
        final int permits;
        final int windowSeconds;
        
        public RateLimitConfig(int permits, int windowSeconds) {
            this.permits = permits;
            this.windowSeconds = windowSeconds;
        }
    }
    
    /**
     * 本地限流缓存
     */
    private static class LocalRateLimit {
        final long resetTime;
        
        public LocalRateLimit(long resetTime) {
            this.resetTime = resetTime;
        }
        
        public boolean isBlocked() {
            return Instant.now().toEpochMilli() < resetTime;
        }
    }
    
    /**
     * 限流检查结果
     */
    public static class RateLimitResult {
        private final boolean allowed;
        private final int remaining;
        private final long resetTime;
        private final String reason;
        
        public RateLimitResult(boolean allowed, int remaining, long resetTime, String reason) {
            this.allowed = allowed;
            this.remaining = remaining;
            this.resetTime = resetTime;
            this.reason = reason;
        }
        
        public boolean isAllowed() {
            return allowed;
        }
        
        public int getRemaining() {
            return remaining;
        }
        
        public long getResetTime() {
            return resetTime;
        }
        
        public String getReason() {
            return reason;
        }
        
        @Override
        public String toString() {
            return "RateLimitResult{allowed=" + allowed + 
                   ", remaining=" + remaining + 
                   ", resetTime=" + resetTime + 
                   ", reason='" + reason + "'}";
        }
    }
    
    /**
     * 限流统计信息
     */
    public static class RateLimitStats {
        private final String key;
        private final String clientId;
        private final int currentCount;
        private final int limitCount;
        private final int windowSeconds;
        private final long nextResetTime;
        
        public RateLimitStats(String key, String clientId, int currentCount, 
                             int limitCount, int windowSeconds, long nextResetTime) {
            this.key = key;
            this.clientId = clientId;
            this.currentCount = currentCount;
            this.limitCount = limitCount;
            this.windowSeconds = windowSeconds;
            this.nextResetTime = nextResetTime;
        }
        
        public String getKey() { return key; }
        public String getClientId() { return clientId; }
        public int getCurrentCount() { return currentCount; }
        public int getLimitCount() { return limitCount; }
        public int getWindowSeconds() { return windowSeconds; }
        public long getNextResetTime() { return nextResetTime; }
    }
}
