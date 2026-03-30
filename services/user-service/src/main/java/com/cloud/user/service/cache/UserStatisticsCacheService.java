package com.cloud.user.service.cache;

import com.cloud.common.domain.vo.user.UserStatisticsVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatisticsCacheService {

  private static final String PREFIX = "user:statistics:";

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

  @Value("${user.cache.statistics.ttl-seconds:300}")
  private long ttlSeconds;

  public UserStatisticsVO getOverview() {
    return get(overviewKey(), UserStatisticsVO.class);
  }

  public void putOverview(UserStatisticsVO overview) {
    put(overviewKey(), overview);
  }

  public Map<LocalDate, Long> getRegistrationTrend(LocalDate startDate, LocalDate endDate) {
    return get(
        registrationTrendKey(startDate, endDate), new TypeReference<Map<LocalDate, Long>>() {});
  }

  public void putRegistrationTrend(
      LocalDate startDate, LocalDate endDate, Map<LocalDate, Long> value) {
    put(registrationTrendKey(startDate, endDate), value);
  }

  public Map<String, Long> getRoleDistribution() {
    return get(roleDistributionKey(), new TypeReference<Map<String, Long>>() {});
  }

  public void putRoleDistribution(Map<String, Long> value) {
    put(roleDistributionKey(), value);
  }

  public Map<String, Long> getStatusDistribution() {
    return get(statusDistributionKey(), new TypeReference<Map<String, Long>>() {});
  }

  public void putStatusDistribution(Map<String, Long> value) {
    put(statusDistributionKey(), value);
  }

  public Long getActiveUsers(Integer days) {
    return get(activeUsersKey(days), Long.class);
  }

  public void putActiveUsers(Integer days, Long value) {
    put(activeUsersKey(days), value);
  }

  public void clearAll() {
    try {
      Set<String> keys = redisTemplate.keys(PREFIX + "*");
      if (keys != null && !keys.isEmpty()) {
        redisTemplate.delete(keys);
      }
    } catch (Exception ex) {
      log.warn("Clear user statistics cache failed", ex);
    }
  }

  private <T> T get(String key, Class<T> type) {
    try {
      String json = redisTemplate.opsForValue().get(key);
      if (json == null || json.isBlank()) {
        return null;
      }
      redisTemplate.expire(key, ttl());
      return objectMapper.readValue(json, type);
    } catch (Exception ex) {
      log.warn("Read statistics cache failed: key={}", key, ex);
      return null;
    }
  }

  private <T> T get(String key, TypeReference<T> typeReference) {
    try {
      String json = redisTemplate.opsForValue().get(key);
      if (json == null || json.isBlank()) {
        return null;
      }
      redisTemplate.expire(key, ttl());
      return objectMapper.readValue(json, typeReference);
    } catch (Exception ex) {
      log.warn("Read statistics cache failed: key={}", key, ex);
      return null;
    }
  }

  private void put(String key, Object value) {
    if (value == null) {
      return;
    }
    try {
      redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl());
    } catch (Exception ex) {
      log.warn("Write statistics cache failed: key={}", key, ex);
    }
  }

  private String overviewKey() {
    return PREFIX + "overview";
  }

  private String registrationTrendKey(LocalDate startDate, LocalDate endDate) {
    return PREFIX + "registration:" + startDate + ":" + endDate;
  }

  private String roleDistributionKey() {
    return PREFIX + "role_distribution";
  }

  private String statusDistributionKey() {
    return PREFIX + "status_distribution";
  }

  private String activeUsersKey(Integer days) {
    int safeDays = days == null || days <= 0 ? 7 : days;
    return PREFIX + "active:" + safeDays;
  }

  private Duration ttl() {
    return Duration.ofSeconds(Math.max(60L, ttlSeconds));
  }
}
