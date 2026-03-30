package com.cloud.user.service.cache;

import com.cloud.user.module.entity.MerchantAuth;
import java.time.Duration;
import java.time.LocalDateTime;
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
public class TransactionalMerchantAuthCacheService {

  private static final String KEY_ID_PREFIX = "merchant:auth:";
  private static final String KEY_MERCHANT_ID_PREFIX = "merchant:auth:merchant:";

  private static final String FIELD_ID = "id";
  private static final String FIELD_MERCHANT_ID = "merchantId";
  private static final String FIELD_BUSINESS_LICENSE_NUMBER = "businessLicenseNumber";
  private static final String FIELD_BUSINESS_LICENSE = "businessLicenseUrl";
  private static final String FIELD_ID_CARD_FRONT_URL = "idCardFrontUrl";
  private static final String FIELD_ID_CARD_BACK_URL = "idCardBackUrl";
  private static final String FIELD_CONTACT_PHONE = "contactPhone";
  private static final String FIELD_CONTACT_ADDRESS = "contactAddress";
  private static final String FIELD_AUTH_STATUS = "authStatus";
  private static final String FIELD_AUTH_REMARK = "authRemark";
  private static final String FIELD_CREATED_AT = "createdAt";
  private static final String FIELD_UPDATED_AT = "updatedAt";

  private final StringRedisTemplate redisTemplate;

  @Value("${user.cache.merchant-auth.ttl-seconds:1800}")
  private long ttlSeconds;

  public MerchantAuthCache getById(Long id) {
    if (id == null) {
      return null;
    }
    return getFromRedis(idKey(id));
  }

  public MerchantAuthCache getByMerchantId(Long merchantId) {
    if (merchantId == null) {
      return null;
    }
    return getFromRedis(merchantIdKey(merchantId));
  }

  @Transactional(rollbackFor = Exception.class)
  public void putTransactional(MerchantAuth merchantAuth) {
    put(merchantAuth);
  }

  public void put(MerchantAuth merchantAuth) {
    if (merchantAuth == null
        || merchantAuth.getId() == null
        || merchantAuth.getMerchantId() == null) {
      return;
    }

    Map<String, String> fields = buildFieldMap(merchantAuth);
    if (fields.isEmpty()) {
      return;
    }

    try {
      HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
      hashOperations.putAll(idKey(merchantAuth.getId()), fields);
      redisTemplate.expire(idKey(merchantAuth.getId()), ttl());
      hashOperations.putAll(merchantIdKey(merchantAuth.getMerchantId()), fields);
      redisTemplate.expire(merchantIdKey(merchantAuth.getMerchantId()), ttl());
    } catch (Exception ex) {
      log.warn("Write merchant auth cache failed: merchantAuthId={}", merchantAuth.getId(), ex);
    }
  }

  @Transactional(rollbackFor = Exception.class)
  public void evictTransactional(Long id, Long merchantId) {
    evict(id, merchantId);
  }

  public void evict(Long id, Long merchantId) {
    try {
      List<String> keys = new ArrayList<>(2);
      if (id != null) {
        keys.add(idKey(id));
      }
      if (merchantId != null) {
        keys.add(merchantIdKey(merchantId));
      }
      if (!keys.isEmpty()) {
        redisTemplate.delete(keys);
      }
    } catch (Exception ex) {
      log.warn("Evict merchant auth cache failed: merchantAuthId={}", id, ex);
    }
  }

  private MerchantAuthCache getFromRedis(String key) {
    try {
      Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
      if (entries == null || entries.isEmpty()) {
        return null;
      }
      redisTemplate.expire(key, ttl());
      return fromMap(entries);
    } catch (Exception ex) {
      log.warn("Read merchant auth cache failed: key={}", key, ex);
      return null;
    }
  }

  private Map<String, String> buildFieldMap(MerchantAuth merchantAuth) {
    Map<String, String> fields = new HashMap<>();
    fields.put(FIELD_ID, String.valueOf(merchantAuth.getId()));
    fields.put(FIELD_MERCHANT_ID, String.valueOf(merchantAuth.getMerchantId()));
    putIfNotBlank(fields, FIELD_BUSINESS_LICENSE_NUMBER, merchantAuth.getBusinessLicenseNumber());
    putIfNotBlank(fields, FIELD_BUSINESS_LICENSE, merchantAuth.getBusinessLicenseUrl());
    putIfNotBlank(fields, FIELD_ID_CARD_FRONT_URL, merchantAuth.getIdCardFrontUrl());
    putIfNotBlank(fields, FIELD_ID_CARD_BACK_URL, merchantAuth.getIdCardBackUrl());
    putIfNotBlank(fields, FIELD_CONTACT_PHONE, merchantAuth.getContactPhone());
    putIfNotBlank(fields, FIELD_CONTACT_ADDRESS, merchantAuth.getContactAddress());
    if (merchantAuth.getAuthStatus() != null) {
      fields.put(FIELD_AUTH_STATUS, String.valueOf(merchantAuth.getAuthStatus()));
    }
    putIfNotBlank(fields, FIELD_AUTH_REMARK, merchantAuth.getAuthRemark());
    putIfNotBlank(fields, FIELD_CREATED_AT, formatTime(merchantAuth.getCreatedAt()));
    putIfNotBlank(fields, FIELD_UPDATED_AT, formatTime(merchantAuth.getUpdatedAt()));
    return fields;
  }

  private MerchantAuthCache fromMap(Map<Object, Object> map) {
    Long id = parseLong(map.get(FIELD_ID));
    Long merchantId = parseLong(map.get(FIELD_MERCHANT_ID));
    if (id == null || merchantId == null) {
      return null;
    }
    return new MerchantAuthCache(
        id,
        merchantId,
        parseString(map.get(FIELD_BUSINESS_LICENSE_NUMBER)),
        parseString(map.get(FIELD_BUSINESS_LICENSE)),
        parseString(map.get(FIELD_ID_CARD_FRONT_URL)),
        parseString(map.get(FIELD_ID_CARD_BACK_URL)),
        parseString(map.get(FIELD_CONTACT_PHONE)),
        parseString(map.get(FIELD_CONTACT_ADDRESS)),
        parseInteger(map.get(FIELD_AUTH_STATUS)),
        parseString(map.get(FIELD_AUTH_REMARK)),
        parseTime(map.get(FIELD_CREATED_AT)),
        parseTime(map.get(FIELD_UPDATED_AT)));
  }

  private void putIfNotBlank(Map<String, String> map, String key, String value) {
    if (value != null && !value.isBlank()) {
      map.put(key, value);
    }
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

  private LocalDateTime parseTime(Object value) {
    if (value == null) {
      return null;
    }
    try {
      return LocalDateTime.parse(value.toString());
    } catch (Exception ex) {
      return null;
    }
  }

  private String parseString(Object value) {
    return value == null ? null : value.toString();
  }

  private String formatTime(LocalDateTime value) {
    return value == null ? null : value.toString();
  }

  private String idKey(Long id) {
    return KEY_ID_PREFIX + id;
  }

  private String merchantIdKey(Long merchantId) {
    return KEY_MERCHANT_ID_PREFIX + merchantId;
  }

  private Duration ttl() {
    return Duration.ofSeconds(Math.max(60L, ttlSeconds));
  }

  public record MerchantAuthCache(
      Long id,
      Long merchantId,
      String businessLicenseNumber,
      String businessLicenseUrl,
      String idCardFrontUrl,
      String idCardBackUrl,
      String contactPhone,
      String contactAddress,
      Integer authStatus,
      String authRemark,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {}
}
