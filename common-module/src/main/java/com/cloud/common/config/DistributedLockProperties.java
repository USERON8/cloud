package com.cloud.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁配置属性
 * 提供分布式锁的全局配置选项
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "cloud.distributed-lock")
public class DistributedLockProperties {

    /**
     * 是否启用分布式锁功能
     */
    private boolean enabled = true;

    /**
     * 默认锁等待时间
     */
    private long defaultWaitTime = 3;

    /**
     * 默认锁持有时间
     */
    private long defaultLeaseTime = 10;

    /**
     * 默认时间单位
     */
    private TimeUnit defaultTimeUnit = TimeUnit.SECONDS;

    /**
     * 锁键前缀
     */
    private String keyPrefix = "distributed-lock";

    /**
     * 是否启用锁监控
     */
    private boolean monitorEnabled = true;

    /**
     * 监控统计间隔（秒）
     */
    private long monitorInterval = 60;

    /**
     * 是否打印锁操作日志
     */
    private boolean logEnabled = true;

    /**
     * 锁操作日志级别
     */
    private String logLevel = "DEBUG";

    /**
     * 是否启用锁性能统计
     */
    private boolean performanceStatsEnabled = false;

    /**
     * 性能统计保留时间（小时）
     */
    private int performanceStatsRetentionHours = 24;

    /**
     * Redisson配置
     */
    private RedissonProperties redisson = new RedissonProperties();

    /**
     * Redisson配置属性
     */
    @Data
    public static class RedissonProperties {

        /**
         * 看门狗超时时间（毫秒）
         */
        private long lockWatchdogTimeout = 30000;

        /**
         * 线程池大小
         */
        private int threads = 16;

        /**
         * Netty线程池大小
         */
        private int nettyThreads = 32;

        /**
         * 连接超时时间（毫秒）
         */
        private int connectTimeout = 10000;

        /**
         * 命令执行超时时间（毫秒）
         */
        private int timeout = 3000;

        /**
         * 重试次数
         */
        private int retryAttempts = 3;

        /**
         * 重试间隔（毫秒）
         */
        private int retryInterval = 1500;

        /**
         * 连接池最小空闲连接数
         */
        private int connectionMinimumIdleSize = 10;

        /**
         * 连接池大小
         */
        private int connectionPoolSize = 64;

        /**
         * 空闲连接超时时间（毫秒）
         */
        private int idleConnectionTimeout = 10000;
    }
}
