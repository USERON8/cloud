package com.cloud.common.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 缓存数据分析器
 * - 决定数据使用 String 还是 Hash 存储
 * - 提供对象到 Hash 的转换和从 Hash 的重建
 */
@Slf4j
@Component
public class CacheDataAnalyzer {

    private static final String META_CLASS = "_meta:class";

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 分析存储类型
     * 规则：
     * - 简单值类型/集合/Map -> String
     * - POJO/DTO 且字段数量适中(>=3) -> Hash
     * - user/userCache 等用户对象优先 Hash
     */
    public HybridCacheManager.CacheStorageType analyzeStorageType(String key, Object value) {
        if (value == null) return HybridCacheManager.CacheStorageType.STRING;

        Class<?> clazz = value.getClass();
        if (isSimpleValue(clazz) || isCollectionLike(clazz)) {
            return HybridCacheManager.CacheStorageType.STRING;
        }

        // 约定：用户相关 key 优先使用 Hash
        if (key != null && (key.contains(":userCache:") || key.contains(":user:") || key.startsWith("user:"))) {
            return HybridCacheManager.CacheStorageType.HASH;
        }

        // 尝试粗略统计字段数量
        try {
            Map<String, Object> map = objectMapper.convertValue(value, new TypeReference<Map<String, Object>>() {});
            int fieldCount = map != null ? map.size() : 0;
            if (fieldCount >= 3 && fieldCount <= 64) {
                return HybridCacheManager.CacheStorageType.HASH;
            }
        } catch (IllegalArgumentException e) {
            // 转换失败时退回 String
            return HybridCacheManager.CacheStorageType.STRING;
        }

        return HybridCacheManager.CacheStorageType.STRING;
    }

    /**
     * 将对象转换为 Hash 字段映射
     * - 嵌套复杂对象序列化为 JSON 字符串，避免深层结构
     * - 写入类元信息，便于重建
     */
    public Map<String, Object> convertToHash(Object value) {
        if (value == null) return null;
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> raw = objectMapper.convertValue(value, new TypeReference<Map<String, Object>>() {});
            if (raw != null) {
                for (Map.Entry<String, Object> e : raw.entrySet()) {
                    Object v = e.getValue();
                    if (v == null || isSimpleValue(v.getClass())) {
                        result.put(e.getKey(), v);
                    } else {
                        // 复杂对象序列化为字符串
                        result.put(e.getKey(), toJsonSafe(v));
                    }
                }
            }
            result.put(META_CLASS, value.getClass().getName());
            return result;
        } catch (Exception ex) {
            log.warn("对象转换为Hash失败，回退到String存储: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * 从 Hash 字段映射重建对象
     */
    public <T> T reconstructFromHash(Map<String, Object> hashData, Class<T> targetType) {
        if (hashData == null || hashData.isEmpty()) return null;
        try {
            // 尝试使用目标类型，如果没有提供则尝试使用元信息
            Class<T> type = targetType;
            if (type == null && hashData.containsKey(META_CLASS)) {
                try {
                    @SuppressWarnings("unchecked")
                    Class<T> inferred = (Class<T>) Class.forName(String.valueOf(hashData.get(META_CLASS)));
                    type = inferred;
                } catch (ClassNotFoundException ignored) { }
            }
            // 移除元信息
            Map<String, Object> copy = new HashMap<>(hashData);
            copy.remove(META_CLASS);
            return type != null ? objectMapper.convertValue(copy, type) : (T) copy;
        } catch (IllegalArgumentException e) {
            log.warn("从Hash重建对象失败: {}", e.getMessage());
            return null;
        }
    }

    private boolean isSimpleValue(Class<?> clazz) {
        return clazz.isPrimitive()
                || Number.class.isAssignableFrom(clazz)
                || CharSequence.class.isAssignableFrom(clazz)
                || Boolean.class.isAssignableFrom(clazz)
                || java.util.Date.class.isAssignableFrom(clazz)
                || java.time.temporal.Temporal.class.isAssignableFrom(clazz)
                || clazz.isEnum();
    }

    private boolean isCollectionLike(Class<?> clazz) {
        return java.util.Collection.class.isAssignableFrom(clazz)
                || java.util.Map.class.isAssignableFrom(clazz)
                || clazz.isArray();
    }

    private String toJsonSafe(Object v) {
        try {
            return objectMapper.writeValueAsString(v);
        } catch (JsonProcessingException e) {
            return String.valueOf(v);
        }
    }
}

