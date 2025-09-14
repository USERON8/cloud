package com.cloud.common.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于Redis的分布式限流管理器
 * 支持多种限流算法：固定窗口、滑动窗口、令牌桶、漏桶
 *
 * @author what's up
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitManager {

    private final RedisTemplate<String, Object> redisTemplate;

    // 限流规则缓存
    private final Map<String, RateLimitRule> rateLimitRules = new ConcurrentHashMap<>();

    // 限流统计
    private final Map<String, RateLimitStats> limitStats = new ConcurrentHashMap<>();

    /**
     * 检查限流 - 主入口
     *
     * @param key        限流键（通常是 IP + API 或 用户ID + API）
     * @param identifier 标识符（用户ID、IP等）
     * @return 限流结果
     */
    public RateLimitResult checkLimit(String key, String identifier) {
        RateLimitRule rule = rateLimitRules.get(key);
        if (rule == null) {
            // 没有限流规则，直接允许
            return RateLimitResult.allow(Long.MAX_VALUE, 0);
        }

        RateLimitStats stats = limitStats.computeIfAbsent(key + ":" + identifier, k -> new RateLimitStats());

        try {
            RateLimitResult result = switch (rule.getType()) {
                case FIXED_WINDOW -> checkFixedWindow(rule, identifier);
                case SLIDING_WINDOW -> checkSlidingWindow(rule, identifier);
                case TOKEN_BUCKET -> checkTokenBucket(rule, identifier);
                case LEAKY_BUCKET -> checkLeakyBucket(rule, identifier);
            };

            // 记录统计
            if (result.isAllowed()) {
                stats.recordAllow();
            } else {
                stats.recordReject();
            }

            return result;

        } catch (Exception e) {
            log.error("限流检查异常, key: {}, identifier: {}", key, identifier, e);
            // 异常时允许访问（fail-open策略）
            return RateLimitResult.allow(Long.MAX_VALUE, 0);
        }
    }

    /**
     * 固定窗口限流算法
     */
    private RateLimitResult checkFixedWindow(RateLimitRule rule, String identifier) {
        String redisKey = "rate_limit:fixed:" + rule.getKey() + ":" + identifier;
        long windowStart = Instant.now().getEpochSecond() / rule.getWindow().getSeconds() * rule.getWindow().getSeconds();
        String windowKey = redisKey + ":" + windowStart;

        // Lua脚本实现原子操作
        String luaScript = """
                local key = KEYS[1]
                local limit = tonumber(ARGV[1])
                local window = tonumber(ARGV[2])
                local current = redis.call('GET', key)
                
                if current == false then
                    redis.call('SET', key, 1)
                    redis.call('EXPIRE', key, window)
                    return {1, limit - 1, window}
                else
                    current = tonumber(current)
                    if current < limit then
                        redis.call('INCR', key)
                        return {1, limit - current - 1, redis.call('TTL', key)}
                    else
                        return {0, 0, redis.call('TTL', key)}
                    end
                end
                """;

        RedisScript<List> script = RedisScript.of(luaScript, List.class);
        List<Long> result = redisTemplate.execute(script,
                Collections.singletonList(windowKey),
                rule.getPermits(),
                rule.getWindow().getSeconds());

        boolean allowed = result.get(0) == 1;
        long remaining = result.get(1);
        long resetTime = result.get(2);

        if (allowed) {
            return RateLimitResult.allow(remaining, resetTime);
        } else {
            return RateLimitResult.reject(resetTime, "固定窗口限流：超出 " + rule.getPermits() + " 次/" + rule.getWindow().getSeconds() + "s 限制");
        }
    }

    /**
     * 滑动窗口限流算法
     */
    private RateLimitResult checkSlidingWindow(RateLimitRule rule, String identifier) {
        String redisKey = "rate_limit:sliding:" + rule.getKey() + ":" + identifier;
        long now = Instant.now().getEpochSecond();
        long windowStart = now - rule.getWindow().getSeconds();

        // 使用有序集合实现滑动窗口
        String luaScript = """
                local key = KEYS[1]
                local now = tonumber(ARGV[1])
                local window_start = tonumber(ARGV[2])
                local limit = tonumber(ARGV[3])
                local window_size = tonumber(ARGV[4])
                
                -- 清理过期数据
                redis.call('ZREMRANGEBYSCORE', key, 0, window_start)
                
                -- 获取当前窗口内的请求数
                local current_count = redis.call('ZCARD', key)
                
                if current_count < limit then
                    -- 添加当前请求
                    redis.call('ZADD', key, now, now .. ':' .. math.random())
                    redis.call('EXPIRE', key, window_size)
                    return {1, limit - current_count - 1, window_size}
                else
                    return {0, 0, window_size}
                end
                """;

        RedisScript<List> script = RedisScript.of(luaScript, List.class);
        List<Long> result = redisTemplate.execute(script,
                Collections.singletonList(redisKey),
                now,
                windowStart,
                rule.getPermits(),
                rule.getWindow().getSeconds());

        boolean allowed = result.get(0) == 1;
        long remaining = result.get(1);
        long resetTime = result.get(2);

        if (allowed) {
            return RateLimitResult.allow(remaining, resetTime);
        } else {
            return RateLimitResult.reject(resetTime, "滑动窗口限流：超出 " + rule.getPermits() + " 次/" + rule.getWindow().getSeconds() + "s 限制");
        }
    }

    /**
     * 令牌桶限流算法
     */
    private RateLimitResult checkTokenBucket(RateLimitRule rule, String identifier) {
        String redisKey = "rate_limit:token:" + rule.getKey() + ":" + identifier;
        long now = Instant.now().getEpochSecond();

        // 令牌桶参数
        int capacity = rule.getPermits();  // 桶容量
        double refillRate = (double) rule.getPermits() / rule.getWindow().getSeconds(); // 每秒补充速率

        String luaScript = """
                local key = KEYS[1]
                local capacity = tonumber(ARGV[1])
                local refill_rate = tonumber(ARGV[2])
                local now = tonumber(ARGV[3])
                
                local bucket = redis.call('HMGET', key, 'tokens', 'last_refill')
                local tokens = tonumber(bucket[1]) or capacity
                local last_refill = tonumber(bucket[2]) or now
                
                -- 计算需要补充的令牌数
                local time_passed = now - last_refill
                local tokens_to_add = time_passed * refill_rate
                tokens = math.min(capacity, tokens + tokens_to_add)
                
                if tokens >= 1 then
                    tokens = tokens - 1
                    redis.call('HMSET', key, 'tokens', tokens, 'last_refill', now)
                    redis.call('EXPIRE', key, 3600)  -- 1小时过期
                    return {1, math.floor(tokens), 0}
                else
                    redis.call('HMSET', key, 'tokens', tokens, 'last_refill', now)
                    redis.call('EXPIRE', key, 3600)
                    return {0, 0, math.ceil((1 - tokens) / refill_rate)}
                end
                """;

        RedisScript<List> script = RedisScript.of(luaScript, List.class);
        List<Long> result = redisTemplate.execute(script,
                Collections.singletonList(redisKey),
                capacity,
                refillRate,
                now);

        boolean allowed = result.get(0) == 1;
        long remaining = result.get(1);
        long resetTime = result.get(2);

        if (allowed) {
            return RateLimitResult.allow(remaining, resetTime);
        } else {
            return RateLimitResult.reject(resetTime, "令牌桶限流：令牌不足，请等待 " + resetTime + " 秒后重试");
        }
    }

    /**
     * 漏桶限流算法
     */
    private RateLimitResult checkLeakyBucket(RateLimitRule rule, String identifier) {
        String redisKey = "rate_limit:leaky:" + rule.getKey() + ":" + identifier;
        long now = Instant.now().getEpochSecond();

        // 漏桶参数
        int capacity = rule.getPermits();  // 桶容量
        double leakRate = (double) rule.getPermits() / rule.getWindow().getSeconds(); // 每秒漏出速率

        String luaScript = """
                local key = KEYS[1]
                local capacity = tonumber(ARGV[1])
                local leak_rate = tonumber(ARGV[2])
                local now = tonumber(ARGV[3])
                
                local bucket = redis.call('HMGET', key, 'volume', 'last_leak')
                local volume = tonumber(bucket[1]) or 0
                local last_leak = tonumber(bucket[2]) or now
                
                -- 计算漏出的水量
                local time_passed = now - last_leak
                local leaked = time_passed * leak_rate
                volume = math.max(0, volume - leaked)
                
                if volume < capacity then
                    volume = volume + 1
                    redis.call('HMSET', key, 'volume', volume, 'last_leak', now)
                    redis.call('EXPIRE', key, 3600)
                    return {1, capacity - volume, 0}
                else
                    redis.call('HMSET', key, 'volume', volume, 'last_leak', now)
                    redis.call('EXPIRE', key, 3600)
                    return {0, 0, math.ceil((volume - capacity + 1) / leak_rate)}
                end
                """;

        RedisScript<List> script = RedisScript.of(luaScript, List.class);
        List<Long> result = redisTemplate.execute(script,
                Collections.singletonList(redisKey),
                capacity,
                leakRate,
                now);

        boolean allowed = result.get(0) == 1;
        long remaining = result.get(1);
        long resetTime = result.get(2);

        if (allowed) {
            return RateLimitResult.allow(remaining, resetTime);
        } else {
            return RateLimitResult.reject(resetTime, "漏桶限流：桶已满，请等待 " + resetTime + " 秒后重试");
        }
    }

    /**
     * 注册限流规则
     *
     * @param key         限流键
     * @param permits     允许的请求数
     * @param window      时间窗口
     * @param type        限流类型
     * @param description 规则描述
     */
    public void registerRule(String key, int permits, Duration window, RateLimitType type, String description) {
        RateLimitRule rule = new RateLimitRule(key, permits, window, type, description);
        rateLimitRules.put(key, rule);
        log.info("注册限流规则: key={}, permits={}, window={}, type={}, description={}",
                key, permits, window, type, description);
    }

    /**
     * 移除限流规则
     *
     * @param key 限流键
     */
    public void removeRule(String key) {
        rateLimitRules.remove(key);
        log.info("移除限流规则: key={}", key);
    }

    /**
     * 获取限流统计信息
     *
     * @return 限流统计信息
     */
    public Map<String, RateLimitStats> getRateLimitStats() {
        return new HashMap<>(limitStats);
    }

    /**
     * 获取限流规则
     *
     * @return 限流规则
     */
    public Map<String, RateLimitRule> getRateLimitRules() {
        return new HashMap<>(rateLimitRules);
    }

    /**
     * 清理过期统计数据
     */
    public void cleanupExpiredStats() {
        Instant cutoffTime = Instant.now().minus(Duration.ofHours(24));

        limitStats.entrySet().removeIf(entry -> {
            Instant lastRequest = entry.getValue().getLastRequestTime();
            return lastRequest != null && lastRequest.isBefore(cutoffTime);
        });

        log.info("清理过期限流统计数据完成");
    }

    /**
     * 预置常用限流规则
     */
    public void initDefaultRules() {
        // API访问限流 - 每分钟100次
        registerRule("api:access", 100, Duration.ofMinutes(1), RateLimitType.SLIDING_WINDOW, "API接口访问限流");

        // API测试访问限流 - 每分钟200次，更宽松的限制
        registerRule("api:test", 200, Duration.ofMinutes(1), RateLimitType.SLIDING_WINDOW, "API测试接口访问限流");

        // 登录限流 - 每分钟5次
        registerRule("auth:login", 5, Duration.ofMinutes(1), RateLimitType.FIXED_WINDOW, "登录接口限流");

        // 注册限流 - 每小时3次
        registerRule("auth:register", 3, Duration.ofHours(1), RateLimitType.FIXED_WINDOW, "注册接口限流");

        // 短信发送限流 - 每分钟1次
        registerRule("sms:send", 1, Duration.ofMinutes(1), RateLimitType.LEAKY_BUCKET, "短信发送限流");

        // 文件上传限流 - 每小时10次
        registerRule("file:upload", 10, Duration.ofHours(1), RateLimitType.TOKEN_BUCKET, "文件上传限流");

        log.info("初始化默认限流规则完成");
    }

    /**
     * 限流类型枚举
     */
    public enum RateLimitType {
        FIXED_WINDOW,    // 固定窗口
        SLIDING_WINDOW,  // 滑动窗口
        TOKEN_BUCKET,    // 令牌桶
        LEAKY_BUCKET     // 漏桶
    }

    /**
     * 限流规则
     */
    public static class RateLimitRule {
        private final String key;
        private final int permits;           // 允许的请求数
        private final Duration window;       // 时间窗口
        private final RateLimitType type;    // 限流类型
        private final String description;    // 规则描述

        public RateLimitRule(String key, int permits, Duration window, RateLimitType type, String description) {
            this.key = key;
            this.permits = permits;
            this.window = window;
            this.type = type;
            this.description = description;
        }

        // Getters
        public String getKey() {
            return key;
        }

        public int getPermits() {
            return permits;
        }

        public Duration getWindow() {
            return window;
        }

        public RateLimitType getType() {
            return type;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 限流统计信息
     */
    public static class RateLimitStats {
        private long totalRequests;      // 总请求数
        private long allowedRequests;    // 允许的请求数
        private long rejectedRequests;   // 拒绝的请求数
        private Instant lastRequestTime; // 最后请求时间

        public void recordAllow() {
            totalRequests++;
            allowedRequests++;
            lastRequestTime = Instant.now();
        }

        public void recordReject() {
            totalRequests++;
            rejectedRequests++;
            lastRequestTime = Instant.now();
        }

        public double getRejectRate() {
            return totalRequests > 0 ? (double) rejectedRequests / totalRequests : 0.0;
        }

        // Getters
        public long getTotalRequests() {
            return totalRequests;
        }

        public long getAllowedRequests() {
            return allowedRequests;
        }

        public long getRejectedRequests() {
            return rejectedRequests;
        }

        public Instant getLastRequestTime() {
            return lastRequestTime;
        }
    }

    /**
     * 限流结果
     */
    public static class RateLimitResult {
        private final boolean allowed;
        private final long remaining;
        private final long resetTime;
        private final String reason;

        public RateLimitResult(boolean allowed, long remaining, long resetTime, String reason) {
            this.allowed = allowed;
            this.remaining = remaining;
            this.resetTime = resetTime;
            this.reason = reason;
        }

        public static RateLimitResult allow(long remaining, long resetTime) {
            return new RateLimitResult(true, remaining, resetTime, "允许访问");
        }

        public static RateLimitResult reject(long resetTime, String reason) {
            return new RateLimitResult(false, 0, resetTime, reason);
        }

        // Getters
        public boolean isAllowed() {
            return allowed;
        }

        public long getRemaining() {
            return remaining;
        }

        public long getResetTime() {
            return resetTime;
        }

        public String getReason() {
            return reason;
        }
    }
}
