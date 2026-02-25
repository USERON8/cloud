package com.cloud.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncConfigTest {

    private final AsyncConfig asyncConfig = new AsyncConfig();

    @Test
    void authAsyncExecutorShouldUseConfiguredPrefix() {
        ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) asyncConfig.authAsyncExecutor();
        assertEquals("auth-async-", pool.getThreadNamePrefix());
        assertEquals(3, pool.getCorePoolSize());
        assertEquals(8, pool.getMaxPoolSize());
    }

    @Test
    void authTokenExecutorShouldBeScalable() {
        ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) asyncConfig.authTokenExecutor();
        assertEquals("auth-token-", pool.getThreadNamePrefix());
        assertTrue(pool.getMaxPoolSize() >= pool.getCorePoolSize());
    }
}
