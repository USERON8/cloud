package com.cloud.order.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AsyncConfigTest {

    private final AsyncConfig asyncConfig = new AsyncConfig();

    @Test
    void orderAsyncExecutorShouldMatchDefaults() {
        ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) asyncConfig.orderAsyncExecutor();
        assertEquals("order-async-", pool.getThreadNamePrefix());
        assertEquals(4, pool.getCorePoolSize());
        assertEquals(8, pool.getMaxPoolSize());
    }

    @Test
    void orderPaymentExecutorShouldBeProvisioned() {
        ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) asyncConfig.orderPaymentExecutor();
        assertEquals("order-payment-", pool.getThreadNamePrefix());
        assertEquals(4, pool.getCorePoolSize());
    }
}
