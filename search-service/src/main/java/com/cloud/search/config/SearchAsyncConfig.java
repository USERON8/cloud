package com.cloud.search.config;

import com.cloud.common.config.BaseAsyncConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 搜索服务异步配置类
 * 继承基础异步配置类，提供搜索服务专用的线程池配置
 * <p>
 * 搜索服务特点：
 * - 高并发的搜索请求
 * - 复杂的ES查询操作
 * - 搜索索引更新
 * - 搜索数据分析
 *
 * @author what's up
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableAsync
@ConditionalOnProperty(name = "search.async.enabled", havingValue = "true", matchIfMissing = true)
public class SearchAsyncConfig extends BaseAsyncConfig {

    /**
     * 搜索查询异步线程池
     * 专门用于搜索查询相关的异步处理
     * 高并发查询优化
     */
    @Bean("searchQueryExecutor")
    public Executor searchQueryExecutor() {
        ThreadPoolTaskExecutor executor = createQueryExecutor("search-query-");
        executor.initialize();

        log.info("✅ 搜索查询线程池初始化完成");
        return executor;
    }

    /**
     * 搜索索引异步线程池
     * 专门用于搜索索引的创建和更新
     * 写操作优化，保证索引一致性
     */
    @Bean("searchIndexExecutor")
    public Executor searchIndexExecutor() {
        ThreadPoolTaskExecutor executor = createWriteExecutor("search-index-");
        executor.initialize();

        log.info("✅ 搜索索引线程池初始化完成");
        return executor;
    }

    /**
     * ES批量操作异步线程池
     * 专门用于Elasticsearch批量操作
     * 优化批量索引和批量查询性能
     */
    @Bean("searchESBatchExecutor")
    public Executor searchESBatchExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                4,
                12,
                800,
                "search-es-batch-"
        );
        executor.initialize();

        log.info("✅ 搜索ES批量操作线程池初始化完成 - 核心线程数: 4, 最大线程数: 12, 队列容量: 800");
        return executor;
    }

    /**
     * 搜索建议异步线程池
     * 专门用于搜索建议和自动补全
     * 低延迟高响应优化
     */
    @Bean("searchSuggestionExecutor")
    public Executor searchSuggestionExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                3,
                8,
                200,
                "search-suggestion-"
        );
        executor.initialize();

        log.info("✅ 搜索建议线程池初始化完成 - 核心线程数: 3, 最大线程数: 8, 队列容量: 200");
        return executor;
    }

    /**
     * 搜索统计异步线程池
     * 专门用于搜索数据统计和分析
     * CPU密集型任务优化
     */
    @Bean("searchStatisticsExecutor")
    @ConditionalOnProperty(name = "search.statistics.enabled", havingValue = "true", matchIfMissing = true)
    public Executor searchStatisticsExecutor() {
        ThreadPoolTaskExecutor executor = createCPUExecutor("search-stats-");
        executor.initialize();

        log.info("✅ 搜索统计线程池初始化完成");
        return executor;
    }

    /**
     * 热门搜索异步线程池
     * 专门用于热门搜索词统计和更新
     * 定时任务和缓存更新优化
     */
    @Bean("searchHotKeywordExecutor")
    public Executor searchHotKeywordExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                2,
                4,
                100,
                "search-hot-"
        );
        executor.initialize();

        log.info("✅ 热门搜索线程池初始化完成 - 核心线程数: 2, 最大线程数: 4, 队列容量: 100");
        return executor;
    }

    /**
     * 搜索缓存异步线程池
     * 专门用于搜索缓存的更新和清理
     * 缓存管理优化
     */
    @Bean("searchCacheExecutor")
    public Executor searchCacheExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                2,
                6,
                150,
                "search-cache-"
        );
        executor.initialize();

        log.info("✅ 搜索缓存线程池初始化完成 - 核心线程数: 2, 最大线程数: 6, 队列容量: 150");
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

        log.info("✅ 搜索服务通用异步线程池初始化完成");
        return executor;
    }
}
