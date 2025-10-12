package com.cloud.common.lock.controller;

import com.cloud.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RKeys;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分布式锁监控控制器
 * <p>
 * 提供分布式锁的监控和管理接口:
 * - 查看当前所有锁
 * - 查看锁详情
 * - 强制释放锁
 * - 锁统计信息
 *
 * @author CloudDevAgent
 * @since 2025-10-12
 */
@Slf4j
@RestController
@RequestMapping("/api/lock/monitor")
@Tag(name = "分布式锁监控", description = "分布式锁监控和管理接口")
@RequiredArgsConstructor
@ConditionalOnBean(RedissonClient.class)
public class DistributedLockMonitorController {

    private static final String LOCK_PREFIX_PATTERN = "distributed:lock:*";
    private final RedissonClient redissonClient;

    /**
     * 获取所有锁信息
     */
    @GetMapping("/locks")
    @Operation(summary = "获取所有分布式锁")
    public Result<List<Map<String, Object>>> getAllLocks() {
        try {
            RKeys keys = redissonClient.getKeys();
            Iterable<String> lockKeys = keys.getKeysByPattern(LOCK_PREFIX_PATTERN);

            List<Map<String, Object>> locks = new ArrayList<>();

            for (String lockKey : lockKeys) {
                Map<String, Object> lockInfo = getLockInfo(lockKey);
                if (lockInfo != null) {
                    locks.add(lockInfo);
                }
            }

            return Result.success(locks);

        } catch (Exception e) {
            log.error("获取所有锁信息失败", e);
            return Result.error("获取所有锁信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取指定锁的详细信息
     */
    @GetMapping("/lock/{lockKey}")
    @Operation(summary = "获取指定锁的详细信息")
    public Result<Map<String, Object>> getLockDetails(@PathVariable String lockKey) {
        try {
            String fullKey = lockKey.startsWith("distributed:lock:")
                    ? lockKey
                    : "distributed:lock:" + lockKey;

            Map<String, Object> lockInfo = getLockInfo(fullKey);

            if (lockInfo == null) {
                return Result.error("锁不存在: " + lockKey);
            }

            return Result.success(lockInfo);

        } catch (Exception e) {
            log.error("获取锁详情失败: {}", lockKey, e);
            return Result.error("获取锁详情失败: " + e.getMessage());
        }
    }

    /**
     * 强制释放锁 (危险操作,仅用于异常情况)
     */
    @DeleteMapping("/lock/{lockKey}")
    @Operation(summary = "强制释放指定锁")
    public Result<String> forceUnlock(@PathVariable String lockKey) {
        try {
            String fullKey = lockKey.startsWith("distributed:lock:")
                    ? lockKey
                    : "distributed:lock:" + lockKey;

            RLock lock = redissonClient.getLock(fullKey);

            if (!lock.isLocked()) {
                return Result.error("锁不存在或已释放: " + lockKey);
            }

            // 强制解锁
            lock.forceUnlock();

            log.warn("⚠️ 强制释放分布式锁: {}", lockKey);
            return Result.success("锁已强制释放: " + lockKey);

        } catch (Exception e) {
            log.error("强制释放锁失败: {}", lockKey, e);
            return Result.error("强制释放锁失败: " + e.getMessage());
        }
    }

    /**
     * 获取锁统计信息
     */
    @GetMapping("/stats")
    @Operation(summary = "获取锁统计信息")
    public Result<Map<String, Object>> getLockStats() {
        try {
            RKeys keys = redissonClient.getKeys();
            Iterable<String> lockKeys = keys.getKeysByPattern(LOCK_PREFIX_PATTERN);

            int totalLocks = 0;
            int activeLocks = 0;
            long totalTtl = 0;
            long minTtl = Long.MAX_VALUE;
            long maxTtl = 0;

            for (String lockKey : lockKeys) {
                totalLocks++;

                RLock lock = redissonClient.getLock(lockKey);

                if (lock.isLocked()) {
                    activeLocks++;
                }

                long ttl = lock.remainTimeToLive();
                if (ttl > 0) {
                    totalTtl += ttl;
                    minTtl = Math.min(minTtl, ttl);
                    maxTtl = Math.max(maxTtl, ttl);
                }
            }

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalLocks", totalLocks);
            stats.put("activeLocks", activeLocks);
            stats.put("inactiveLocks", totalLocks - activeLocks);
            stats.put("averageTtl", totalLocks > 0 ? totalTtl / totalLocks + "ms" : "N/A");
            stats.put("minTtl", minTtl != Long.MAX_VALUE ? minTtl + "ms" : "N/A");
            stats.put("maxTtl", maxTtl > 0 ? maxTtl + "ms" : "N/A");

            return Result.success(stats);

        } catch (Exception e) {
            log.error("获取锁统计信息失败", e);
            return Result.error("获取锁统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 清除所有过期锁
     */
    @DeleteMapping("/clear-expired")
    @Operation(summary = "清除所有过期锁")
    public Result<String> clearExpiredLocks() {
        try {
            RKeys keys = redissonClient.getKeys();
            Iterable<String> lockKeys = keys.getKeysByPattern(LOCK_PREFIX_PATTERN);

            int clearedCount = 0;

            for (String lockKey : lockKeys) {
                RLock lock = redissonClient.getLock(lockKey);

                // 如果锁未被持有,则删除
                if (!lock.isLocked()) {
                    keys.delete(lockKey);
                    clearedCount++;
                }
            }

            log.info("✅ 清除过期锁完成, 共清除 {} 个", clearedCount);
            return Result.success("清除完成, 共清除 " + clearedCount + " 个过期锁");

        } catch (Exception e) {
            log.error("清除过期锁失败", e);
            return Result.error("清除过期锁失败: " + e.getMessage());
        }
    }

    /**
     * 获取锁信息
     */
    private Map<String, Object> getLockInfo(String lockKey) {
        try {
            RLock lock = redissonClient.getLock(lockKey);

            Map<String, Object> info = new HashMap<>();
            info.put("lockKey", lockKey);
            info.put("isLocked", lock.isLocked());
            info.put("isHeldByCurrentThread", lock.isHeldByCurrentThread());
            info.put("holdCount", lock.getHoldCount());

            long ttl = lock.remainTimeToLive();
            info.put("remainTimeToLive", ttl > 0 ? ttl + "ms" : "已过期");

            // 获取锁的实际值
            RKeys keys = redissonClient.getKeys();
            long keyTtl = keys.remainTimeToLive(lockKey);
            info.put("keyTtl", keyTtl > 0 ? keyTtl + "ms" : (keyTtl == -1 ? "永久" : "不存在"));

            return info;

        } catch (Exception e) {
            log.warn("获取锁信息失败: {}", lockKey, e);
            return null;
        }
    }
}
