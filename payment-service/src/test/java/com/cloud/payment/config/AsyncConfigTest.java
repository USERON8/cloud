package com.cloud.payment.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AsyncConfigTest {

    private final AsyncConfig asyncConfig = new AsyncConfig();

    @Test
    void paymentAsyncExecutorShouldMatchDefaults() {
        ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) asyncConfig.paymentAsyncExecutor();
        assertEquals("payment-async-", pool.getThreadNamePrefix());
        assertEquals(3, pool.getCorePoolSize());
        assertEquals(8, pool.getMaxPoolSize());
    }

    @Test
    void paymentStatisticsExecutorShouldUseStatsPrefix() {
        ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) asyncConfig.paymentStatisticsExecutor();
        assertEquals("payment-stats-", pool.getThreadNamePrefix());
    }
}
