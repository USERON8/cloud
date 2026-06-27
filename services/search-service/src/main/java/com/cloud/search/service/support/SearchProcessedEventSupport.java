package com.cloud.search.service.support;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.springframework.data.redis.core.StringRedisTemplate;

public final class SearchProcessedEventSupport {

  private static final long PROCESSED_EVENT_TTL_SECONDS = 24 * 60 * 60;
  private static final int PROCESSED_LOOKBACK_DAYS = 1;
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

  private SearchProcessedEventSupport() {}

  public static boolean isProcessed(
      StringRedisTemplate redisTemplate, String bucketPrefix, String traceId, Logger log) {
    if (traceId == null || traceId.isBlank()) {
      return false;
    }
    try {
      for (int i = 0; i <= PROCESSED_LOOKBACK_DAYS; i++) {
        Boolean exists =
            redisTemplate.opsForHash().hasKey(buildBucketKey(bucketPrefix, i), traceId);
        if (Boolean.TRUE.equals(exists)) {
          return true;
        }
      }
      return false;
    } catch (Exception ex) {
      log.warn(
          "Check processed search event failed: bucketPrefix={}, traceId={}",
          bucketPrefix,
          traceId,
          ex);
      return false;
    }
  }

  public static void markProcessed(
      StringRedisTemplate redisTemplate, String bucketPrefix, String traceId, Logger log) {
    if (traceId == null || traceId.isBlank()) {
      return;
    }
    try {
      String bucketKey = buildBucketKey(bucketPrefix, 0);
      redisTemplate.opsForHash().put(bucketKey, traceId, "1");
      redisTemplate.expire(bucketKey, PROCESSED_EVENT_TTL_SECONDS, TimeUnit.SECONDS);
    } catch (Exception ex) {
      log.warn(
          "Mark processed search event failed: bucketPrefix={}, traceId={}",
          bucketPrefix,
          traceId,
          ex);
    }
  }

  private static String buildBucketKey(String bucketPrefix, int offsetDays) {
    LocalDate date = LocalDate.now().minusDays(offsetDays);
    return bucketPrefix + date.format(DATE_FORMATTER);
  }
}
