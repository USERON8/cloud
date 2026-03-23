package com.cloud.user.service.cache;

import cn.hutool.core.util.StrUtil;
import com.cloud.user.module.entity.User;
import java.time.Duration;
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
public class TransactionalUserCacheService {

  private static final String KEY_ID_PREFIX = "user:info:";
  private static final String KEY_NAME_PREFIX = "user:info:name:";

  private static final String FIELD_ID = "id";
  private static final String FIELD_USERNAME = "username";
  private static final String FIELD_PHONE = "phone";
  private static final String FIELD_NICKNAME = "nickname";
  private static final String FIELD_AVATAR = "avatarUrl";
  private static final String FIELD_EMAIL = "email";
  private static final String FIELD_STATUS = "status";

  private final StringRedisTemplate redisTemplate;

  @Value("${user.cache.info.ttl-seconds:1800}")
  private long ttlSeconds;

  public UserCache getById(Long id) {
    if (id == null) {
      return null;
    }
    return getFromRedis(idKey(id));
  }

  public UserCache getByUsername(String username) {
    if (StrUtil.isBlank(username)) {
      return null;
    }
    return getFromRedis(nameKey(username));
  }

  @Transactional(rollbackFor = Exception.class)
  public void putTransactional(User user) {
    put(user);
  }

  public void put(User user) {
    if (user == null || user.getId() == null) {
      return;
    }

    Map<String, String> fields = buildFieldMap(user);
    if (fields.isEmpty()) {
      return;
    }

    try {
      HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
      hashOperations.putAll(idKey(user.getId()), fields);
      redisTemplate.expire(idKey(user.getId()), ttl());

      if (StrUtil.isNotBlank(user.getUsername())) {
        hashOperations.putAll(nameKey(user.getUsername()), fields);
        redisTemplate.expire(nameKey(user.getUsername()), ttl());
      }
    } catch (Exception ex) {
      log.warn("Write user info cache failed: userId={}", user.getId(), ex);
    }
  }

  @Transactional(rollbackFor = Exception.class)
  public void evictTransactional(Long id, String username) {
    evict(id, username);
  }

  public void evict(Long id, String username) {
    try {
      List<String> keys = new java.util.ArrayList<>(2);
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
      log.warn("Evict user info cache failed: userId={}", id, ex);
    }
  }

  private UserCache getFromRedis(String key) {
    try {
      Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
      if (entries == null || entries.isEmpty()) {
        return null;
      }
      redisTemplate.expire(key, ttl());
      return fromMap(entries);
    } catch (Exception ex) {
      log.warn("Read user info cache failed: key={}", key, ex);
      return null;
    }
  }

  private Map<String, String> buildFieldMap(User user) {
    Map<String, String> fields = new HashMap<>();
    if (user.getId() != null) {
      fields.put(FIELD_ID, String.valueOf(user.getId()));
    }
    putIfNotBlank(fields, FIELD_USERNAME, user.getUsername());
    putIfNotBlank(fields, FIELD_PHONE, user.getPhone());
    putIfNotBlank(fields, FIELD_NICKNAME, user.getNickname());
    putIfNotBlank(fields, FIELD_AVATAR, user.getAvatarUrl());
    putIfNotBlank(fields, FIELD_EMAIL, user.getEmail());
    if (user.getStatus() != null) {
      fields.put(FIELD_STATUS, String.valueOf(user.getStatus()));
    }
    return fields;
  }

  private void putIfNotBlank(Map<String, String> map, String key, String value) {
    if (StrUtil.isNotBlank(value)) {
      map.put(key, value);
    }
  }

  private UserCache fromMap(Map<Object, Object> map) {
    Long id = parseLong(map.get(FIELD_ID));
    if (id == null) {
      return null;
    }
    return new UserCache(
        id,
        parseString(map.get(FIELD_USERNAME)),
        parseString(map.get(FIELD_PHONE)),
        parseString(map.get(FIELD_NICKNAME)),
        parseString(map.get(FIELD_AVATAR)),
        parseString(map.get(FIELD_EMAIL)),
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

  public record UserCache(
      Long id,
      String username,
      String phone,
      String nickname,
      String avatarUrl,
      String email,
      Integer status) {}
}
