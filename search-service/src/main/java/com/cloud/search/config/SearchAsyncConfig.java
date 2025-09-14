package com.cloud.search.config;

import com.cloud.common.config.BaseAsyncConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 搜索服务异步配置类
 * 继承基础异步配置类，提供搜索服务专用的线程池配置
 *
 * @author what's up
 */
@Slf4j
@Configuration
@EnableAsync
public class SearchAsyncConfig extends BaseAsyncConfig {

    /**
     * 搜索查询专用线程池
     * 根据搜索查询的特点进行优化配置
     */
    @Bean("searchQueryExecutor")
    public Executor searchQueryExecutor() {
        // 搜索查询需要快速响应，使用较多的核心线程
        int processors = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                Math.max(6, processors),
                processors * 4,
                800,
                "search-query-"
        );
        executor.initialize();

        log.info("搜索查询线程池初始化完成");
        return executor;
    }

    /**
     * 搜索索引操作专用线程池
     * 用于处理ES索引的创建、更新、删除等操作
     */
    @Bean("searchIndexExecutor")
    public Executor searchIndexExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                4,
                16,
                400,
                "search-index-"
        );
        executor.initialize();

        log.info("搜索索引操作线程池初始化完成");
        return executor;
    }

    /**
     * 数据同步专用线程池
     * 用于处理从数据库到ES的数据同步
     */
    @Bean("dataSyncExecutor")
    public Executor dataSyncExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                3,
                12,
                200,
                "data-sync-"
        );
        executor.initialize();

        log.info("数据同步线程池初始化完成");
        return executor;
    }

    /**
     * 搜索聚合专用线程池
     * 用于处理复杂的聚合查询和统计分析
     */
    @Bean("searchAggregationExecutor")
    public Executor searchAggregationExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                2,
                8,
                100,
                "search-aggregation-"
        );
        executor.initialize();

        log.info("搜索聚合线程池初始化完成");
        return executor;
    }

    /**
     * 搜索建议专用线程池
     * 用于处理搜索自动完成和建议功能
     */
    @Bean("searchSuggestionExecutor")
    public Executor searchSuggestionExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                2,
                6,
                150,
                "search-suggestion-"
        );
        executor.initialize();

        log.info("搜索建议线程池初始化完成");
        return executor;
    }

    /**
     * 搜索分析专用线程池
     * 用于处理搜索行为分析和热词统计
     */
    @Bean("searchAnalysisExecutor")
    public Executor searchAnalysisExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                1,
                4,
                80,
                "search-analysis-"
        );
        executor.initialize();

        log.info("搜索分析线程池初始化完成");
        return executor;
    }

    /**
     * 通用异步线程池
     * 用于处理其他异步任务
     */
    @Bean("searchCommonAsyncExecutor")
    public Executor searchCommonAsyncExecutor() {
        ThreadPoolTaskExecutor executor = createCommonAsyncExecutor();
        executor.initialize();

        log.info("搜索服务通用异步线程池初始化完成");
        return executor;
    }
}
