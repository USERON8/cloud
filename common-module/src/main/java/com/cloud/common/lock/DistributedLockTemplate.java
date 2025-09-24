package com.cloud.common.lock;

import com.cloud.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 分布式锁模板类
 * 提供更便捷的分布式锁使用方式，支持函数式编程风格
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * // 执行带锁的操作
 * String result = lockTemplate.execute("user:123", Duration.ofSeconds(10), () -> {
 *     // 业务逻辑
 *     return "success";
 * });
 *
 * // 尝试执行带锁的操作
 * Optional<String> result = lockTemplate.tryExecute("order:456", Duration.ofSeconds(5),
 *     Duration.ofMillis(200), () -> {
 *     // 业务逻辑
 *     return "success";
 * });
 * }</pre>
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedLockTemplate {

    private final DistributedLockManager lockManager;

    /**
     * 执行带锁的操作（阻塞等待获取锁）
     * 如果获取不到锁会抛出异常
     *
     * @param lockKey 锁键
     * @param timeout 锁过期时间
     * @param action  要执行的操作
     * @param <T>     返回值类型
     * @return 操作结果
     * @throws BusinessException 获取锁失败时抛出
     */
    public <T> T execute(String lockKey, Duration timeout, Supplier<T> action) {
        return execute(lockKey, timeout, Duration.ofMillis(200), action);
    }

    /**
     * 执行带锁的操作（指定等待时间）
     * 如果获取不到锁会抛出异常
     *
     * @param lockKey  锁键
     * @param timeout  锁过期时间
     * @param waitTime 等待获取锁的时间
     * @param action   要执行的操作
     * @param <T>      返回值类型
     * @return 操作结果
     * @throws BusinessException 获取锁失败时抛出
     */
    public <T> T execute(String lockKey, Duration timeout, Duration waitTime, Supplier<T> action) {
        LockInfo lockInfo = lockManager.tryLock(lockKey, timeout, waitTime);

        if (lockInfo == null) {
            throw new BusinessException(String.format("获取分布式锁失败，锁键: %s", lockKey));
        }

        try {
            log.debug("🔒 开始执行带锁操作 - 锁键: {}", lockKey);
            long startTime = System.currentTimeMillis();

            T result = action.get();

            long executionTime = System.currentTimeMillis() - startTime;
            log.debug("✅ 带锁操作执行完成 - 锁键: {}, 执行时间: {}ms", lockKey, executionTime);

            return result;

        } catch (Exception e) {
            log.error("❌ 带锁操作执行异常 - 锁键: {}", lockKey, e);
            throw e;
        } finally {
            // 确保锁被释放
            boolean unlocked = lockManager.unlock(lockInfo);
            if (!unlocked) {
                log.warn("⚠️ 锁释放失败，可能已过期 - 锁键: {}", lockKey);
            }
        }
    }

    /**
     * 尝试执行带锁的操作（非阻塞）
     * 如果获取不到锁会返回null
     *
     * @param lockKey 锁键
     * @param timeout 锁过期时间
     * @param action  要执行的操作
     * @param <T>     返回值类型
     * @return 操作结果，获取锁失败返回null
     */
    public <T> T tryExecute(String lockKey, Duration timeout, Supplier<T> action) {
        return tryExecute(lockKey, timeout, Duration.ZERO, action);
    }

    /**
     * 尝试执行带锁的操作（指定等待时间）
     * 如果获取不到锁会返回null
     *
     * @param lockKey  锁键
     * @param timeout  锁过期时间
     * @param waitTime 等待获取锁的时间
     * @param action   要执行的操作
     * @param <T>      返回值类型
     * @return 操作结果，获取锁失败返回null
     */
    public <T> T tryExecute(String lockKey, Duration timeout, Duration waitTime, Supplier<T> action) {
        LockInfo lockInfo = lockManager.tryLock(lockKey, timeout, waitTime);

        if (lockInfo == null) {
            log.debug("🔒 获取分布式锁失败，跳过执行 - 锁键: {}", lockKey);
            return null;
        }

        try {
            log.debug("🔒 开始执行带锁操作 - 锁键: {}", lockKey);
            long startTime = System.currentTimeMillis();

            T result = action.get();

            long executionTime = System.currentTimeMillis() - startTime;
            log.debug("✅ 带锁操作执行完成 - 锁键: {}, 执行时间: {}ms", lockKey, executionTime);

            return result;

        } catch (Exception e) {
            log.error("❌ 带锁操作执行异常 - 锁键: {}", lockKey, e);
            throw e;
        } finally {
            // 确保锁被释放
            boolean unlocked = lockManager.unlock(lockInfo);
            if (!unlocked) {
                log.warn("⚠️ 锁释放失败，可能已过期 - 锁键: {}", lockKey);
            }
        }
    }

    /**
     * 执行无返回值的带锁操作
     *
     * @param lockKey 锁键
     * @param timeout 锁过期时间
     * @param action  要执行的操作
     */
    public void execute(String lockKey, Duration timeout, Runnable action) {
        execute(lockKey, timeout, () -> {
            action.run();
            return null;
        });
    }

    /**
     * 执行无返回值的带锁操作（指定等待时间）
     *
     * @param lockKey  锁键
     * @param timeout  锁过期时间
     * @param waitTime 等待获取锁的时间
     * @param action   要执行的操作
     */
    public void execute(String lockKey, Duration timeout, Duration waitTime, Runnable action) {
        execute(lockKey, timeout, waitTime, () -> {
            action.run();
            return null;
        });
    }

    /**
     * 尝试执行无返回值的带锁操作
     *
     * @param lockKey 锁键
     * @param timeout 锁过期时间
     * @param action  要执行的操作
     * @return 是否执行成功（获取到锁并执行）
     */
    public boolean tryExecute(String lockKey, Duration timeout, Runnable action) {
        Object result = tryExecute(lockKey, timeout, () -> {
            action.run();
            return "success";
        });
        return result != null;
    }

    /**
     * 尝试执行无返回值的带锁操作（指定等待时间）
     *
     * @param lockKey  锁键
     * @param timeout  锁过期时间
     * @param waitTime 等待获取锁的时间
     * @param action   要执行的操作
     * @return 是否执行成功（获取到锁并执行）
     */
    public boolean tryExecute(String lockKey, Duration timeout, Duration waitTime, Runnable action) {
        Object result = tryExecute(lockKey, timeout, waitTime, () -> {
            action.run();
            return "success";
        });
        return result != null;
    }
}
