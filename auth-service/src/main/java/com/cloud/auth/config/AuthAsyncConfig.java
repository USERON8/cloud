package com.cloud.auth.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 认证服务异步配置类
 * 继承基础异步配置类，提供认证服务专用的线程池配置
 * 
 * 认证服务特点：
 * - 高并发的认证请求处理
 * - Token生成和验证
 * - 用户登录状态管理
 * - 安全日志记录
 *
 * @author what's up
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableAsync
@ConditionalOnProperty(name = "auth.async.enabled", havingValue = "true", matchIfMissing = true)
public class AuthAsyncConfig {

    /**
     * 认证业务异步线程池
     * 专门用于认证相关的异步业务处理
     * 如：Token刷新、用户状态更新、登录日志记录等
     */
    @Bean("authAsyncExecutor")
    public Executor authAsyncExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                3,
                8,
                300,
                "auth-async-"
        );
        executor.initialize();

        log.info("✅ 认证异步线程池初始化完成 - 核心线程数: 3, 最大线程数: 8, 队列容量: 300");
        return executor;
    }

    /**
     * Token处理异步线程池
     * 专门用于Token相关的异步处理
     * 如：Token生成、验证、刷新、黑名单管理等
     */
    @Bean("authTokenExecutor")
    public Executor authTokenExecutor() {
        ThreadPoolTaskExecutor executor = createQueryExecutor("auth-token-");
        executor.initialize();

        log.info("✅ Token处理线程池初始化完成");
        return executor;
    }

    /**
     * 安全日志异步线程池
     * 专门用于安全相关的日志记录
     * 如：登录日志、安全事件、审计日志等
     */
    @Bean("authSecurityLogExecutor")
    public Executor authSecurityLogExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                2,
                6,
                500,
                "auth-security-log-"
        );
        executor.initialize();

        log.info("✅ 安全日志线程池初始化完成 - 核心线程数: 2, 最大线程数: 6, 队列容量: 500");
        return executor;
    }

    /**
     * OAuth2处理异步线程池
     * 专门用于OAuth2相关的异步处理
     * 如：第三方登录、授权码处理、客户端管理等
     */
    @Bean("authOAuth2Executor")
    @ConditionalOnProperty(name = "auth.oauth2.enabled", havingValue = "true", matchIfMissing = true)
    public Executor authOAuth2Executor() {
        ThreadPoolTaskExecutor executor = createIOExecutor("auth-oauth2-");
        executor.initialize();

        log.info("✅ OAuth2处理线程池初始化完成");
        return executor;
    }

    /**
     * 用户会话管理异步线程池
     * 专门用于用户会话相关的异步处理
     * 如：会话创建、更新、清理、统计等
     */
    @Bean("authSessionExecutor")
    public Executor authSessionExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                2,
                5,
                200,
                "auth-session-"
        );
        executor.initialize();

        log.info("✅ 用户会话管理线程池初始化完成 - 核心线程数: 2, 最大线程数: 5, 队列容量: 200");
        return executor;
    }

    /**
     * 通用异步线程池
     * 用于处理其他异步任务
     */
    @Bean("authCommonAsyncExecutor")
    public Executor authCommonAsyncExecutor() {
        ThreadPoolTaskExecutor executor = createCommonAsyncExecutor();
        executor.initialize();

        log.info("✅ 认证服务通用异步线程池初始化完成");
        return executor;
    }

    /**
     * 创建线程池任务执行器的工厂方法
     * 提供统一的线程池配置模板
     */
    private ThreadPoolTaskExecutor createThreadPoolTaskExecutor(int corePoolSize, int maxPoolSize,
                                                                int queueCapacity, String threadNamePrefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 基本配置
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);

        // 高级配置
        executor.setKeepAliveSeconds(60);
        executor.setAllowCoreThreadTimeOut(false);

        // 拒绝策略：调用者运行策略（保证任务不丢失）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 优雅关闭配置
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        return executor;
    }

    /**
     * 创建通用异步执行器
     * 适用于各服务的通用异步任务
     */
    private ThreadPoolTaskExecutor createCommonAsyncExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        return createThreadPoolTaskExecutor(
                Math.max(2, processors / 2),
                processors * 2,
                200,
                "common-async-"
        );
    }

    /**
     * 创建高并发查询专用线程池
     * 适用于查询密集型业务
     */
    private ThreadPoolTaskExecutor createQueryExecutor(String threadNamePrefix) {
        int processors = Runtime.getRuntime().availableProcessors();
        return createThreadPoolTaskExecutor(
                Math.max(4, processors),
                processors * 4,
                500,
                threadNamePrefix
        );
    }

    /**
     * 创建IO密集型线程池
     * 适用于文件上传、网络请求等IO密集型任务
     */
    private ThreadPoolTaskExecutor createIOExecutor(String threadNamePrefix) {
        int processors = Runtime.getRuntime().availableProcessors();
        return createThreadPoolTaskExecutor(
                processors * 2,
                processors * 4,
                300,
                threadNamePrefix
        );
    }
}
