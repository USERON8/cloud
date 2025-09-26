package com.cloud.common.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 混合缓存策略管理器
 * 智能选择String或Hash存储类型，根据数据特性自动优化存储方式
 * 
 * <p>存储策略选择规则：</p>
 * <ul>
 *   <li><b>String类型</b>: 简单对象、序列化数据、完整对象缓存</li>
 *   <li><b>Hash类型</b>: 复杂对象、需要部分字段访问、对象属性频繁更新</li>
 * </ul>
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HybridCacheManager {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheDataAnalyzer dataAnalyzer;
    private final CachePerformanceMetrics performanceMetrics;
    
    private final ValueOperations<String, Object> stringOps;
    private final HashOperations<String, String, Object> hashOps;

    public HybridCacheManager(RedisTemplate<String, Object> redisTemplate,
                             CacheDataAnalyzer dataAnalyzer,
                             CachePerformanceMetrics performanceMetrics) {
        this.redisTemplate = redisTemplate;
        this.dataAnalyzer = dataAnalyzer;
        this.performanceMetrics = performanceMetrics;
        this.stringOps = redisTemplate.opsForValue();
        this.hashOps = redisTemplate.opsForHash();
    }

    /**
     * 智能缓存存储 - 自动选择最优存储类型
     *
     * @param key     缓存键
     * @param value   缓存值
     * @param timeout 过期时间
     * @param unit    时间单位
     */
    public void smartSet(String key, Object value, long timeout, TimeUnit unit) {
        if (value == null) {
            log.warn("⚠️ 尝试缓存null值，跳过缓存操作 - key: {}", key);
            return;
        }

        // 分析数据特性
        CacheStorageType recommendedType = dataAnalyzer.analyzeStorageType(key, value);
        
        long startTime = System.currentTimeMillis();
        
        try {
            switch (recommendedType) {
                case STRING -> setAsString(key, value, timeout, unit);
                case HASH -> setAsHash(key, value, timeout, unit);
                case AUTO -> {
                    // 自动模式：根据历史性能选择
                    CacheStorageType bestType = performanceMetrics.getBestPerformingType(key);
                    if (bestType == CacheStorageType.HASH) {
                        setAsHash(key, value, timeout, unit);
                    } else {
                        setAsString(key, value, timeout, unit);
                    }
                }
            }
            
            long duration = System.currentTimeMillis() - startTime;
            performanceMetrics.recordSetOperation(key, recommendedType, duration, true);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            performanceMetrics.recordSetOperation(key, recommendedType, duration, false);
            log.error("❌ 智能缓存存储失败 - key: {}, type: {}", key, recommendedType, e);
            throw e;
        }
    }

    /**
     * 智能缓存获取 - 自动识别存储类型并获取
     *
     * @param key 缓存键
     * @param targetType 目标返回类型
     * @param <T> 返回类型
     * @return 缓存值
     */
    @SuppressWarnings("unchecked")
    public <T> T smartGet(String key, Class<T> targetType) {
        long startTime = System.currentTimeMillis();
        CacheStorageType detectedType = null;
        
        try {
            var redisType = redisTemplate.type(key);
            if (redisType != null && "hash".equalsIgnoreCase(redisType.code())) {
                detectedType = CacheStorageType.HASH;
                Map<String, Object> hashData = hashOps.entries(key);
                if (!hashData.isEmpty()) {
                    T result = dataAnalyzer.reconstructFromHash(hashData, targetType);
                    long duration = System.currentTimeMillis() - startTime;
                    performanceMetrics.recordGetOperation(key, detectedType, duration, true);
                    log.debug("✅ Hash缓存命中 - key: {}", key);
                    return result;
                }
            }
            
            // 尝试String类型
            detectedType = CacheStorageType.STRING;
            Object stringData = stringOps.get(key);
            if (stringData != null) {
                T result;
                if (targetType != null && !targetType.isInstance(stringData)) {
                    // 尝试通过Hash的重建逻辑进行一次类型转换（如果可能）
                    if (stringData instanceof String s) {
                        // 交给数据分析器尝试重建（当stringData是JSON时）
                        Map<String, Object> map = new java.util.HashMap<>();
                        map.put("value", s);
                        result = dataAnalyzer.reconstructFromHash(map, targetType);
                    } else {
                        result = (T) stringData;
                    }
                } else {
                    result = (T) stringData;
                }
                long duration = System.currentTimeMillis() - startTime;
                performanceMetrics.recordGetOperation(key, detectedType, duration, true);
                log.debug("✅ String缓存命中 - key: {}", key);
                return result;
            }
            
            // 缓存未命中
            long duration = System.currentTimeMillis() - startTime;
            performanceMetrics.recordGetOperation(key, CacheStorageType.STRING, duration, false);
            log.debug("❌ 缓存未命中 - key: {}", key);
            return null;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            performanceMetrics.recordGetOperation(key, detectedType != null ? detectedType : CacheStorageType.STRING, 
                                                duration, false);
            log.error("❌ 智能缓存获取失败 - key: {}", key, e);
            return null;
        }
    }

    /**
     * Hash字段部分获取 - 适用于大对象的部分字段访问
     *
     * @param key    缓存键
     * @param fields 需要获取的字段列表
     * @return 字段值映射
     */
    public Map<String, Object> getHashFields(String key, String... fields) {
        long startTime = System.currentTimeMillis();
        
        try {
            List<Object> values = hashOps.multiGet(key, Arrays.asList(fields));
            Map<String, Object> result = new HashMap<>();
            
            for (int i = 0; i < fields.length; i++) {
                if (values.size() > i && values.get(i) != null) {
                    result.put(fields[i], values.get(i));
                }
            }
            
            long duration = System.currentTimeMillis() - startTime;
            performanceMetrics.recordGetOperation(key, CacheStorageType.HASH, duration, !result.isEmpty());
            
            log.debug("✅ Hash字段获取完成 - key: {}, fields: {}, found: {}", 
                     key, Arrays.toString(fields), result.size());
            
            return result;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            performanceMetrics.recordGetOperation(key, CacheStorageType.HASH, duration, false);
            log.error("❌ Hash字段获取失败 - key: {}, fields: {}", key, Arrays.toString(fields), e);
            return Collections.emptyMap();
        }
    }

    /**
     * Hash字段部分更新 - 适用于大对象的部分字段更新
     *
     * @param key     缓存键
     * @param fields  字段值映射
     * @param timeout 过期时间
     * @param unit    时间单位
     */
    public void updateHashFields(String key, Map<String, Object> fields, long timeout, TimeUnit unit) {
        if (fields == null || fields.isEmpty()) {
            log.warn("⚠️ 更新字段为空，跳过操作 - key: {}", key);
            return;
        }

        long startTime = System.currentTimeMillis();
        
        try {
            // 批量更新字段
            hashOps.putAll(key, fields);
            
            // 设置过期时间
            if (timeout > 0) {
                redisTemplate.expire(key, timeout, unit);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            performanceMetrics.recordSetOperation(key, CacheStorageType.HASH, duration, true);
            
            log.debug("✅ Hash字段更新完成 - key: {}, fields: {}", key, fields.keySet());
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            performanceMetrics.recordSetOperation(key, CacheStorageType.HASH, duration, false);
            log.error("❌ Hash字段更新失败 - key: {}, fields: {}", key, fields.keySet(), e);
            throw e;
        }
    }

    /**
     * 智能批量缓存存储
     *
     * @param cacheMap 缓存数据映射
     * @param timeout  过期时间
     * @param unit     时间单位
     */
    public void smartMultiSet(Map<String, Object> cacheMap, long timeout, TimeUnit unit) {
        if (cacheMap == null || cacheMap.isEmpty()) {
            return;
        }

        // 按存储类型分组
        Map<CacheStorageType, Map<String, Object>> typeGroups = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : cacheMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            CacheStorageType type = dataAnalyzer.analyzeStorageType(key, value);
            typeGroups.computeIfAbsent(type, k -> new HashMap<>()).put(key, value);
        }

        // 分类型批量存储
        typeGroups.forEach((type, dataMap) -> {
            switch (type) {
                case STRING -> batchSetAsString(dataMap, timeout, unit);
                case HASH -> batchSetAsHash(dataMap, timeout, unit);
                case AUTO -> {
                    // 自动模式分别处理
                    dataMap.forEach((k, v) -> smartSet(k, v, timeout, unit));
                }
            }
        });

        log.info("✅ 智能批量缓存存储完成 - 总数: {}, String: {}, Hash: {}, Auto: {}", 
                cacheMap.size(),
                typeGroups.getOrDefault(CacheStorageType.STRING, Collections.emptyMap()).size(),
                typeGroups.getOrDefault(CacheStorageType.HASH, Collections.emptyMap()).size(),
                typeGroups.getOrDefault(CacheStorageType.AUTO, Collections.emptyMap()).size());
    }

    /**
     * 获取缓存存储类型信息
     *
     * @param key 缓存键
     * @return 存储类型信息
     */
    public CacheStorageInfo getCacheStorageInfo(String key) {
        try {
            if (!redisTemplate.hasKey(key)) {
                return new CacheStorageInfo(key, CacheStorageType.NOT_EXISTS, 0, -1);
            }

            String type = redisTemplate.type(key).code();
            CacheStorageType storageType = switch (type) {
                case "string" -> CacheStorageType.STRING;
                case "hash" -> CacheStorageType.HASH;
                default -> CacheStorageType.UNKNOWN;
            };

            long size = switch (storageType) {
                case STRING -> {
                    Object value = stringOps.get(key);
                    yield value != null ? value.toString().length() : 0;
                }
                case HASH -> hashOps.size(key);
                default -> 0;
            };

            long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            
            return new CacheStorageInfo(key, storageType, size, ttl);
            
        } catch (Exception e) {
            log.error("❌ 获取缓存存储信息失败 - key: {}", key, e);
            return new CacheStorageInfo(key, CacheStorageType.ERROR, 0, -1);
        }
    }

    /**
     * 缓存性能分析报告
     *
     * @return 性能分析结果
     */
    public CachePerformanceMetrics.CachePerformanceReport getPerformanceReport() {
        return performanceMetrics.generateReport();
    }

    // ==================== 私有方法 ====================

    /**
     * 以String类型存储
     */
    private void setAsString(String key, Object value, long timeout, TimeUnit unit) {
        if (timeout > 0) {
            stringOps.set(key, value, timeout, unit);
        } else {
            stringOps.set(key, value);
        }
        log.debug("🔤 String缓存存储 - key: {}, timeout: {}", key, timeout);
    }

    /**
     * 以Hash类型存储
     */
    private void setAsHash(String key, Object value, long timeout, TimeUnit unit) {
        Map<String, Object> hashData = dataAnalyzer.convertToHash(value);
        if (hashData != null && !hashData.isEmpty()) {
            hashOps.putAll(key, hashData);
            if (timeout > 0) {
                redisTemplate.expire(key, timeout, unit);
            }
            log.debug("🗂️ Hash缓存存储 - key: {}, fields: {}, timeout: {}", 
                     key, hashData.size(), timeout);
        } else {
            // 回退到String存储
            setAsString(key, value, timeout, unit);
            log.debug("⚠️ Hash转换失败，回退到String存储 - key: {}", key);
        }
    }

    /**
     * 批量String存储
     */
    private void batchSetAsString(Map<String, Object> dataMap, long timeout, TimeUnit unit) {
        try {
            stringOps.multiSet(dataMap);
            if (timeout > 0) {
                dataMap.keySet().forEach(key -> redisTemplate.expire(key, timeout, unit));
            }
            log.debug("🔤 批量String缓存存储完成 - 数量: {}", dataMap.size());
        } catch (Exception e) {
            log.error("❌ 批量String缓存存储失败", e);
            // 降级到单个存储
            dataMap.forEach((k, v) -> setAsString(k, v, timeout, unit));
        }
    }

    /**
     * 批量Hash存储
     */
    private void batchSetAsHash(Map<String, Object> dataMap, long timeout, TimeUnit unit) {
        dataMap.forEach((key, value) -> setAsHash(key, value, timeout, unit));
        log.debug("🗂️ 批量Hash缓存存储完成 - 数量: {}", dataMap.size());
    }

    /**
     * 智能删除缓存
     */
    public boolean smartDelete(String key) {
        try {
            Boolean deleted = redisTemplate.delete(key);
            log.debug("🗑️ 缓存删除 - key: {}, success: {}", key, deleted);
            return Boolean.TRUE.equals(deleted);
        } catch (Exception e) {
            log.error("❌ 缓存删除失败 - key: {}", key, e);
            return false;
        }
    }

    /**
     * 缓存存储类型枚举
     */
    public enum CacheStorageType {
        STRING("String类型存储，适用于简单对象"),
        HASH("Hash类型存储，适用于复杂对象部分访问"),
        AUTO("自动选择最优存储类型"),
        NOT_EXISTS("缓存不存在"),
        UNKNOWN("未知存储类型"),
        ERROR("获取类型时发生错误");

        private final String description;

        CacheStorageType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 缓存存储信息
     */
    public record CacheStorageInfo(
            String key,
            CacheStorageType type,
            long size,
            long ttlSeconds
    ) {
        public String getFormattedInfo() {
            return String.format("Key: %s, Type: %s, Size: %d, TTL: %ds", 
                               key, type.name(), size, ttlSeconds);
        }
    }
}
