package com.cloud.stock.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class AsyncConfigTest {

  private final AsyncConfig asyncConfig = new AsyncConfig();

  @Test
  void stockQueryExecutorShouldScaleWithCpu() {
    ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) asyncConfig.stockQueryExecutor();
    int processors = Runtime.getRuntime().availableProcessors();
    assertEquals("stock-query-", pool.getThreadNamePrefix());
    assertEquals(Math.max(4, processors), pool.getCorePoolSize());
    assertEquals(Math.max(12, processors * 3), pool.getMaxPoolSize());
  }

  @Test
  void stockCommonExecutorShouldUseCommonPrefix() {
    ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) asyncConfig.stockCommonExecutor();
    assertEquals("stock-common-async-", pool.getThreadNamePrefix());
  }
}
