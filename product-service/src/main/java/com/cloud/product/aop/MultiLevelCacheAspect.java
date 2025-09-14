package com.cloud.product.aop;

import com.cloud.common.cache.annotation.MultiLevelCacheEvict;
import com.cloud.common.cache.annotation.MultiLevelCachePut;
import com.cloud.common.cache.annotation.MultiLevelCacheable;
import com.cloud.common.cache.annotation.MultiLevelCaching;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Autowired;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Set;

/**
 * 商品服务多级缓存AOP切面
 * 处理 @MultiLevelCacheable、@MultiLevelCachePut、@MultiLevelCacheEvict 和 @MultiLevelCaching 注解
 * 实现本地缓存（L1）+ Redis缓存（L2）的多级缓存机制
 *
 * @author what's up
 */
@Aspect
@Component
public class MultiLevelCacheAspect {

    private static final Logger logger = LoggerFactory.getLogger(MultiLevelCacheAspect.class);
    private final ExpressionParser parser = new SpelExpressionParser();
    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 处理 @MultiLevelCacheable 注解
     * 先查本地缓存，再查Redis缓存，都没有则执行方法并缓存结果
     */
    @Around("@annotation(com.cloud.common.cache.annotation.MultiLevelCacheable)")
    public Object handleCacheable(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        MultiLevelCacheable annotation = method.getAnnotation(MultiLevelCacheable.class);

        if (annotation == null) {
            return joinPoint.proceed();
        }

        String key = generateKey(joinPoint, annotation.key());
        String cacheName = annotation.value();

        // 检查条件
        if (!evaluateCondition(joinPoint, annotation.condition())) {
            return joinPoint.proceed();
        }

        // 1. 检查本地缓存（如果启用）
        Object result = null;
        Cache localCache = null;

        if (annotation.enableLocalCache() && cacheManager != null) {
            localCache = cacheManager.getCache(cacheName);
            if (localCache != null) {
                Cache.ValueWrapper wrapper = localCache.get(key);
                if (wrapper != null) {
                    result = wrapper.get();
                    logger.debug("本地缓存命中: key={}", key);
                    return result;
                }
            }
        }

        // 2. 检查Redis缓存
        try {
            String redisKey = cacheName + ":" + key;
            result = redisTemplate.opsForValue().get(redisKey);

            if (result != null) {
                logger.debug("Redis缓存命中: key={}", redisKey);

                // 回写到本地缓存
                if (annotation.enableLocalCache() && localCache != null) {
                    localCache.put(key, result);
                }
                return result;
            }
        } catch (Exception e) {
            logger.warn("Redis缓存读取失败: key={}", key, e);
        }

        // 3. 执行目标方法
        result = joinPoint.proceed();

        if (result != null) {
            // 4. 存储到Redis缓存
            try {
                String redisKey = cacheName + ":" + key;
                if (annotation.expire() > 0) {
                    redisTemplate.opsForValue().set(redisKey, result,
                            Duration.of(annotation.expire(), annotation.timeUnit().toChronoUnit()));
                } else {
                    redisTemplate.opsForValue().set(redisKey, result);
                }
                logger.debug("数据已存储到Redis缓存: key={}", redisKey);
            } catch (Exception e) {
                logger.warn("Redis缓存写入失败: key={}", key, e);
            }

            // 5. 存储到本地缓存
            if (annotation.enableLocalCache() && localCache != null) {
                localCache.put(key, result);
                logger.debug("数据已存储到本地缓存: key={}", key);
            }
        }

        return result;
    }

    /**
     * 处理 @MultiLevelCachePut 注解
     * 总是执行方法，并将结果存储到缓存
     */
    @Around("@annotation(com.cloud.common.cache.annotation.MultiLevelCachePut)")
    public Object handleCachePut(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        MultiLevelCachePut annotation = method.getAnnotation(MultiLevelCachePut.class);

        if (annotation == null) {
            return joinPoint.proceed();
        }

        // 检查条件
        if (!evaluateCondition(joinPoint, annotation.condition())) {
            return joinPoint.proceed();
        }

        // 执行方法
        Object result = joinPoint.proceed();

        if (result != null) {
            String key = generateKey(joinPoint, annotation.key());
            String cacheName = annotation.value();

            // 存储到Redis缓存
            try {
                String redisKey = cacheName + ":" + key;
                if (annotation.expire() > 0) {
                    redisTemplate.opsForValue().set(redisKey, result,
                            Duration.of(annotation.expire(), annotation.timeUnit().toChronoUnit()));
                } else {
                    redisTemplate.opsForValue().set(redisKey, result);
                }
                logger.debug("@MultiLevelCachePut 数据已更新到Redis缓存: key={}", redisKey);
            } catch (Exception e) {
                logger.warn("Redis缓存更新失败: key={}", key, e);
            }

            // 存储到本地缓存
            if (annotation.enableLocalCache() && cacheManager != null) {
                Cache localCache = cacheManager.getCache(cacheName);
                if (localCache != null) {
                    localCache.put(key, result);
                    logger.debug("@MultiLevelCachePut 数据已更新到本地缓存: key={}", key);
                }
            }
        }

        return result;
    }

    /**
     * 处理 @MultiLevelCacheEvict 注解
     * 从缓存中删除数据
     */
    @Around("@annotation(com.cloud.common.cache.annotation.MultiLevelCacheEvict)")
    public Object handleCacheEvict(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        MultiLevelCacheEvict annotation = method.getAnnotation(MultiLevelCacheEvict.class);

        if (annotation == null) {
            return joinPoint.proceed();
        }

        // 检查条件
        if (!evaluateCondition(joinPoint, annotation.condition())) {
            return joinPoint.proceed();
        }

        Object result = null;

        // beforeInvocation为true时，在方法执行前清除缓存
        if (annotation.beforeInvocation()) {
            evictCache(joinPoint, annotation);
        }

        try {
            result = joinPoint.proceed();
        } finally {
            // beforeInvocation为false时，在方法执行后清除缓存
            if (!annotation.beforeInvocation()) {
                evictCache(joinPoint, annotation);
            }
        }

        return result;
    }

    /**
     * 处理 @MultiLevelCaching 注解
     * 组合处理多个缓存注解
     */
    @Around("@annotation(com.cloud.common.cache.annotation.MultiLevelCaching)")
    public Object handleCaching(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        MultiLevelCaching annotation = method.getAnnotation(MultiLevelCaching.class);

        if (annotation == null) {
            return joinPoint.proceed();
        }

        // 处理 evict 注解（beforeInvocation=true的）
        for (MultiLevelCacheEvict evict : annotation.evict()) {
            if (evict.beforeInvocation() && evaluateCondition(joinPoint, evict.condition())) {
                evictCacheByEvict(joinPoint, evict);
            }
        }

        Object result = null;
        boolean cacheHit = false;

        // 处理 cacheable 注解
        for (MultiLevelCacheable cacheable : annotation.cacheable()) {
            if (evaluateCondition(joinPoint, cacheable.condition())) {
                result = handleCacheableLogic(joinPoint, cacheable);
                if (result != null) {
                    cacheHit = true;
                    break;
                }
            }
        }

        // 如果缓存没有命中，执行方法
        if (!cacheHit) {
            result = joinPoint.proceed();
        }

        // 处理 put 注解
        for (MultiLevelCachePut put : annotation.put()) {
            if (evaluateCondition(joinPoint, put.condition()) && result != null) {
                handleCachePutLogic(joinPoint, put, result);
            }
        }

        // 处理 evict 注解（beforeInvocation=false的）
        for (MultiLevelCacheEvict evict : annotation.evict()) {
            if (!evict.beforeInvocation() && evaluateCondition(joinPoint, evict.condition())) {
                evictCacheByEvict(joinPoint, evict);
            }
        }

        return result;
    }

    private Object handleCacheableLogic(ProceedingJoinPoint joinPoint, MultiLevelCacheable annotation) {
        String key = generateKey(joinPoint, annotation.key());
        String cacheName = annotation.value();

        // 检查本地缓存
        if (annotation.enableLocalCache() && cacheManager != null) {
            Cache localCache = cacheManager.getCache(cacheName);
            if (localCache != null) {
                Cache.ValueWrapper wrapper = localCache.get(key);
                if (wrapper != null) {
                    return wrapper.get();
                }
            }
        }

        // 检查Redis缓存
        try {
            String redisKey = cacheName + ":" + key;
            return redisTemplate.opsForValue().get(redisKey);
        } catch (Exception e) {
            logger.warn("Redis缓存读取失败: key={}", key, e);
            return null;
        }
    }

    private void handleCachePutLogic(ProceedingJoinPoint joinPoint, MultiLevelCachePut annotation, Object result) {
        String key = generateKey(joinPoint, annotation.key());
        String cacheName = annotation.value();

        // 存储到Redis缓存
        try {
            String redisKey = cacheName + ":" + key;
            if (annotation.expire() > 0) {
                redisTemplate.opsForValue().set(redisKey, result,
                        Duration.of(annotation.expire(), annotation.timeUnit().toChronoUnit()));
            } else {
                redisTemplate.opsForValue().set(redisKey, result);
            }
        } catch (Exception e) {
            logger.warn("Redis缓存写入失败: key={}", key, e);
        }

        // 存储到本地缓存
        if (annotation.enableLocalCache() && cacheManager != null) {
            Cache localCache = cacheManager.getCache(cacheName);
            if (localCache != null) {
                localCache.put(key, result);
            }
        }
    }

    private void evictCache(ProceedingJoinPoint joinPoint, MultiLevelCacheEvict annotation) {
        evictCacheByEvict(joinPoint, annotation);
    }

    private void evictCacheByEvict(ProceedingJoinPoint joinPoint, MultiLevelCacheEvict annotation) {
        String[] cacheNames = annotation.value();
        
        // 处理每个缓存名称
        for (String cacheName : cacheNames) {
            evictSingleCache(joinPoint, annotation, cacheName);
        }
    }
    
    private void evictSingleCache(ProceedingJoinPoint joinPoint, MultiLevelCacheEvict annotation, String cacheName) {
        if (annotation.allEntries()) {
            // 清空所有缓存
            if (annotation.enableLocalCache() && cacheManager != null) {
                Cache localCache = cacheManager.getCache(cacheName);
                if (localCache != null) {
                    localCache.clear();
                    logger.debug("已清空本地缓存: cacheName={}", cacheName);
                }
            }

            // 清空Redis缓存
            try {
                Set<String> keys = redisTemplate.keys(cacheName + ":*");
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                }
                logger.debug("已清空Redis缓存: cacheName={}", cacheName);
            } catch (Exception e) {
                logger.warn("清空Redis缓存失败: cacheName={}", cacheName, e);
            }
        } else {
            // 清除指定key的缓存
            String key = generateKey(joinPoint, annotation.key());

            if (annotation.enableLocalCache() && cacheManager != null) {
                Cache localCache = cacheManager.getCache(cacheName);
                if (localCache != null) {
                    localCache.evict(key);
                    logger.debug("已清除本地缓存: key={}", key);
                }
            }

            // 清除Redis缓存
            try {
                String redisKey = cacheName + ":" + key;
                redisTemplate.delete(redisKey);
                logger.debug("已清除Redis缓存: key={}", redisKey);
            } catch (Exception e) {
                logger.warn("清除Redis缓存失败: key={}", key, e);
            }
        }
    }

    /**
     * 生成缓存key
     */
    private String generateKey(ProceedingJoinPoint joinPoint, String keyExpression) {
        if (keyExpression == null || keyExpression.isEmpty()) {
            // 默认key生成策略：类名.方法名.参数hash
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            StringBuilder keyBuilder = new StringBuilder();
            keyBuilder.append(signature.getDeclaringType().getSimpleName())
                    .append(".")
                    .append(signature.getName());

            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                keyBuilder.append(".").append(java.util.Arrays.hashCode(args));
            }

            return keyBuilder.toString();
        }

        try {
            // 解析SpEL表达式
            Expression expression = parser.parseExpression(keyExpression);
            EvaluationContext context = createEvaluationContext(joinPoint);
            return String.valueOf(expression.getValue(context));
        } catch (Exception e) {
            logger.warn("解析缓存key表达式失败: {}", keyExpression, e);
            return keyExpression;
        }
    }

    /**
     * 评估条件表达式
     */
    private boolean evaluateCondition(ProceedingJoinPoint joinPoint, String condition) {
        if (condition == null || condition.trim().isEmpty()) {
            return true;
        }

        try {
            Expression expression = parser.parseExpression(condition);
            EvaluationContext context = createEvaluationContext(joinPoint);
            Boolean result = expression.getValue(context, Boolean.class);
            return result != null ? result : true;
        } catch (Exception e) {
            logger.warn("评估条件表达式失败: {}", condition, e);
            return true;
        }
    }

    /**
     * 创建SpEL表达式评估上下文
     */
    private EvaluationContext createEvaluationContext(ProceedingJoinPoint joinPoint) {
        StandardEvaluationContext context = new StandardEvaluationContext();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        // 设置方法参数
        if (paramNames != null && args != null) {
            for (int i = 0; i < paramNames.length && i < args.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }

        // 设置方法和目标对象
        context.setVariable("method", signature.getMethod());
        context.setVariable("target", joinPoint.getTarget());
        context.setVariable("args", args);

        return context;
    }
}
