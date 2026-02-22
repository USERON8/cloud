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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/lock/monitor")
@Tag(name = "Distributed Lock Monitor", description = "Distributed lock monitoring and management APIs")
@RequiredArgsConstructor
@ConditionalOnBean(RedissonClient.class)
public class DistributedLockMonitorController {

    private static final String LOCK_PREFIX = "distributed:lock:";
    private static final String LOCK_PREFIX_PATTERN = LOCK_PREFIX + "*";

    private final RedissonClient redissonClient;

    @GetMapping("/locks")
    @Operation(summary = "Get all distributed locks")
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
            log.error("Failed to load lock list", e);
            return Result.error("Failed to load lock list: " + e.getMessage());
        }
    }

    @GetMapping("/lock/{lockKey}")
    @Operation(summary = "Get lock details")
    public Result<Map<String, Object>> getLockDetails(@PathVariable String lockKey) {
        try {
            String fullKey = normalizeLockKey(lockKey);
            Map<String, Object> lockInfo = getLockInfo(fullKey);
            if (lockInfo == null) {
                return Result.error("Lock not found: " + lockKey);
            }
            return Result.success(lockInfo);
        } catch (Exception e) {
            log.error("Failed to load lock details: {}", lockKey, e);
            return Result.error("Failed to load lock details: " + e.getMessage());
        }
    }

    @DeleteMapping("/lock/{lockKey}")
    @Operation(summary = "Force unlock a lock")
    public Result<String> forceUnlock(@PathVariable String lockKey) {
        try {
            String fullKey = normalizeLockKey(lockKey);
            RLock lock = redissonClient.getLock(fullKey);
            if (!lock.isLocked()) {
                return Result.error("Lock does not exist or is already unlocked: " + lockKey);
            }

            lock.forceUnlock();
            log.warn("Force unlocked distributed lock: {}", fullKey);
            return Result.success("Force unlocked: " + lockKey);
        } catch (Exception e) {
            log.error("Failed to force unlock: {}", lockKey, e);
            return Result.error("Failed to force unlock: " + e.getMessage());
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "Get lock statistics")
    public Result<Map<String, Object>> getLockStats() {
        try {
            RKeys keys = redissonClient.getKeys();
            Iterable<String> lockKeys = keys.getKeysByPattern(LOCK_PREFIX_PATTERN);

            int totalLocks = 0;
            int activeLocks = 0;
            long totalTtl = 0;
            long minTtl = Long.MAX_VALUE;
            long maxTtl = 0;
            long ttlCount = 0;

            for (String lockKey : lockKeys) {
                totalLocks++;
                RLock lock = redissonClient.getLock(lockKey);
                if (lock.isLocked()) {
                    activeLocks++;
                }

                long ttl = lock.remainTimeToLive();
                if (ttl > 0) {
                    ttlCount++;
                    totalTtl += ttl;
                    minTtl = Math.min(minTtl, ttl);
                    maxTtl = Math.max(maxTtl, ttl);
                }
            }

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalLocks", totalLocks);
            stats.put("activeLocks", activeLocks);
            stats.put("inactiveLocks", totalLocks - activeLocks);
            stats.put("averageTtl", ttlCount > 0 ? (totalTtl / ttlCount) + "ms" : "N/A");
            stats.put("minTtl", ttlCount > 0 ? minTtl + "ms" : "N/A");
            stats.put("maxTtl", ttlCount > 0 ? maxTtl + "ms" : "N/A");

            return Result.success(stats);
        } catch (Exception e) {
            log.error("Failed to load lock statistics", e);
            return Result.error("Failed to load lock statistics: " + e.getMessage());
        }
    }

    @DeleteMapping("/clear-expired")
    @Operation(summary = "Clear expired lock keys")
    public Result<String> clearExpiredLocks() {
        try {
            RKeys keys = redissonClient.getKeys();
            Iterable<String> lockKeys = keys.getKeysByPattern(LOCK_PREFIX_PATTERN);
            int clearedCount = 0;

            for (String lockKey : lockKeys) {
                RLock lock = redissonClient.getLock(lockKey);
                if (!lock.isLocked()) {
                    keys.delete(lockKey);
                    clearedCount++;
                }
            }

            return Result.success("Cleared expired lock keys: " + clearedCount);
        } catch (Exception e) {
            log.error("Failed to clear expired lock keys", e);
            return Result.error("Failed to clear expired lock keys: " + e.getMessage());
        }
    }

    private String normalizeLockKey(String lockKey) {
        if (lockKey.startsWith(LOCK_PREFIX)) {
            return lockKey;
        }
        return LOCK_PREFIX + lockKey;
    }

    private Map<String, Object> getLockInfo(String lockKey) {
        try {
            RLock lock = redissonClient.getLock(lockKey);
            Map<String, Object> info = new HashMap<>();
            info.put("lockKey", lockKey);
            info.put("isLocked", lock.isLocked());
            info.put("isHeldByCurrentThread", lock.isHeldByCurrentThread());
            info.put("holdCount", lock.getHoldCount());

            long ttl = lock.remainTimeToLive();
            info.put("remainTimeToLive", ttl > 0 ? ttl + "ms" : "expired");

            RKeys keys = redissonClient.getKeys();
            long keyTtl = keys.remainTimeToLive(lockKey);
            if (keyTtl > 0) {
                info.put("keyTtl", keyTtl + "ms");
            } else if (keyTtl == -1) {
                info.put("keyTtl", "persistent");
            } else {
                info.put("keyTtl", "not_exists");
            }

            return info;
        } catch (Exception e) {
            log.warn("Failed to get lock info: {}", lockKey, e);
            return null;
        }
    }
}
