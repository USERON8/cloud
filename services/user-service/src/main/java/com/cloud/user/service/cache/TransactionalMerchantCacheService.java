package com.cloud.user.service.cache;

import cn.hutool.core.util.StrUtil;
import com.cloud.user.module.entity.Merchant;
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
public class TransactionalMerchantCacheService {

  private static final String KEY_ID_PREFIX = "merchant:info:";
  private static final String KEY_NAME_PREFIX = "merchant:info:name:";
  private static final String KEY_MERCHANT_NAME_PREFIX = "merchant:info:merchant-name:";

  private static final String FIELD_ID = "id";
  private static final String FIELD_USERNAME = "username";
  private static final String FIELD_MERCHANT_NAME = "merchantName";
  private static final String FIELD_PHONE = "phone";
  private static final String FIELD_OWNER_USER_ID = "ownerUserId";
  private static final String FIELD_STATUS = "status";
  private static final String FIELD_AUDIT_STATUS = "auditStatus";

  private final StringRedisTemplate redisTemplate;

  @Value("${user.cache.merchant.ttl-seconds:1800}")
  private long ttlSeconds;

  public MerchantCache getById(Long id) {
    if (id == null) {
      return null;
    }
    return getFromRedis(idKey(id));
  }

  public MerchantCache getByUsername(String username) {
    if (StrUtil.isBlank(username)) {
      return null;
    }
    return getFromRedis(nameKey(username));
  }

  public MerchantCache getByMerchantName(String merchantName) {
    if (StrUtil.isBlank(merchantName)) {
      return null;
    }
    return getFromRedis(merchantNameKey(merchantName));
  }

  @Transactional(rollbackFor = Exception.class)
  public void putTransactional(Merchant merchant) {
    put(merchant);
  }

  public void put(Merchant merchant) {
    if (merchant == null || merchant.getId() == null) {
      return;
    }

    Map<String, String> fields = buildFieldMap(merchant);
    if (fields.isEmpty()) {
      return;
    }

    try {
      HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
      hashOperations.putAll(idKey(merchant.getId()), fields);
      redisTemplate.expire(idKey(merchant.getId()), ttl());

      if (StrUtil.isNotBlank(merchant.getUsername())) {
        hashOperations.putAll(nameKey(merchant.getUsername()), fields);
        redisTemplate.expire(nameKey(merchant.getUsername()), ttl());
      }
      if (StrUtil.isNotBlank(merchant.getMerchantName())) {
        hashOperations.putAll(merchantNameKey(merchant.getMerchantName()), fields);
        redisTemplate.expire(merchantNameKey(merchant.getMerchantName()), ttl());
      }
    } catch (Exception ex) {
      log.warn("Write merchant cache failed: merchantId={}", merchant.getId(), ex);
    }
  }

  @Transactional(rollbackFor = Exception.class)
  public void evictTransactional(Long id, String username, String merchantName) {
    evict(id, username, merchantName);
  }

  public void evict(Long id, String username, String merchantName) {
    try {
      List<String> keys = new ArrayList<>(3);
      if (id != null) {
        keys.add(idKey(id));
      }
      if (StrUtil.isNotBlank(username)) {
        keys.add(nameKey(username));
      }
      if (StrUtil.isNotBlank(merchantName)) {
        keys.add(merchantNameKey(merchantName));
      }
      if (!keys.isEmpty()) {
        redisTemplate.delete(keys);
      }
    } catch (Exception ex) {
      log.warn("Evict merchant cache failed: merchantId={}", id, ex);
    }
  }

  public void clearAll() {
    deleteByPattern(KEY_ID_PREFIX + "*");
    deleteByPattern(KEY_NAME_PREFIX + "*");
    deleteByPattern(KEY_MERCHANT_NAME_PREFIX + "*");
  }

  private void deleteByPattern(String pattern) {
    try {
      java.util.Set<String> keys = redisTemplate.keys(pattern);
      if (keys != null && !keys.isEmpty()) {
        redisTemplate.delete(keys);
      }
    } catch (Exception ex) {
      log.warn("Clear merchant cache by pattern failed: pattern={}", pattern, ex);
    }
  }

  private MerchantCache getFromRedis(String key) {
    try {
      Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
      if (entries == null || entries.isEmpty()) {
        return null;
      }
      redisTemplate.expire(key, ttl());
      return fromMap(entries);
    } catch (Exception ex) {
      log.warn("Read merchant cache failed: key={}", key, ex);
      return null;
    }
  }

  private Map<String, String> buildFieldMap(Merchant merchant) {
    Map<String, String> fields = new HashMap<>();
    fields.put(FIELD_ID, String.valueOf(merchant.getId()));
    putIfNotBlank(fields, FIELD_USERNAME, merchant.getUsername());
    putIfNotBlank(fields, FIELD_MERCHANT_NAME, merchant.getMerchantName());
    putIfNotBlank(fields, FIELD_PHONE, merchant.getPhone());
    if (merchant.getOwnerUserId() != null) {
      fields.put(FIELD_OWNER_USER_ID, String.valueOf(merchant.getOwnerUserId()));
    }
    if (merchant.getStatus() != null) {
      fields.put(FIELD_STATUS, String.valueOf(merchant.getStatus()));
    }
    if (merchant.getAuditStatus() != null) {
      fields.put(FIELD_AUDIT_STATUS, String.valueOf(merchant.getAuditStatus()));
    }
    return fields;
  }

  private void putIfNotBlank(Map<String, String> map, String key, String value) {
    if (StrUtil.isNotBlank(value)) {
      map.put(key, value);
    }
  }

  private MerchantCache fromMap(Map<Object, Object> map) {
    Long id = parseLong(map.get(FIELD_ID));
    if (id == null) {
      return null;
    }
    return new MerchantCache(
        id,
        parseString(map.get(FIELD_USERNAME)),
        parseString(map.get(FIELD_MERCHANT_NAME)),
        parseString(map.get(FIELD_PHONE)),
        parseLong(map.get(FIELD_OWNER_USER_ID)),
        parseInteger(map.get(FIELD_STATUS)),
        parseInteger(map.get(FIELD_AUDIT_STATUS)));
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

  private String merchantNameKey(String merchantName) {
    return KEY_MERCHANT_NAME_PREFIX + merchantName;
  }

  private Duration ttl() {
    return Duration.ofSeconds(Math.max(60L, ttlSeconds));
  }

  public record MerchantCache(
      Long id,
      String username,
      String merchantName,
      String phone,
      Long ownerUserId,
      Integer status,
      Integer auditStatus) {}
}
