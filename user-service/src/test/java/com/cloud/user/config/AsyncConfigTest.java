package com.cloud.user.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncConfigTest {

    private final AsyncConfig asyncConfig = new AsyncConfig();

    @Test
    void userQueryExecutorShouldBeProvisioned() {
        ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) asyncConfig.userQueryExecutor();
        assertEquals("user-query-", pool.getThreadNamePrefix());
        assertTrue(pool.getMaxPoolSize() >= pool.getCorePoolSize());
    }

    @Test
    void userCommonExecutorShouldUseCommonPrefix() {
        ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) asyncConfig.userCommonAsyncExecutor();
        assertEquals("common-async-", pool.getThreadNamePrefix());
    }
}
