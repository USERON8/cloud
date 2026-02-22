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







@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "rawtypes"}) 
public class RateLimitManager {

    
    private static final String RULE_PREFIX = "rate_limit:rules:";
    private static final String STATS_PREFIX = "rate_limit:stats:";
    private final RedisTemplate<String, Object> redisTemplate;
    
    private final Map<String, RateLimitRule> rateLimitRules = new HashMap<>();

    
    private final Map<String, RateLimitStats> limitStats = new HashMap<>();

    






    public RateLimitResult checkLimit(String key, String identifier) {
        RateLimitRule rule = rateLimitRules.get(key);
        if (rule == null) {
            
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

            
            if (result.isAllowed()) {
                stats.recordAllow();
            } else {
                stats.recordReject();
            }

            return result;

        } catch (Exception e) {
            log.error("闄愭祦妫€鏌ュ紓甯? key: {}, identifier: {}", key, identifier, e);
            
            return RateLimitResult.allow(Long.MAX_VALUE, 0);
        }
    }

    


    private RateLimitResult checkFixedWindow(RateLimitRule rule, String identifier) {
        String redisKey = "rate_limit:fixed:" + rule.getKey() + ":" + identifier;
        long windowStart = Instant.now().getEpochSecond() / rule.getWindow().getSeconds() * rule.getWindow().getSeconds();
        String windowKey = redisKey + ":" + windowStart;

        
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
        List result = redisTemplate.execute(script,
                Collections.singletonList(windowKey),
                rule.getPermits(),
                rule.getWindow().getSeconds());

        
        boolean allowed = result != null && result.size() > 0 && ((Number) result.get(0)).longValue() == 1;
        long remaining = result != null && result.size() > 1 ? ((Number) result.get(1)).longValue() : 0;
        long resetTime = result != null && result.size() > 2 ? ((Number) result.get(2)).longValue() : 0;

        if (allowed) {
            return RateLimitResult.allow(remaining, resetTime);
        } else {
            return RateLimitResult.reject(resetTime, "鍥哄畾绐楀彛闄愭祦锛氳秴鍑?" + rule.getPermits() + " 娆?" + rule.getWindow().getSeconds() + "s 闄愬埗");
        }
    }

    


    private RateLimitResult checkSlidingWindow(RateLimitRule rule, String identifier) {
        String redisKey = "rate_limit:sliding:" + rule.getKey() + ":" + identifier;
        long now = Instant.now().getEpochSecond();
        long windowStart = now - rule.getWindow().getSeconds();

        
        String luaScript = """
                local key = KEYS[1]
                local now = tonumber(ARGV[1])
                local window_start = tonumber(ARGV[2])
                local limit = tonumber(ARGV[3])
                local window_size = tonumber(ARGV[4])
                
                -- ASCII comment sanitized for compatibility.
                redis.call('ZREMRANGEBYSCORE', key, 0, window_start)
                
                -- ASCII comment sanitized for compatibility.
                local current_count = redis.call('ZCARD', key)
                
                if current_count < limit then
                    -- ASCII comment sanitized for compatibility.
                    redis.call('ZADD', key, now, now .. ':' .. math.random())
                    redis.call('EXPIRE', key, window_size)
                    return {1, limit - current_count - 1, window_size}
                else
                    return {0, 0, window_size}
                end
                """;

        RedisScript<List> script = RedisScript.of(luaScript, List.class);
        List result = redisTemplate.execute(script,
                Collections.singletonList(redisKey),
                now,
                windowStart,
                rule.getPermits(),
                rule.getWindow().getSeconds());

        
        boolean allowed = result != null && result.size() > 0 && ((Number) result.get(0)).longValue() == 1;
        long remaining = result != null && result.size() > 1 ? ((Number) result.get(1)).longValue() : 0;
        long resetTime = result != null && result.size() > 2 ? ((Number) result.get(2)).longValue() : 0;

        if (allowed) {
            return RateLimitResult.allow(remaining, resetTime);
        } else {
            return RateLimitResult.reject(resetTime, "婊戝姩绐楀彛闄愭祦锛氳秴鍑?" + rule.getPermits() + " 娆?" + rule.getWindow().getSeconds() + "s 闄愬埗");
        }
    }

    


    private RateLimitResult checkTokenBucket(RateLimitRule rule, String identifier) {
        String redisKey = "rate_limit:token:" + rule.getKey() + ":" + identifier;
        long now = Instant.now().getEpochSecond();

        
        int capacity = rule.getPermits();  
        double refillRate = (double) rule.getPermits() / rule.getWindow().getSeconds(); 

        String luaScript = """
                local key = KEYS[1]
                local capacity = tonumber(ARGV[1])
                local refill_rate = tonumber(ARGV[2])
                local now = tonumber(ARGV[3])
                
                local bucket = redis.call('HMGET', key, 'tokens', 'last_refill')
                local tokens = tonumber(bucket[1]) or capacity
                local last_refill = tonumber(bucket[2]) or now
                
                -- ASCII comment sanitized for compatibility.
                local time_passed = now - last_refill
                local tokens_to_add = time_passed * refill_rate
                tokens = math.min(capacity, tokens + tokens_to_add)
                
                if tokens >= 1 then
                    tokens = tokens - 1
                    redis.call('HMSET', key, 'tokens', tokens, 'last_refill', now)
                    redis.call('EXPIRE', key, 3600)  -- 1灏忔椂杩囨湡
                    return {1, math.floor(tokens), 0}
                else
                    redis.call('HMSET', key, 'tokens', tokens, 'last_refill', now)
                    redis.call('EXPIRE', key, 3600)
                    return {0, 0, math.ceil((1 - tokens) / refill_rate)}
                end
                """;

        RedisScript<List> script = RedisScript.of(luaScript, List.class);
        List result = redisTemplate.execute(script,
                Collections.singletonList(redisKey),
                capacity,
                refillRate,
                now);

        
        boolean allowed = result != null && result.size() > 0 && ((Number) result.get(0)).longValue() == 1;
        long remaining = result != null && result.size() > 1 ? ((Number) result.get(1)).longValue() : 0;
        long resetTime = result != null && result.size() > 2 ? ((Number) result.get(2)).longValue() : 0;

        if (allowed) {
            return RateLimitResult.allow(remaining, resetTime);
        } else {
            return RateLimitResult.reject(resetTime, "浠ょ墝妗堕檺娴侊細浠ょ墝涓嶈冻锛岃绛夊緟 " + resetTime + " 绉掑悗閲嶈瘯");
        }
    }

    


    private RateLimitResult checkLeakyBucket(RateLimitRule rule, String identifier) {
        String redisKey = "rate_limit:leaky:" + rule.getKey() + ":" + identifier;
        long now = Instant.now().getEpochSecond();

        
        int capacity = rule.getPermits();  
        double leakRate = (double) rule.getPermits() / rule.getWindow().getSeconds(); 

        String luaScript = """
                local key = KEYS[1]
                local capacity = tonumber(ARGV[1])
                local leak_rate = tonumber(ARGV[2])
                local now = tonumber(ARGV[3])
                
                local bucket = redis.call('HMGET', key, 'volume', 'last_leak')
                local volume = tonumber(bucket[1]) or 0
                local last_leak = tonumber(bucket[2]) or now
                
                -- ASCII comment sanitized for compatibility.
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
        List result = redisTemplate.execute(script,
                Collections.singletonList(redisKey),
                capacity,
                leakRate,
                now);

        
        boolean allowed = result != null && result.size() > 0 && ((Number) result.get(0)).longValue() == 1;
        long remaining = result != null && result.size() > 1 ? ((Number) result.get(1)).longValue() : 0;
        long resetTime = result != null && result.size() > 2 ? ((Number) result.get(2)).longValue() : 0;

        if (allowed) {
            return RateLimitResult.allow(remaining, resetTime);
        } else {
            return RateLimitResult.reject(resetTime, "婕忔《闄愭祦锛氭《宸叉弧锛岃绛夊緟 " + resetTime + " 绉掑悗閲嶈瘯");
        }
    }

    








    public void registerRule(String key, int permits, Duration window, RateLimitType type, String description) {
        RateLimitRule rule = new RateLimitRule(key, permits, window, type, description);
        rateLimitRules.put(key, rule);
        

    }

    




    public void removeRule(String key) {
        rateLimitRules.remove(key);
        
    }

    




    public Map<String, RateLimitStats> getRateLimitStats() {
        return new HashMap<>(limitStats);
    }

    




    public Map<String, RateLimitRule> getRateLimitRules() {
        return new HashMap<>(rateLimitRules);
    }

    


    public void cleanupExpiredStats() {
        Instant cutoffTime = Instant.now().minus(Duration.ofHours(24));

        limitStats.entrySet().removeIf(entry -> {
            Instant lastRequest = entry.getValue().getLastRequestTime();
            return lastRequest != null && lastRequest.isBefore(cutoffTime);
        });

        
    }

    


    public void initDefaultRules() {
        
        registerRule("api:access", 100, Duration.ofMinutes(1), RateLimitType.SLIDING_WINDOW, "API鎺ュ彛璁块棶闄愭祦");

        
        registerRule("api:test", 200, Duration.ofMinutes(1), RateLimitType.SLIDING_WINDOW, "API娴嬭瘯鎺ュ彛璁块棶闄愭祦");

        
        registerRule("auth:login", 5, Duration.ofMinutes(1), RateLimitType.FIXED_WINDOW, "鐧诲綍鎺ュ彛闄愭祦");

        
        registerRule("auth:register", 3, Duration.ofHours(1), RateLimitType.FIXED_WINDOW, "娉ㄥ唽鎺ュ彛闄愭祦");

        
        registerRule("sms:send", 1, Duration.ofMinutes(1), RateLimitType.LEAKY_BUCKET, "SMS send rate limit");

        
        registerRule("file:upload", 10, Duration.ofHours(1), RateLimitType.TOKEN_BUCKET, "鏂囦欢涓婁紶闄愭祦");

    }

    


    public enum RateLimitType {
        FIXED_WINDOW,    
        SLIDING_WINDOW,  
        TOKEN_BUCKET,    
        LEAKY_BUCKET     
    }

    


    public static class RateLimitRule {
        private final String key;
        private final int permits;           
        private final Duration window;       
        private final RateLimitType type;    
        private final String description;    

        public RateLimitRule(String key, int permits, Duration window, RateLimitType type, String description) {
            this.key = key;
            this.permits = permits;
            this.window = window;
            this.type = type;
            this.description = description;
        }

        
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

    


    public static class RateLimitStats {
        private long totalRequests;      
        private long allowedRequests;    
        private long rejectedRequests;   
        private Instant lastRequestTime; 

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
            return new RateLimitResult(true, remaining, resetTime, "鍏佽璁块棶");
        }

        public static RateLimitResult reject(long resetTime, String reason) {
            return new RateLimitResult(false, 0, resetTime, reason);
        }

        
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
