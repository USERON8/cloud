package com.cloud.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncConfigTest {

    private final AsyncConfig asyncConfig = new AsyncConfig();

    @Test
    void gatewayRouteExecutorShouldBeConfigured() {
        Executor executor = asyncConfig.gatewayRouteExecutor();
        ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) executor;
        assertEquals("gateway-route-", pool.getThreadNamePrefix());
        assertTrue(pool.getCorePoolSize() >= 4);
        assertTrue(pool.getMaxPoolSize() >= pool.getCorePoolSize());
    }

    @Test
    void gatewayLogExecutorShouldUseLogPrefix() {
        ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) asyncConfig.gatewayLogExecutor();
        assertEquals("gateway-log-", pool.getThreadNamePrefix());
        assertEquals(2, pool.getCorePoolSize());
        assertEquals(4, pool.getMaxPoolSize());
    }
}
