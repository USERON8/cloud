package com.cloud.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class CacheTtlSupportTest {

  @Test
  void shouldUseDefaultMinimumWhenTtlIsTooSmall() {
    assertEquals(Duration.ofSeconds(60), CacheTtlSupport.ttlDuration(5));
  }

  @Test
  void shouldUseProvidedTtlWhenItExceedsMinimum() {
    assertEquals(Duration.ofSeconds(120), CacheTtlSupport.ttlDuration(120));
  }

  @Test
  void shouldRespectCustomMinimum() {
    assertEquals(Duration.ofSeconds(30), CacheTtlSupport.ttlDuration(10, 30));
  }
}
