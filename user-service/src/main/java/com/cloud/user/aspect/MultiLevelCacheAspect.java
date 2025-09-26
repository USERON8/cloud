package com.cloud.user.aspect;

import com.cloud.common.cache.HybridCacheManager;
import com.cloud.user.annotation.MultiLevelCacheEvict;
import com.cloud.user.annotation.MultiLevelCachePut;
import com.cloud.user.annotation.MultiLevelCacheable;
import com.cloud.user.annotation.MultiLevelCaching;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 多级缓存AOP切面
 * 拦截@MultiLevelCacheable、@MultiLevelCachePut、@MultiLevelCacheEvict注解
 * 实现L1(本地缓存) + L2(Redis缓存)的两级缓存逻辑
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class MultiLevelCacheAspect {

    private final CacheManager localCacheManager;
    private final RedisTemplate<String, Object> redisTemplate;
    private final HybridCacheManager hybridCacheManager;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    /**
     * 拦截@MultiLevelCacheable注解的方法
     */
    @Around("@annotation(multiLevelCacheable)")
    public Object handleCacheable(ProceedingJoinPoint joinPoint, MultiLevelCacheable multiLevelCacheable) throws Throwable {
        Method method = getMethod(joinPoint);
        String cacheKey = generateKey(method, joinPoint.getArgs(), multiLevelCacheable.key());
        String cacheName = multiLevelCacheable.cacheName();

        // 检查条件
        if (!evaluateCondition(method, joinPoint.getArgs(), multiLevelCacheable.condition())) {
            return joinPoint.proceed();
        }

        // 1. 从L1缓存获取
        Object result = null;
        if (multiLevelCacheable.enableLocalCache()) {
            result = getFromLocalCache(cacheName, cacheKey);
            if (result != null) {
                log.debug("从L1缓存获取数据, cacheName={}, key={}", cacheName, cacheKey);
                return result;
            }
        }

        // 2. 从L2缓存获取
        result = getFromRedisCache(cacheName, cacheKey, method.getReturnType());
        if (result != null) {
            log.debug("从L2缓存获取数据, cacheName={}, key={}", cacheName, cacheKey);
            // 回种到L1缓存
            if (multiLevelCacheable.enableLocalCache()) {
                putToLocalCache(cacheName, cacheKey, result);
            }
            return result;
        }

        // 3. 执行原方法
        log.debug("从数据源加载数据, cacheName={}, key={}", cacheName, cacheKey);
        result = joinPoint.proceed();

        // 4. 检查unless条件，决定是否缓存结果
        if (result != null && !evaluateCondition(method, joinPoint.getArgs(), multiLevelCacheable.unless())) {
            // 存储到缓存
            if (multiLevelCacheable.enableLocalCache()) {
                putToLocalCache(cacheName, cacheKey, result);
            }
            putToRedisCache(cacheName, cacheKey, result, multiLevelCacheable.expire(), multiLevelCacheable.timeUnit());
        }

        return result;
    }

    /**
     * 拦截@MultiLevelCachePut注解的方法
     */
    @Around("@annotation(multiLevelCachePut)")
    public Object handleCachePut(ProceedingJoinPoint joinPoint, MultiLevelCachePut multiLevelCachePut) throws Throwable {
        Method method = getMethod(joinPoint);
        String cacheKey = generateKey(method, joinPoint.getArgs(), multiLevelCachePut.key());
        String cacheName = multiLevelCachePut.cacheName();

        // 总是执行原方法
        Object result = joinPoint.proceed();

        // 检查条件
        if (evaluateCondition(method, joinPoint.getArgs(), multiLevelCachePut.condition()) &&
                !evaluateCondition(method, joinPoint.getArgs(), multiLevelCachePut.unless()) &&
                result != null) {

            // 更新缓存
            if (multiLevelCachePut.enableLocalCache()) {
                putToLocalCache(cacheName, cacheKey, result);
            }
            putToRedisCache(cacheName, cacheKey, result, multiLevelCachePut.expire(), multiLevelCachePut.timeUnit());

            log.debug("更新多级缓存, cacheName={}, key={}", cacheName, cacheKey);
        }

        return result;
    }

    /**
     * 拦截@MultiLevelCacheEvict注解的方法
     */
    @Around("@annotation(multiLevelCacheEvict)")
    public Object handleCacheEvict(ProceedingJoinPoint joinPoint, MultiLevelCacheEvict multiLevelCacheEvict) throws Throwable {
        Method method = getMethod(joinPoint);
        String cacheKey = generateKey(method, joinPoint.getArgs(), multiLevelCacheEvict.key());
        String cacheName = multiLevelCacheEvict.cacheName();

        // 检查条件
        boolean shouldEvict = evaluateCondition(method, joinPoint.getArgs(), multiLevelCacheEvict.condition());

        // 方法执行前删除缓存
        if (shouldEvict && multiLevelCacheEvict.beforeInvocation()) {
            evictCache(cacheName, cacheKey, multiLevelCacheEvict.allEntries());
        }

        Object result = joinPoint.proceed();

        // 方法执行后删除缓存
        if (shouldEvict && !multiLevelCacheEvict.beforeInvocation()) {
            evictCache(cacheName, cacheKey, multiLevelCacheEvict.allEntries());
        }

        return result;
    }

    /**
     * 拦截@MultiLevelCaching组合注解的方法
     */
    @Around("@annotation(multiLevelCaching)")
    public Object handleCaching(ProceedingJoinPoint joinPoint, MultiLevelCaching multiLevelCaching) throws Throwable {
        Method method = getMethod(joinPoint);
        Object[] args = joinPoint.getArgs();

        // 处理evict操作（beforeInvocation=true的）
        for (MultiLevelCacheEvict evict : multiLevelCaching.evict()) {
            if (evict.beforeInvocation() && evaluateCondition(method, args, evict.condition())) {
                String cacheKey = generateKey(method, args, evict.key());
                evictCache(evict.cacheName(), cacheKey, evict.allEntries());
            }
        }

        // 检查cacheable操作
        for (MultiLevelCacheable cacheable : multiLevelCaching.cacheable()) {
            if (evaluateCondition(method, args, cacheable.condition())) {
                String cacheKey = generateKey(method, args, cacheable.key());
                String cacheName = cacheable.cacheName();

                // 尝试从缓存获取
                Object result = null;
                if (cacheable.enableLocalCache()) {
                    result = getFromLocalCache(cacheName, cacheKey);
                    if (result != null) {
                        log.debug("从L1缓存获取数据, cacheName={}, key={}", cacheName, cacheKey);
                        return result;
                    }
                }

                result = getFromRedisCache(cacheName, cacheKey, method.getReturnType());
                if (result != null) {
                    log.debug("从L2缓存获取数据, cacheName={}, key={}", cacheName, cacheKey);
                    if (cacheable.enableLocalCache()) {
                        putToLocalCache(cacheName, cacheKey, result);
                    }
                    return result;
                }
            }
        }

        // 执行原方法
        Object result = joinPoint.proceed();

        // 处理put操作
        for (MultiLevelCachePut put : multiLevelCaching.put()) {
            if (evaluateCondition(method, args, put.condition()) &&
                    !evaluateCondition(method, args, put.unless()) &&
                    result != null) {
                String cacheKey = generateKey(method, args, put.key());
                if (put.enableLocalCache()) {
                    putToLocalCache(put.cacheName(), cacheKey, result);
                }
                putToRedisCache(put.cacheName(), cacheKey, result, put.expire(), put.timeUnit());
            }
        }

        // 处理cacheable的缓存存储
        for (MultiLevelCacheable cacheable : multiLevelCaching.cacheable()) {
            if (evaluateCondition(method, args, cacheable.condition()) &&
                    !evaluateCondition(method, args, cacheable.unless()) &&
                    result != null) {
                String cacheKey = generateKey(method, args, cacheable.key());
                if (cacheable.enableLocalCache()) {
                    putToLocalCache(cacheable.cacheName(), cacheKey, result);
                }
                putToRedisCache(cacheable.cacheName(), cacheKey, result, cacheable.expire(), cacheable.timeUnit());
            }
        }

        // 处理evict操作（beforeInvocation=false的）
        for (MultiLevelCacheEvict evict : multiLevelCaching.evict()) {
            if (!evict.beforeInvocation() && evaluateCondition(method, args, evict.condition())) {
                String cacheKey = generateKey(method, args, evict.key());
                evictCache(evict.cacheName(), cacheKey, evict.allEntries());
            }
        }

        return result;
    }

    /**
     * 从本地缓存获取数据
     */
    private Object getFromLocalCache(String cacheName, String key) {
        Cache cache = localCacheManager.getCache(cacheName);
        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(key);
            return wrapper != null ? wrapper.get() : null;
        }
        return null;
    }

    /**
     * 存储到本地缓存
     */
    private void putToLocalCache(String cacheName, String key, Object value) {
        Cache cache = localCacheManager.getCache(cacheName);
        if (cache != null) {
            cache.put(key, value);
        }
    }

    /**
     * 从Redis缓存获取数据
     */
    private Object getFromRedisCache(String cacheName, String key, Class<?> returnType) {
        String redisKey = buildRedisKey(cacheName, key);
        try {
            return hybridCacheManager.smartGet(redisKey, (Class) returnType);
        } catch (Exception e) {
            // 兼容回退
            return redisTemplate.opsForValue().get(redisKey);
        }
    }

    /**
     * 存储到Redis缓存
     */
    private void putToRedisCache(String cacheName, String key, Object value, long expire, TimeUnit timeUnit) {
        String redisKey = buildRedisKey(cacheName, key);
        try {
            hybridCacheManager.smartSet(redisKey, value, expire, timeUnit);
        } catch (Exception e) {
            // 兼容回退
            redisTemplate.opsForValue().set(redisKey, value, expire, timeUnit);
        }
    }

    /**
     * 删除缓存
     */
    private void evictCache(String cacheName, String key, boolean allEntries) {
        if (allEntries) {
            // 清空本地缓存
            Cache cache = localCacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
            // 清空Redis缓存（通过模式匹配）
            String pattern = buildRedisKey(cacheName, "*");
            redisTemplate.delete(redisTemplate.keys(pattern));
            log.debug("清空所有缓存, cacheName={}", cacheName);
        } else {
            // 删除指定key
            Cache cache = localCacheManager.getCache(cacheName);
            if (cache != null) {
                cache.evict(key);
            }
            String redisKey = buildRedisKey(cacheName, key);
            redisTemplate.delete(redisKey);
            log.debug("删除缓存, cacheName={}, key={}", cacheName, key);
        }
    }

    /**
     * 构建Redis键
     */
    private String buildRedisKey(String cacheName, String key) {
        return "cache:" + cacheName + ":" + key;
    }

    /**
     * 生成缓存键
     */
    private String generateKey(Method method, Object[] args, String keyExpression) {
        if (!StringUtils.hasText(keyExpression)) {
            // 如果没有指定key表达式，使用方法签名
            StringBuilder sb = new StringBuilder();
            sb.append(method.getName());
            for (Object arg : args) {
                sb.append(":").append(arg != null ? arg.toString() : "null");
            }
            return sb.toString();
        }

        // 解析SpEL表达式
        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                null, method, args, parameterNameDiscoverer);
        return expressionParser.parseExpression(keyExpression).getValue(context, String.class);
    }

    /**
     * 评估条件表达式
     */
    private boolean evaluateCondition(Method method, Object[] args, String conditionExpression) {
        if (!StringUtils.hasText(conditionExpression)) {
            return true;
        }
        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                null, method, args, parameterNameDiscoverer);
        return Boolean.TRUE.equals(expressionParser.parseExpression(conditionExpression).getValue(context, Boolean.class));
    }

    /**
     * 获取方法对象
     */
    private Method getMethod(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getMethod();
    }
}
