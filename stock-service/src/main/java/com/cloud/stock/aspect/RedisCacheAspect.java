package com.cloud.stock.aspect;

import com.cloud.stock.annotation.RedisCacheEvict;
import com.cloud.stock.annotation.RedisCachePut;
import com.cloud.stock.annotation.RedisCacheable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * Redis缓存AOP切面
 * 拦截@RedisCacheable、@RedisCachePut、@RedisCacheEvict注解
 * 实现纯 Redis缓存逻辑，保证缓存一致性
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RedisCacheAspect {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    /**
     * 拦截@RedisCacheable注解的方法
     */
    @Around("@annotation(redisCacheable)")
    public Object handleCacheable(ProceedingJoinPoint joinPoint, RedisCacheable redisCacheable) throws Throwable {
        Method method = getMethod(joinPoint);
        String cacheKey = generateKey(method, joinPoint.getArgs(), redisCacheable.key());
        String cacheName = redisCacheable.cacheName();
        String redisKey = buildRedisKey(cacheName, cacheKey);

        // 检查条件
        if (!evaluateCondition(method, joinPoint.getArgs(), redisCacheable.condition())) {
            return joinPoint.proceed();
        }

        // 1. 从 Redis 缓存获取
        Object result = redisTemplate.opsForValue().get(redisKey);
        if (result != null) {
            log.debug("从 Redis 缓存获取数据, cacheName={}, key={}", cacheName, cacheKey);
            return result;
        }

        // 2. 缓存未命中，执行原方法
        log.debug("从数据源加载数据, cacheName={}, key={}", cacheName, cacheKey);
        result = joinPoint.proceed();

        // 3. 检查unless条件，决定是否缓存结果
        if (result != null && !evaluateCondition(method, joinPoint.getArgs(), redisCacheable.unless())) {
            redisTemplate.opsForValue().set(redisKey, result, redisCacheable.expire(), redisCacheable.timeUnit());
            log.debug("更新 Redis 缓存, cacheName={}, key={}, expire={}{}",
                    cacheName, cacheKey, redisCacheable.expire(), redisCacheable.timeUnit());
        }

        return result;
    }

    /**
     * 拦截@RedisCachePut注解的方法
     */
    @Around("@annotation(redisCachePut)")
    public Object handleCachePut(ProceedingJoinPoint joinPoint, RedisCachePut redisCachePut) throws Throwable {
        Method method = getMethod(joinPoint);
        String cacheKey = generateKey(method, joinPoint.getArgs(), redisCachePut.key());
        String cacheName = redisCachePut.cacheName();
        String redisKey = buildRedisKey(cacheName, cacheKey);

        // 总是执行原方法
        Object result = joinPoint.proceed();

        // 检查条件
        if (evaluateCondition(method, joinPoint.getArgs(), redisCachePut.condition()) &&
                !evaluateCondition(method, joinPoint.getArgs(), redisCachePut.unless()) &&
                result != null) {

            // 更新缓存
            redisTemplate.opsForValue().set(redisKey, result, redisCachePut.expire(), redisCachePut.timeUnit());
            log.debug("更新 Redis 缓存, cacheName={}, key={}", cacheName, cacheKey);
        }

        return result;
    }

    /**
     * 拦截@RedisCacheEvict注解的方法
     */
    @Around("@annotation(redisCacheEvict)")
    public Object handleCacheEvict(ProceedingJoinPoint joinPoint, RedisCacheEvict redisCacheEvict) throws Throwable {
        Method method = getMethod(joinPoint);
        String cacheKey = generateKey(method, joinPoint.getArgs(), redisCacheEvict.key());
        String cacheName = redisCacheEvict.cacheName();
        String redisKey = buildRedisKey(cacheName, cacheKey);

        // 检查条件
        boolean shouldEvict = evaluateCondition(method, joinPoint.getArgs(), redisCacheEvict.condition());

        // 方法执行前删除缓存
        if (shouldEvict && redisCacheEvict.beforeInvocation()) {
            evictRedisCache(cacheName, cacheKey, redisCacheEvict.allEntries());
        }

        Object result = joinPoint.proceed();

        // 方法执行后删除缓存
        if (shouldEvict && !redisCacheEvict.beforeInvocation()) {
            evictRedisCache(cacheName, cacheKey, redisCacheEvict.allEntries());
        }

        return result;
    }

    /**
     * 删除Redis缓存
     */
    private void evictRedisCache(String cacheName, String key, boolean allEntries) {
        if (allEntries) {
            // 清空所有缓存（通过模式匹配）
            String pattern = buildRedisKey(cacheName, "*");
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
            log.debug("清空 Redis 缓存, cacheName={}", cacheName);
        } else {
            // 删除指定key
            String redisKey = buildRedisKey(cacheName, key);
            redisTemplate.delete(redisKey);
            log.debug("删除 Redis 缓存, cacheName={}, key={}", cacheName, key);
        }
    }

    /**
     * 构建 Redis 键
     */
    private String buildRedisKey(String cacheName, String key) {
        return "stock-cache:" + cacheName + ":" + key;
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
