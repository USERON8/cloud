package com.cloud.auth.service.support;

import cn.hutool.core.util.StrUtil;
import com.cloud.auth.module.model.OAuthAccountRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OAuthAccountCacheService {

  private static final String KEY_PREFIX = "auth:oauth:account:";

  @Qualifier("oauth2MainRedisTemplate")
  private final RedisTemplate<String, Object> redisTemplate;

  public OAuthAccountRecord getByProviderUserId(String provider, String providerUserId) {
    String key = buildKey(provider, providerUserId);
    if (key == null) {
      return null;
    }
    Object value = redisTemplate.opsForValue().get(key);
    if (value instanceof OAuthAccountRecord record) {
      return record;
    }
    return null;
  }

  public void save(OAuthAccountRecord record) {
    if (record == null) {
      return;
    }
    String key = buildKey(record.getProvider(), record.getProviderUserId());
    if (key == null) {
      return;
    }
    redisTemplate.opsForValue().set(key, record);
  }

  public void delete(String provider, String providerUserId) {
    String key = buildKey(provider, providerUserId);
    if (key == null) {
      return;
    }
    redisTemplate.delete(key);
  }

  private String buildKey(String provider, String providerUserId) {
    if (StrUtil.isBlank(provider) || StrUtil.isBlank(providerUserId)) {
      return null;
    }
    return KEY_PREFIX + provider.trim().toLowerCase() + ":" + providerUserId.trim();
  }
}
