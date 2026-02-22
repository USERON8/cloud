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
@ConditionalOnProperty(name = "search.async.enabled", havingValue = "true", matchIfMissing = true)
public class AsyncConfig extends BaseAsyncConfig {

    




    @Bean("searchQueryExecutor")
    public Executor searchQueryExecutor() {
        ThreadPoolTaskExecutor executor = createQueryExecutor("search-query-");
        executor.initialize();

        
        return executor;
    }

    




    @Bean("searchIndexExecutor")
    public Executor searchIndexExecutor() {
        ThreadPoolTaskExecutor executor = createWriteExecutor("search-index-");
        executor.initialize();

        
        return executor;
    }

    




    @Bean("searchESBatchExecutor")
    public Executor searchESBatchExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                4,
                12,
                800,
                "search-es-batch-"
        );
        executor.initialize();

        
        return executor;
    }

    




    @Bean("searchSuggestionExecutor")
    public Executor searchSuggestionExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                3,
                8,
                200,
                "search-suggestion-"
        );
        executor.initialize();

        
        return executor;
    }

    




    @Bean("searchStatisticsExecutor")
    @ConditionalOnProperty(name = "search.statistics.enabled", havingValue = "true", matchIfMissing = true)
    public Executor searchStatisticsExecutor() {
        ThreadPoolTaskExecutor executor = createCPUExecutor("search-stats-");
        executor.initialize();

        
        return executor;
    }

    




    @Bean("searchHotKeywordExecutor")
    public Executor searchHotKeywordExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                2,
                4,
                100,
                "search-hot-"
        );
        executor.initialize();

        
        return executor;
    }

    




    @Bean("searchCacheExecutor")
    public Executor searchCacheExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                2,
                6,
                150,
                "search-cache-"
        );
        executor.initialize();

        
        return executor;
    }

    



    @Bean("searchCommonAsyncExecutor")
    public Executor searchCommonAsyncExecutor() {
        ThreadPoolTaskExecutor executor = createCommonAsyncExecutor();
        executor.initialize();

        
        return executor;
    }
}
