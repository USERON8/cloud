package com.cloud.product.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AsyncConfigTest {

    private final AsyncConfig asyncConfig = new AsyncConfig();

    @Test
    void productAsyncExecutorShouldMatchDefaults() {
        ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) asyncConfig.productAsyncExecutor();
        assertEquals("product-async-", pool.getThreadNamePrefix());
        assertEquals(2, pool.getCorePoolSize());
        assertEquals(4, pool.getMaxPoolSize());
    }
}
