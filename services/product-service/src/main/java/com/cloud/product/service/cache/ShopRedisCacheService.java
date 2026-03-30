package com.cloud.product.service.cache;

import com.cloud.common.result.PageResult;
import com.cloud.product.module.dto.ShopPageDTO;
import com.cloud.product.module.vo.ShopVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopRedisCacheService {

  private static final String PREFIX = "product:shop:";
  private static final String ID_PREFIX = PREFIX + "id:";
  private static final String PAGE_PREFIX = PREFIX + "page:";
  private static final String MERCHANT_PREFIX = PREFIX + "merchant:";
  private static final String SEARCH_PREFIX = PREFIX + "search:";
  private static final String STATS_PREFIX = PREFIX + "stats:";
  private static final String PERMISSION_PREFIX = PREFIX + "permission:";

  private final StringRedisTemplate stringRedisTemplate;
  private final ObjectMapper objectMapper;
  private final TaskScheduler taskScheduler;

  @Value("${product.cache.shop.ttl-seconds:1800}")
  private long ttlSeconds;

  @Value("${product.cache.shop.delayed-double-delete-ms:500}")
  private long delayedDoubleDeleteMs;

  public ShopVO getById(Long id) {
    if (id == null) {
      return null;
    }
    return get(idKey(id), ShopVO.class);
  }

  public void putById(Long id, ShopVO value) {
    if (id == null) {
      return;
    }
    put(idKey(id), value);
  }

  public PageResult<ShopVO> getPage(ShopPageDTO pageDTO) {
    return get(pageKey(pageDTO), new TypeReference<PageResult<ShopVO>>() {});
  }

  public void putPage(ShopPageDTO pageDTO, PageResult<ShopVO> value) {
    put(pageKey(pageDTO), value);
  }

  public List<ShopVO> getMerchantList(Long merchantId, Integer status) {
    return get(merchantKey(merchantId, status), new TypeReference<List<ShopVO>>() {});
  }

  public void putMerchantList(Long merchantId, Integer status, List<ShopVO> value) {
    put(merchantKey(merchantId, status), value);
  }

  public List<ShopVO> getSearchList(String shopName, Integer status) {
    return get(searchKey(shopName, status), new TypeReference<List<ShopVO>>() {});
  }

  public void putSearchList(String shopName, Integer status, List<ShopVO> value) {
    put(searchKey(shopName, status), value);
  }

  public Long getStat(String statKey) {
    return get(STATS_PREFIX + statKey, Long.class);
  }

  public void putStat(String statKey, Long value) {
    put(STATS_PREFIX + statKey, value);
  }

  public Boolean getPermission(Long merchantId, Long shopId) {
    return get(permissionKey(merchantId, shopId), Boolean.class);
  }

  public void putPermission(Long merchantId, Long shopId, Boolean value) {
    put(permissionKey(merchantId, shopId), value);
  }

  public void evictById(Long id) {
    if (id == null) {
      return;
    }
    evictByIdNow(id);
  }

  public void evictByIdAfterCommit(Long id) {
    if (id == null) {
      return;
    }
    runAfterCommit(
        () -> {
          evictByIdNow(id);
          scheduleDelayedDeleteById(id);
        });
  }

  private void evictByIdNow(Long id) {
    try {
      stringRedisTemplate.delete(idKey(id));
    } catch (Exception ex) {
      log.warn("Evict shop cache failed: id={}", id, ex);
    }
  }

  public void clearAll() {
    clearAllNow();
  }

  public void clearAllAfterCommit() {
    runAfterCommit(
        () -> {
          clearAllNow();
          scheduleDelayedClearAll();
        });
  }

  private void clearAllNow() {
    try {
      Set<String> keys = stringRedisTemplate.keys(PREFIX + "*");
      if (keys != null && !keys.isEmpty()) {
        stringRedisTemplate.delete(keys);
      }
    } catch (Exception ex) {
      log.warn("Clear shop cache failed", ex);
    }
  }

  private <T> T get(String key, Class<T> type) {
    try {
      String json = stringRedisTemplate.opsForValue().get(key);
      if (json == null || json.isBlank()) {
        return null;
      }
      stringRedisTemplate.expire(key, ttl());
      return objectMapper.readValue(json, type);
    } catch (Exception ex) {
      log.warn("Read shop cache failed: key={}", key, ex);
      return null;
    }
  }

  private <T> T get(String key, TypeReference<T> typeReference) {
    try {
      String json = stringRedisTemplate.opsForValue().get(key);
      if (json == null || json.isBlank()) {
        return null;
      }
      stringRedisTemplate.expire(key, ttl());
      return objectMapper.readValue(json, typeReference);
    } catch (Exception ex) {
      log.warn("Read shop cache failed: key={}", key, ex);
      return null;
    }
  }

  private void put(String key, Object value) {
    if (value == null) {
      return;
    }
    try {
      stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl());
    } catch (Exception ex) {
      log.warn("Write shop cache failed: key={}", key, ex);
    }
  }

  private String idKey(Long id) {
    return ID_PREFIX + id;
  }

  private String pageKey(ShopPageDTO pageDTO) {
    ShopPageDTO request = pageDTO == null ? new ShopPageDTO() : pageDTO;
    return PAGE_PREFIX
        + safeLong(request.getCurrent(), 1L)
        + ":"
        + safeLong(request.getSize(), 20L)
        + ":"
        + safeString(request.getMerchantId())
        + ":"
        + safeString(request.getShopNameKeyword())
        + ":"
        + safeString(request.getAddressKeyword())
        + ":"
        + safeString(request.getStatus())
        + ":"
        + safeString(request.getCreateTimeSort())
        + ":"
        + safeString(request.getUpdateTimeSort());
  }

  private String merchantKey(Long merchantId, Integer status) {
    return MERCHANT_PREFIX + safeString(merchantId) + ":" + safeString(status);
  }

  private String searchKey(String shopName, Integer status) {
    return SEARCH_PREFIX + safeString(shopName) + ":" + safeString(status);
  }

  private String permissionKey(Long merchantId, Long shopId) {
    return PERMISSION_PREFIX + safeString(merchantId) + ":" + safeString(shopId);
  }

  private String safeString(Object value) {
    if (value == null) {
      return "null";
    }
    String raw = String.valueOf(value).trim();
    return raw.isEmpty() ? "null" : raw;
  }

  private Long safeLong(Long value, Long fallback) {
    return value == null || value <= 0 ? fallback : value;
  }

  private Duration ttl() {
    return Duration.ofSeconds(Math.max(60L, ttlSeconds));
  }

  private void runAfterCommit(Runnable task) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              task.run();
            }
          });
      return;
    }
    task.run();
  }

  private void scheduleDelayedDeleteById(Long id) {
    long delayMs = Math.max(0L, delayedDoubleDeleteMs);
    if (delayMs <= 0L) {
      return;
    }
    taskScheduler.schedule(() -> evictByIdNow(id), Instant.now().plusMillis(delayMs));
  }

  private void scheduleDelayedClearAll() {
    long delayMs = Math.max(0L, delayedDoubleDeleteMs);
    if (delayMs <= 0L) {
      return;
    }
    taskScheduler.schedule(this::clearAllNow, Instant.now().plusMillis(delayMs));
  }
}
