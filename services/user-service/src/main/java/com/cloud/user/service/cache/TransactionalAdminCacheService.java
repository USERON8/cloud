package com.cloud.user.service.cache;

import cn.hutool.core.util.StrUtil;
import com.cloud.user.module.entity.Admin;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionalAdminCacheService {

  private static final String KEY_ID_PREFIX = "admin:info:";
  private static final String KEY_NAME_PREFIX = "admin:info:name:";

  private static final String FIELD_ID = "id";
  private static final String FIELD_USERNAME = "username";
  private static final String FIELD_REAL_NAME = "realName";
  private static final String FIELD_PHONE = "phone";
  private static final String FIELD_ROLE = "role";
  private static final String FIELD_STATUS = "status";

  private final StringRedisTemplate redisTemplate;

  @Value("${user.cache.admin.ttl-seconds:1800}")
  private long ttlSeconds;

  public AdminCache getById(Long id) {
    if (id == null) {
      return null;
    }
    return getFromRedis(idKey(id));
  }

  public AdminCache getByUsername(String username) {
    if (StrUtil.isBlank(username)) {
      return null;
    }
    return getFromRedis(nameKey(username));
  }

  @Transactional(rollbackFor = Exception.class)
  public void putTransactional(Admin admin) {
    put(admin);
  }

  public void put(Admin admin) {
    if (admin == null || admin.getId() == null) {
      return;
    }

    Map<String, String> fields = buildFieldMap(admin);
    if (fields.isEmpty()) {
      return;
    }

    try {
      HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
      hashOperations.putAll(idKey(admin.getId()), fields);
      redisTemplate.expire(idKey(admin.getId()), ttl());

      if (StrUtil.isNotBlank(admin.getUsername())) {
        hashOperations.putAll(nameKey(admin.getUsername()), fields);
        redisTemplate.expire(nameKey(admin.getUsername()), ttl());
      }
    } catch (Exception ex) {
      log.warn("Write admin cache failed: adminId={}", admin.getId(), ex);
    }
  }

  @Transactional(rollbackFor = Exception.class)
  public void evictTransactional(Long id, String username) {
    evict(id, username);
  }

  public void evict(Long id, String username) {
    try {
      List<String> keys = new ArrayList<>(2);
      if (id != null) {
        keys.add(idKey(id));
      }
      if (StrUtil.isNotBlank(username)) {
        keys.add(nameKey(username));
      }
      if (!keys.isEmpty()) {
        redisTemplate.delete(keys);
      }
    } catch (Exception ex) {
      log.warn("Evict admin cache failed: adminId={}", id, ex);
    }
  }

  public void clearAll() {
    deleteByPattern(KEY_ID_PREFIX + "*");
    deleteByPattern(KEY_NAME_PREFIX + "*");
  }

  private void deleteByPattern(String pattern) {
    try {
      java.util.Set<String> keys = redisTemplate.keys(pattern);
      if (keys != null && !keys.isEmpty()) {
        redisTemplate.delete(keys);
      }
    } catch (Exception ex) {
      log.warn("Clear admin cache by pattern failed: pattern={}", pattern, ex);
    }
  }

  private AdminCache getFromRedis(String key) {
    try {
      Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
      if (entries == null || entries.isEmpty()) {
        return null;
      }
      redisTemplate.expire(key, ttl());
      return fromMap(entries);
    } catch (Exception ex) {
      log.warn("Read admin cache failed: key={}", key, ex);
      return null;
    }
  }

  private Map<String, String> buildFieldMap(Admin admin) {
    Map<String, String> fields = new HashMap<>();
    fields.put(FIELD_ID, String.valueOf(admin.getId()));
    putIfNotBlank(fields, FIELD_USERNAME, admin.getUsername());
    putIfNotBlank(fields, FIELD_REAL_NAME, admin.getRealName());
    putIfNotBlank(fields, FIELD_PHONE, admin.getPhone());
    putIfNotBlank(fields, FIELD_ROLE, admin.getRole());
    if (admin.getStatus() != null) {
      fields.put(FIELD_STATUS, String.valueOf(admin.getStatus()));
    }
    return fields;
  }

  private void putIfNotBlank(Map<String, String> map, String key, String value) {
    if (StrUtil.isNotBlank(value)) {
      map.put(key, value);
    }
  }

  private AdminCache fromMap(Map<Object, Object> map) {
    Long id = parseLong(map.get(FIELD_ID));
    if (id == null) {
      return null;
    }
    return new AdminCache(
        id,
        parseString(map.get(FIELD_USERNAME)),
        parseString(map.get(FIELD_REAL_NAME)),
        parseString(map.get(FIELD_PHONE)),
        parseString(map.get(FIELD_ROLE)),
        parseInteger(map.get(FIELD_STATUS)));
  }

  private Long parseLong(Object value) {
    if (value == null) {
      return null;
    }
    try {
      return Long.parseLong(value.toString());
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private Integer parseInteger(Object value) {
    if (value == null) {
      return null;
    }
    try {
      return Integer.parseInt(value.toString());
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private String parseString(Object value) {
    return value == null ? null : value.toString();
  }

  private String idKey(Long id) {
    return KEY_ID_PREFIX + id;
  }

  private String nameKey(String username) {
    return KEY_NAME_PREFIX + username;
  }

  private Duration ttl() {
    return Duration.ofSeconds(Math.max(60L, ttlSeconds));
  }

  public record AdminCache(
      Long id, String username, String realName, String phone, String role, Integer status) {}
}
