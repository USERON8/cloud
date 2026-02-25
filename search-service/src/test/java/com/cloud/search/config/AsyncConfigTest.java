package com.cloud.search.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AsyncConfigTest {

    private final AsyncConfig asyncConfig = new AsyncConfig();

    @Test
    void searchQueryExecutorShouldUseQueryPrefix() {
        ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) asyncConfig.searchQueryExecutor();
        assertEquals("search-query-", pool.getThreadNamePrefix());
    }

    @Test
    void searchSuggestionExecutorShouldUseSuggestionPrefix() {
        ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) asyncConfig.searchSuggestionExecutor();
        assertEquals("search-suggestion-", pool.getThreadNamePrefix());
    }
}
