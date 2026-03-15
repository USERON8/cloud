package com.cloud.user.service.support;

import com.cloud.user.module.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import cn.hutool.core.util.StrUtil;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserInfoHashCacheService {

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
        String key = idKey(id);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        UserCache cache = fromMap(entries);
        if (cache == null) {
            return null;
        }
        renewTtl(key);
        if (StrUtil.isNotBlank(cache.username())) {
            renewTtl(nameKey(cache.username()));
        }
        return cache;
    }

    public UserCache getByUsername(String username) {
        if (StrUtil.isBlank(username)) {
            return null;
        }
        String key = nameKey(username);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        UserCache cache = fromMap(entries);
        if (cache == null) {
            return null;
        }
        renewTtl(key);
        if (cache.id() != null) {
            renewTtl(idKey(cache.id()));
        }
        return cache;
    }

    public void put(User user) {
        if (user == null || user.getId() == null) {
            return;
        }
        Map<String, String> fields = buildFieldMap(user);
        if (fields.isEmpty()) {
            return;
        }
        String idKey = idKey(user.getId());
        try {
            redisTemplate.opsForHash().putAll(idKey, fields);
            renewTtl(idKey);
            if (StrUtil.isNotBlank(user.getUsername())) {
                String nameKey = nameKey(user.getUsername());
                redisTemplate.opsForHash().putAll(nameKey, fields);
                renewTtl(nameKey);
            }
        } catch (Exception ex) {
            log.warn("Cache user info failed: id={}", user.getId(), ex);
        }
    }

    public void updateFields(Long id, String username, Map<String, String> fields) {
        if (id == null || fields == null || fields.isEmpty()) {
            return;
        }
        Map<String, String> payload = normalizeFields(fields);
        payload.put(FIELD_ID, String.valueOf(id));
        if (StrUtil.isNotBlank(username)) {
            payload.put(FIELD_USERNAME, username);
        }
        String idKey = idKey(id);
        try {
            redisTemplate.opsForHash().putAll(idKey, payload);
            renewTtl(idKey);
            if (StrUtil.isNotBlank(username)) {
                String nameKey = nameKey(username);
                redisTemplate.opsForHash().putAll(nameKey, payload);
                renewTtl(nameKey);
            }
        } catch (Exception ex) {
            log.warn("Update user cache fields failed: id={}", id, ex);
        }
    }

    public void updateUsernameAtomic(Long id, String oldUsername, String newUsername, Map<String, String> fields) {
        if (id == null || StrUtil.isBlank(newUsername) || fields == null || fields.isEmpty()) {
            return;
        }
        Map<String, String> payload = normalizeFields(fields);
        payload.put(FIELD_ID, String.valueOf(id));
        payload.put(FIELD_USERNAME, newUsername);
        String idKey = idKey(id);
        String newNameKey = nameKey(newUsername);
        String oldNameKey = StrUtil.isNotBlank(oldUsername) ? nameKey(oldUsername) : null;
        try {
            redisTemplate.execute(new SessionCallback<java.util.List<Object>>() {
                @Override
                public java.util.List<Object> execute(RedisOperations<String, String> operations) {
                    operations.multi();
                    if (StrUtil.isNotBlank(oldNameKey)) {
                        operations.delete(oldNameKey);
                    }
                    operations.opsForHash().putAll(idKey, payload);
                    operations.expire(idKey, ttl());
                    operations.opsForHash().putAll(newNameKey, payload);
                    operations.expire(newNameKey, ttl());
                    return operations.exec();
                }
            });
        } catch (Exception ex) {
            log.warn("Update username cache atomically failed: id={}", id, ex);
        }
    }

    public void evict(Long id, String username) {
        try {
            if (id != null) {
                redisTemplate.delete(idKey(id));
            }
            if (StrUtil.isNotBlank(username)) {
                redisTemplate.delete(nameKey(username));
            }
        } catch (Exception ex) {
            log.warn("Evict user cache failed: id={}", id, ex);
        }
    }

    private Map<String, String> buildFieldMap(User user) {
        Map<String, String> fields = new HashMap<>();
        if (user.getId() != null) {
            fields.put(FIELD_ID, String.valueOf(user.getId()));
        }
        putIfNotNull(fields, FIELD_USERNAME, user.getUsername());
        putIfNotNull(fields, FIELD_PHONE, user.getPhone());
        putIfNotNull(fields, FIELD_NICKNAME, user.getNickname());
        putIfNotNull(fields, FIELD_AVATAR, user.getAvatarUrl());
        putIfNotNull(fields, FIELD_EMAIL, user.getEmail());
        if (user.getStatus() != null) {
            fields.put(FIELD_STATUS, String.valueOf(user.getStatus()));
        }
        return normalizeFields(fields);
    }

    private void putIfNotNull(Map<String, String> fields, String key, String value) {
        if (value != null) {
            fields.put(key, value);
        }
    }

    private Map<String, String> normalizeFields(Map<String, String> fields) {
        Map<String, String> normalized = new HashMap<>();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            normalized.put(entry.getKey(), entry.getValue());
        }
        return normalized;
    }

    private UserCache fromMap(Map<Object, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        Long id = parseLong(map.get(FIELD_ID));
        String username = parseString(map.get(FIELD_USERNAME));
        String phone = parseString(map.get(FIELD_PHONE));
        String nickname = parseString(map.get(FIELD_NICKNAME));
        String avatarUrl = parseString(map.get(FIELD_AVATAR));
        String email = parseString(map.get(FIELD_EMAIL));
        Integer status = parseInteger(map.get(FIELD_STATUS));
        return new UserCache(id, username, phone, nickname, avatarUrl, email, status);
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (Exception ex) {
            return null;
        }
    }

    private Integer parseInteger(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception ex) {
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

    private void renewTtl(String key) {
        if (StrUtil.isBlank(key)) {
            return;
        }
        try {
            redisTemplate.expire(key, ttl());
        } catch (Exception ex) {
            log.warn("Renew user cache ttl failed: key={}", key, ex);
        }
    }

    private Duration ttl() {
        long seconds = Math.max(60L, ttlSeconds);
        return Duration.ofSeconds(seconds);
    }

    public record UserCache(Long id, String username, String phone, String nickname,
                            String avatarUrl, String email, Integer status) {
    }
}
