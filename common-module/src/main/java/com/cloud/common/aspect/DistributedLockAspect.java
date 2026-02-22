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

@Slf4j
@Aspect
@Component
@Order(1)
@RequiredArgsConstructor
public class DistributedLockAspect {

    private final RedissonClient redissonClient;
    private final ExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        String lockKey = parseLockKey(joinPoint, distributedLock);
        RLock lock = getLock(lockKey, distributedLock.lockType());
        long startTime = System.currentTimeMillis();

        try {
            boolean acquired = tryAcquireLock(lock, distributedLock);
            if (!acquired) {
                return handleLockFailure(joinPoint, distributedLock, lockKey);
            }

            long waitTime = System.currentTimeMillis() - startTime;
            log.debug("Lock acquired: key={}, waitMs={}", lockKey, waitTime);

            long methodStart = System.currentTimeMillis();
            Object result = joinPoint.proceed();
            long executeMs = System.currentTimeMillis() - methodStart;
            log.debug("Method executed under lock: key={}, executeMs={}", lockKey, executeMs);

            return result;
        } catch (Exception e) {
            log.error("Execution failed under lock: key={}", lockKey, e);
            throw e;
        } finally {
            if (distributedLock.autoRelease() && lock.isHeldByCurrentThread()) {
                try {
                    lock.unlock();
                    long totalMs = System.currentTimeMillis() - startTime;
                    log.debug("Lock released: key={}, totalMs={}", lockKey, totalMs);
                } catch (Exception e) {
                    log.warn("Failed to release lock: key={}, error={}", lockKey, e.getMessage());
                }
            }
        }
    }

    private String parseLockKey(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) {
        String key = distributedLock.key();

        if (key.contains("#")) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Object[] args = joinPoint.getArgs();
            String[] paramNames = signature.getParameterNames();

            EvaluationContext context = new StandardEvaluationContext();
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
            context.setVariable("method", method);
            context.setVariable("target", joinPoint.getTarget());

            Expression expression = parser.parseExpression(key);
            Object value = expression.getValue(context);
            key = value != null ? value.toString() : "";
        }

        String prefix = distributedLock.prefix();
        if (StringUtils.hasText(prefix)) {
            key = prefix + ":" + key;
        }

        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("Distributed lock key cannot be empty");
        }

        return key;
    }

    private RLock getLock(String lockKey, DistributedLock.LockType lockType) {
        return switch (lockType) {
            case REENTRANT -> redissonClient.getLock(lockKey);
            case FAIR -> redissonClient.getFairLock(lockKey);
            case READ -> redissonClient.getReadWriteLock(lockKey).readLock();
            case WRITE -> redissonClient.getReadWriteLock(lockKey).writeLock();
            case RED_LOCK -> {
                log.warn("RED_LOCK is configured as single lock fallback: key={}", lockKey);
                yield redissonClient.getLock(lockKey);
            }
        };
    }

    private boolean tryAcquireLock(RLock lock, DistributedLock distributedLock) {
        try {
            return lock.tryLock(
                    distributedLock.waitTime(),
                    distributedLock.leaseTime(),
                    distributedLock.timeUnit()
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while acquiring lock", e);
            return false;
        }
    }

    private Object handleLockFailure(ProceedingJoinPoint joinPoint,
                                     DistributedLock distributedLock,
                                     String lockKey) {
        String failMessage = distributedLock.failMessage();

        return switch (distributedLock.failStrategy()) {
            case THROW_EXCEPTION -> {
                log.warn("Failed to acquire lock: key={}", lockKey);
                throw new LockException("LOCK_ACQUIRE_FAILED", failMessage + " - key=" + lockKey);
            }
            case RETURN_NULL -> {
                log.warn("Failed to acquire lock and return null: key={}", lockKey);
                yield null;
            }
            case RETURN_DEFAULT -> {
                log.warn("Failed to acquire lock and return default: key={}", lockKey);
                yield getDefaultValue(joinPoint);
            }
            case FAIL_FAST -> {
                log.warn("Lock acquisition timeout: key={}", lockKey);
                throw new LockException("LOCK_ACQUIRE_TIMEOUT", "Lock acquisition timeout - key=" + lockKey);
            }
        };
    }

    private Object getDefaultValue(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class<?> returnType = signature.getReturnType();

        if (returnType == void.class || returnType == Void.class) {
            return null;
        }
        if (returnType == boolean.class || returnType == Boolean.class) {
            return false;
        }
        if (returnType == int.class || returnType == Integer.class) {
            return 0;
        }
        if (returnType == long.class || returnType == Long.class) {
            return 0L;
        }
        if (returnType == short.class || returnType == Short.class) {
            return (short) 0;
        }
        if (returnType == byte.class || returnType == Byte.class) {
            return (byte) 0;
        }
        if (returnType == double.class || returnType == Double.class) {
            return 0D;
        }
        if (returnType == float.class || returnType == Float.class) {
            return 0F;
        }
        if (returnType == char.class || returnType == Character.class) {
            return '\0';
        }
        return null;
    }
}
