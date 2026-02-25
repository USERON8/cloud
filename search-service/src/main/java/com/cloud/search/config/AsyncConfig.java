package com.cloud.search.config;

import com.cloud.common.config.BaseAsyncConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;














@Slf4j
@Configuration
@EnableAsync
@ConditionalOnProperty(name = "app.async.enabled", havingValue = "true", matchIfMissing = true)
public class AsyncConfig extends BaseAsyncConfig {

    




    @Bean("searchQueryExecutor")
    public Executor searchQueryExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = createConfiguredExecutor(
                "searchQueryExecutor",
                Math.max(4, processors),
                processors * 4,
                500,
                60,
                "search-query-"
        );
        executor.initialize();

        
        return executor;
    }

    




    @Bean("searchIndexExecutor")
    public Executor searchIndexExecutor() {
        ThreadPoolTaskExecutor executor = createConfiguredExecutor(
                "searchIndexExecutor",
                4,
                16,
                500,
                60,
                "search-index-"
        );
        executor.initialize();

        
        return executor;
    }

    




    @Bean("searchESBatchExecutor")
    public Executor searchESBatchExecutor() {
        ThreadPoolTaskExecutor executor = createConfiguredExecutor(
                "searchESBatchExecutor",
                4,
                12,
                800,
                60,
                "search-es-batch-"
        );
        executor.initialize();

        
        return executor;
    }

    




    @Bean("searchSuggestionExecutor")
    public Executor searchSuggestionExecutor() {
        ThreadPoolTaskExecutor executor = createConfiguredExecutor(
                "searchSuggestionExecutor",
                3,
                8,
                200,
                60,
                "search-suggestion-"
        );
        executor.initialize();

        
        return executor;
    }

    




    @Bean("searchStatisticsExecutor")
    @ConditionalOnProperty(name = "search.statistics.enabled", havingValue = "true", matchIfMissing = true)
    public Executor searchStatisticsExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = createConfiguredExecutor(
                "searchStatisticsExecutor",
                processors,
                processors + 1,
                100,
                60,
                "search-stats-"
        );
        executor.initialize();

        
        return executor;
    }

    




    @Bean("searchHotKeywordExecutor")
    public Executor searchHotKeywordExecutor() {
        ThreadPoolTaskExecutor executor = createConfiguredExecutor(
                "searchHotKeywordExecutor",
                2,
                4,
                100,
                60,
                "search-hot-"
        );
        executor.initialize();

        
        return executor;
    }

    




    @Bean("searchCacheExecutor")
    public Executor searchCacheExecutor() {
        ThreadPoolTaskExecutor executor = createConfiguredExecutor(
                "searchCacheExecutor",
                2,
                6,
                150,
                60,
                "search-cache-"
        );
        executor.initialize();

        
        return executor;
    }

    



    @Bean("searchCommonAsyncExecutor")
    public Executor searchCommonAsyncExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = createConfiguredExecutor(
                "searchCommonAsyncExecutor",
                Math.max(2, processors / 2),
                processors * 2,
                200,
                60,
                "common-async-"
        );
        executor.initialize();

        
        return executor;
    }
}
