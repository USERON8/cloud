package com.cloud.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.example.redis.UnsafeRedisValue;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

class RedisConfigTest {

  private final RedisConfig redisConfig = new RedisConfig();

  @Test
  void jsonRedisSerializerShouldRoundTripCloudDtosAndCommonContainers() {
    GenericJackson2JsonRedisSerializer serializer = redisConfig.jsonRedisSerializer();
    CachePayload payload =
        new CachePayload("order-1", BigDecimal.valueOf(19.99), List.of("PAID", "LOCKED"));
    payload.setMetadata(Map.of("count", 2L));

    byte[] bytes = serializer.serialize(payload);
    Object restored = serializer.deserialize(bytes);

    assertThat(restored).isInstanceOf(CachePayload.class);
    assertThat(restored).usingRecursiveComparison().isEqualTo(payload);
  }

  @Test
  void jsonRedisSerializerShouldRejectTypesOutsideTheAllowList() {
    GenericJackson2JsonRedisSerializer serializer = redisConfig.jsonRedisSerializer();

    byte[] bytes = serializer.serialize(new UnsafeRedisValue("blocked"));

    assertThatThrownBy(() -> serializer.deserialize(bytes))
        .isInstanceOf(SerializationException.class)
        .hasMessageContaining("Could not read JSON");
  }

  private static class CachePayload {

    private String orderNo;
    private BigDecimal amount;
    private List<String> statuses;
    private Map<String, Long> metadata;

    private CachePayload() {}

    private CachePayload(String orderNo, BigDecimal amount, List<String> statuses) {
      this.orderNo = orderNo;
      this.amount = amount;
      this.statuses = statuses;
    }

    public String getOrderNo() {
      return orderNo;
    }

    public BigDecimal getAmount() {
      return amount;
    }

    public List<String> getStatuses() {
      return statuses;
    }

    public Map<String, Long> getMetadata() {
      return metadata;
    }

    public void setMetadata(Map<String, Long> metadata) {
      this.metadata = metadata;
    }
  }
}
