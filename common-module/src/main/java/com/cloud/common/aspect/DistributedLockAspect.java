package com.cloud.common.aspect;

import com.cloud.common.annotation.DistributedLock;
import com.cloud.common.exception.LockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

/**
 * 分布式锁AOP切面
 * 基于Redisson实现的声明式分布式锁切面处理
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
@Order(1)
@RequiredArgsConstructor
public class DistributedLockAspect {

    private final RedissonClient redissonClient;
    private final ExpressionParser parser = new SpelExpressionParser();

    /**
     * 环绕通知处理分布式锁
     *
     * @param joinPoint       连接点
     * @param distributedLock 分布式锁注解
     * @return 方法执行结果
     * @throws Throwable 异常
     */
    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        // 解析锁键
        String lockKey = parseLockKey(joinPoint, distributedLock);

        // 获取锁对象
        RLock lock = getLock(lockKey, distributedLock.lockType());

        // 记录锁操作开始时间
        long startTime = System.currentTimeMillis();

        try {
            // 尝试获取锁
            boolean acquired = tryAcquireLock(lock, distributedLock);

            if (!acquired) {
                // 获取锁失败，根据策略处理
                return handleLockFailure(joinPoint, distributedLock, lockKey);
            }

            long lockWaitTime = System.currentTimeMillis() - startTime;
            log.debug("🔒 获取分布式锁成功 - 锁键: {}, 等待时间: {}ms", lockKey, lockWaitTime);

            // 执行目标方法
            long methodStartTime = System.currentTimeMillis();
            Object result = joinPoint.proceed();
            long methodExecutionTime = System.currentTimeMillis() - methodStartTime;

            log.debug("✅ 方法执行完成 - 锁键: {}, 执行时间: {}ms", lockKey, methodExecutionTime);

            return result;

        } catch (Exception e) {
            log.error("❌ 分布式锁执行异常 - 锁键: {}, 异常: {}", lockKey, e.getMessage(), e);
            throw e;
        } finally {
            // 释放锁
            if (distributedLock.autoRelease() && lock.isHeldByCurrentThread()) {
                try {
                    lock.unlock();
                    long totalTime = System.currentTimeMillis() - startTime;
                    log.debug("🔓 释放分布式锁 - 锁键: {}, 总耗时: {}ms", lockKey, totalTime);
                } catch (Exception e) {
                    log.warn("⚠️ 释放分布式锁异常 - 锁键: {}, 异常: {}", lockKey, e.getMessage());
                }
            }
        }
    }

    /**
     * 解析锁键，支持SpEL表达式
     *
     * @param joinPoint       连接点
     * @param distributedLock 分布式锁注解
     * @return 解析后的锁键
     */
    private String parseLockKey(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) {
        String key = distributedLock.key();

        // 如果包含SpEL表达式
        if (key.contains("#")) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Object[] args = joinPoint.getArgs();
            String[] paramNames = signature.getParameterNames();

            // 创建SpEL上下文
            EvaluationContext context = new StandardEvaluationContext();

            // 设置方法参数
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }

            // 设置方法信息
            context.setVariable("method", method);
            context.setVariable("target", joinPoint.getTarget());

            // 解析表达式
            Expression expression = parser.parseExpression(key);
            Object value = expression.getValue(context);
            key = value != null ? value.toString() : "";
        }

        // 添加前缀
        String prefix = distributedLock.prefix();
        if (StringUtils.hasText(prefix)) {
            key = prefix + ":" + key;
        }

        // 验证锁键
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("分布式锁键不能为空");
        }

        return key;
    }

    /**
     * 获取锁对象
     *
     * @param lockKey  锁键
     * @param lockType 锁类型
     * @return 锁对象
     */
    private RLock getLock(String lockKey, DistributedLock.LockType lockType) {
        return switch (lockType) {
            case REENTRANT -> redissonClient.getLock(lockKey);
            case FAIR -> redissonClient.getFairLock(lockKey);
            case READ -> redissonClient.getReadWriteLock(lockKey).readLock();
            case WRITE -> redissonClient.getReadWriteLock(lockKey).writeLock();
            case RED_LOCK -> {
                // 红锁需要多个Redis实例，这里简化为单实例
                log.warn("⚠️ 红锁类型暂不支持多实例，降级为可重入锁");
                yield redissonClient.getLock(lockKey);
            }
        };
    }

    /**
     * 尝试获取锁
     *
     * @param lock            锁对象
     * @param distributedLock 分布式锁注解
     * @return 是否获取成功
     */
    private boolean tryAcquireLock(RLock lock, DistributedLock distributedLock) {
        try {
            return lock.tryLock(
                    distributedLock.waitTime(),
                    distributedLock.leaseTime(),
                    distributedLock.timeUnit()
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("❌ 获取分布式锁被中断", e);
            return false;
        }
    }

    /**
     * 处理锁获取失败
     *
     * @param joinPoint       连接点
     * @param distributedLock 分布式锁注解
     * @param lockKey         锁键
     * @return 处理结果
     * @throws Throwable 异常
     */
    private Object handleLockFailure(ProceedingJoinPoint joinPoint, DistributedLock distributedLock, String lockKey) throws Throwable {
        String failMessage = distributedLock.failMessage();

        return switch (distributedLock.failStrategy()) {
            case THROW_EXCEPTION -> {
                log.warn("❌ 获取分布式锁失败，抛出异常 - 锁键: {}", lockKey);
                throw new LockException("LOCK_ACQUIRE_FAILED", failMessage + " - 锁键: " + lockKey);
            }
            case RETURN_NULL -> {
                log.warn("❌ 获取分布式锁失败，返回null - 锁键: {}", lockKey);
                yield null;
            }
            case RETURN_DEFAULT -> {
                log.warn("❌ 获取分布式锁失败，返回默认值 - 锁键: {}", lockKey);
                yield getDefaultValue(joinPoint);
            }
            case FAIL_FAST -> {
                log.warn("❌ 获取分布式锁失败，快速失败 - 锁键: {}", lockKey);
                throw new LockException("LOCK_ACQUIRE_TIMEOUT", "获取锁超时，快速失败 - 锁键: " + lockKey);
            }
        };
    }

    /**
     * 获取方法返回类型的默认值
     *
     * @param joinPoint 连接点
     * @return 默认值
     */
    private Object getDefaultValue(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class<?> returnType = signature.getReturnType();

        if (returnType == void.class || returnType == Void.class) {
            return null;
        } else if (returnType == boolean.class || returnType == Boolean.class) {
            return false;
        } else if (returnType.isPrimitive()) {
            if (returnType == int.class || returnType == long.class ||
                    returnType == short.class || returnType == byte.class) {
                return 0;
            } else if (returnType == double.class || returnType == float.class) {
                return 0.0;
            } else if (returnType == char.class) {
                return '\0';
            }
        }

        return null;
    }
}
