package com.cloud.common.config;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SecurityRedisAutoConfigurationTest {

  @Test
  void securitySerializerSupportsJavaTimeValues() {
    var serializer = SecurityRedisAutoConfiguration.securityValueSerializer();

    assertThatCode(
            () -> serializer.serialize(Map.of("blacklistedAt", Instant.parse("2026-09-28T00:00:00Z"))))
        .doesNotThrowAnyException();
  }
}
