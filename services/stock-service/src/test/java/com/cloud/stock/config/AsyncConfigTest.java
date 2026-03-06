package com.cloud.stock.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncConfigTest {

    private final AsyncConfig asyncConfig = new AsyncConfig();

    @Test
    void stockQueryExecutorShouldScaleWithCpu() {
        ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) asyncConfig.stockQueryExecutor();
        assertEquals("stock-query-", pool.getThreadNamePrefix());
        assertTrue(pool.getCorePoolSize() >= 8);
        assertTrue(pool.getMaxPoolSize() >= pool.getCorePoolSize());
    }

    @Test
    void stockCommonExecutorShouldUseCommonPrefix() {
        ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) asyncConfig.stockCommonExecutor();
        assertEquals("stock-common-async-", pool.getThreadNamePrefix());
    }
}
