package com.cloud.user.service.cache;

import cn.hutool.core.util.StrUtil;
import com.cloud.user.module.entity.UserAddress;
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
public class TransactionalUserAddressCacheService {

  private static final String KEY_ID_PREFIX = "user:address:";
  private static final String KEY_USER_PREFIX = "user:address:user:";

  private static final String FIELD_ID = "id";
  private static final String FIELD_USER_ID = "userId";
  private static final String FIELD_CONSIGNEE = "consignee";
  private static final String FIELD_PHONE = "phone";
  private static final String FIELD_PROVINCE = "province";
  private static final String FIELD_CITY = "city";
  private static final String FIELD_DISTRICT = "district";
  private static final String FIELD_STREET = "street";
  private static final String FIELD_DETAIL_ADDRESS = "detailAddress";
  private static final String FIELD_IS_DEFAULT = "isDefault";
  private static final String FIELD_IDS = "ids";

  private final StringRedisTemplate redisTemplate;

  @Value("${user.address.cache.ttl-seconds:1800}")
  private long ttlSeconds;

  public UserAddressCache getById(Long id) {
    if (id == null) {
      return null;
    }
    return getAddressFromRedis(idKey(id));
  }

  public List<UserAddressCache> getByUserId(Long userId) {
    if (userId == null) {
      return List.of();
    }

    try {
      Map<Object, Object> entries = redisTemplate.opsForHash().entries(userKey(userId));
      if (entries == null || entries.isEmpty()) {
        return List.of();
      }
      redisTemplate.expire(userKey(userId), ttl());
      String ids = parseString(entries.get(FIELD_IDS));
      if (StrUtil.isBlank(ids)) {
        return List.of();
      }

      List<UserAddressCache> result = new ArrayList<>();
      for (String idText : ids.split(",")) {
        if (StrUtil.isBlank(idText)) {
          continue;
        }
        try {
          UserAddressCache cache = getById(Long.parseLong(idText));
          if (cache != null) {
            result.add(cache);
          }
        } catch (NumberFormatException ignored) {
        }
      }
      return result;
    } catch (Exception ex) {
      log.warn("Read user address list cache failed: userId={}", userId, ex);
      return List.of();
    }
  }

  @Transactional(rollbackFor = Exception.class)
  public void putTransactional(UserAddress address) {
    put(address);
  }

  public void put(UserAddress address) {
    if (address == null || address.getId() == null) {
      return;
    }

    Map<String, String> fields = buildFieldMap(address);
    if (fields.isEmpty()) {
      return;
    }

    try {
      HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
      hashOperations.putAll(idKey(address.getId()), fields);
      redisTemplate.expire(idKey(address.getId()), ttl());
    } catch (Exception ex) {
      log.warn("Write user address cache failed: addressId={}", address.getId(), ex);
    }
  }

  @Transactional(rollbackFor = Exception.class)
  public void putUserListTransactional(Long userId, List<UserAddress> addresses) {
    putUserList(userId, addresses);
  }

  public void putUserList(Long userId, List<UserAddress> addresses) {
    if (userId == null || addresses == null || addresses.isEmpty()) {
      return;
    }

    try {
      for (UserAddress address : addresses) {
        put(address);
      }
      Map<String, String> payload = new HashMap<>();
      payload.put(
          FIELD_IDS,
          addresses.stream()
              .map(UserAddress::getId)
              .filter(id -> id != null)
              .map(String::valueOf)
              .reduce((left, right) -> left + "," + right)
              .orElse(""));
      if (StrUtil.isBlank(payload.get(FIELD_IDS))) {
        return;
      }
      redisTemplate.opsForHash().putAll(userKey(userId), payload);
      redisTemplate.expire(userKey(userId), ttl());
    } catch (Exception ex) {
      log.warn("Write user address list cache failed: userId={}", userId, ex);
    }
  }

  @Transactional(rollbackFor = Exception.class)
  public void evictTransactional(Long id, Long userId) {
    evict(id, userId);
  }

  public void evict(Long id, Long userId) {
    try {
      List<String> keys = new ArrayList<>(2);
      if (id != null) {
        keys.add(idKey(id));
      }
      if (userId != null) {
        keys.add(userKey(userId));
      }
      if (!keys.isEmpty()) {
        redisTemplate.delete(keys);
      }
    } catch (Exception ex) {
      log.warn("Evict user address cache failed: addressId={}, userId={}", id, userId, ex);
    }
  }

  public void evictUserList(Long userId) {
    evict(null, userId);
  }

  private UserAddressCache getAddressFromRedis(String key) {
    try {
      Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
      if (entries == null || entries.isEmpty()) {
        return null;
      }
      redisTemplate.expire(key, ttl());
      return fromMap(entries);
    } catch (Exception ex) {
      log.warn("Read user address cache failed: key={}", key, ex);
      return null;
    }
  }

  private Map<String, String> buildFieldMap(UserAddress address) {
    Map<String, String> fields = new HashMap<>();
    if (address.getId() != null) {
      fields.put(FIELD_ID, String.valueOf(address.getId()));
    }
    if (address.getUserId() != null) {
      fields.put(FIELD_USER_ID, String.valueOf(address.getUserId()));
    }
    putIfNotBlank(fields, FIELD_CONSIGNEE, address.getConsignee());
    putIfNotBlank(fields, FIELD_PHONE, address.getPhone());
    putIfNotBlank(fields, FIELD_PROVINCE, address.getProvince());
    putIfNotBlank(fields, FIELD_CITY, address.getCity());
    putIfNotBlank(fields, FIELD_DISTRICT, address.getDistrict());
    putIfNotBlank(fields, FIELD_STREET, address.getStreet());
    putIfNotBlank(fields, FIELD_DETAIL_ADDRESS, address.getDetailAddress());
    if (address.getIsDefault() != null) {
      fields.put(FIELD_IS_DEFAULT, String.valueOf(address.getIsDefault()));
    }
    return fields;
  }

  private void putIfNotBlank(Map<String, String> fields, String key, String value) {
    if (StrUtil.isNotBlank(value)) {
      fields.put(key, value);
    }
  }

  private UserAddressCache fromMap(Map<Object, Object> map) {
    Long id = parseLong(map.get(FIELD_ID));
    Long userId = parseLong(map.get(FIELD_USER_ID));
    if (id == null || userId == null) {
      return null;
    }
    return new UserAddressCache(
        id,
        userId,
        parseString(map.get(FIELD_CONSIGNEE)),
        parseString(map.get(FIELD_PHONE)),
        parseString(map.get(FIELD_PROVINCE)),
        parseString(map.get(FIELD_CITY)),
        parseString(map.get(FIELD_DISTRICT)),
        parseString(map.get(FIELD_STREET)),
        parseString(map.get(FIELD_DETAIL_ADDRESS)),
        parseInteger(map.get(FIELD_IS_DEFAULT)));
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

  private String userKey(Long userId) {
    return KEY_USER_PREFIX + userId;
  }

  private Duration ttl() {
    return Duration.ofSeconds(Math.max(60L, ttlSeconds));
  }

  public record UserAddressCache(
      Long id,
      Long userId,
      String consignee,
      String phone,
      String province,
      String city,
      String district,
      String street,
      String detailAddress,
      Integer isDefault) {}
}
