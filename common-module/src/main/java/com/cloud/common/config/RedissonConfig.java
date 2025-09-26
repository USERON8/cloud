package com.cloud.common.config;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Redisson配置类
 * 提供Redisson客户端的配置和初始化
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Value("${spring.data.redis.database:0}")
    private int database;

    @Value("${spring.data.redis.timeout:3000}")
    private String timeoutStr;

    @Value("${spring.data.redis.lettuce.pool.max-active:8}")
    private int maxActive;

    @Value("${spring.data.redis.lettuce.pool.max-idle:8}")
    private int maxIdle;

    @Value("${spring.data.redis.lettuce.pool.min-idle:0}")
    private int minIdle;

    @Value("${spring.data.redis.lettuce.pool.max-wait:-1}")
    private String maxWaitStr;
    
    private int getTimeout() {
        return parseIntValue(timeoutStr, 3000);
    }
    
    private long getMaxWait() {
        return parseLongValue(maxWaitStr, -1L);
    }
    
    private int parseIntValue(String value, int defaultValue) {
        if (value == null || value.trim().isEmpty()) return defaultValue;
        String cleanValue = value.replaceAll("ms$", "").replaceAll("s$", "");
        try {
            return Integer.parseInt(cleanValue);
        } catch (NumberFormatException e) {
            log.warn("解析整数配置失败，使用默认值: {} -> {}", value, defaultValue);
            return defaultValue;
        }
    }
    
    private long parseLongValue(String value, long defaultValue) {
        if (value == null || value.trim().isEmpty()) return defaultValue;
        String cleanValue = value.replaceAll("ms$", "").replaceAll("s$", "");
        try {
            return Long.parseLong(cleanValue);
        } catch (NumberFormatException e) {
            log.warn("解析长整数配置失败，使用默认值: {} -> {}", value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * 创建Redisson客户端
     *
     * @return RedissonClient实例
     */
    @Bean
    @ConditionalOnMissingBean(RedissonClient.class)
    public RedissonClient redissonClient() {
        Config config = new Config();

        // 单机模式配置
        String redisUrl = String.format("redis://%s:%d", host, port);
        config.useSingleServer()
                .setAddress(redisUrl)
                .setDatabase(database)
                .setTimeout(getTimeout())
                .setConnectionMinimumIdleSize(minIdle)
                .setConnectionPoolSize(maxActive)
                .setIdleConnectionTimeout(10000)
                .setConnectTimeout(getTimeout())
                .setRetryAttempts(3)
                .setRetryInterval(1500);

        // 设置密码（如果有）
        if (StringUtils.hasText(password)) {
            config.useSingleServer().setPassword(password);
        }

        // 设置编解码器
        config.setCodec(new org.redisson.codec.JsonJacksonCodec());

        // 设置线程池配置
        config.setThreads(16);
        config.setNettyThreads(32);

        // 设置锁的看门狗超时时间（默认30秒）
        config.setLockWatchdogTimeout(30000);

        // 创建Redisson客户端
        RedissonClient redissonClient = Redisson.create(config);

        log.info("✅ Redisson客户端初始化成功 - Redis地址: {}, 数据库: {}", redisUrl, database);

        return redissonClient;
    }

    /**
     * 集群模式配置（可选）
     * 当配置了集群节点时启用
     */
    @Bean
    @ConditionalOnProperty(name = "spring.data.redis.cluster.nodes")
    @ConditionalOnMissingBean(RedissonClient.class)
    public RedissonClient redissonClusterClient(
            @Value("${spring.data.redis.cluster.nodes}") String clusterNodes,
            @Value("${spring.data.redis.cluster.max-redirects:3}") int maxRedirects) {

        Config config = new Config();

        // 解析集群节点
        String[] nodes = clusterNodes.split(",");
        String[] addresses = new String[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            addresses[i] = "redis://" + nodes[i].trim();
        }

        // 集群模式配置
        config.useClusterServers()
                .addNodeAddress(addresses)
                .setTimeout(getTimeout())
                .setMasterConnectionMinimumIdleSize(minIdle)
                .setMasterConnectionPoolSize(maxActive)
                .setSlaveConnectionMinimumIdleSize(minIdle)
                .setSlaveConnectionPoolSize(maxActive)
                .setIdleConnectionTimeout(10000)
                .setConnectTimeout(getTimeout())
                .setRetryAttempts(3)
                .setRetryInterval(1500);

        // 设置密码（如果有）
        if (StringUtils.hasText(password)) {
            config.useClusterServers().setPassword(password);
        }

        // 设置编解码器
        config.setCodec(new org.redisson.codec.JsonJacksonCodec());

        // 设置线程池配置
        config.setThreads(16);
        config.setNettyThreads(32);

        // 设置锁的看门狗超时时间
        config.setLockWatchdogTimeout(30000);

        RedissonClient redissonClient = Redisson.create(config);

        log.info("✅ Redisson集群客户端初始化成功 - 集群节点: {}", clusterNodes);

        return redissonClient;
    }

    /**
     * 哨兵模式配置（可选）
     * 当配置了哨兵节点时启用
     */
    @Bean
    @ConditionalOnProperty(name = "spring.data.redis.sentinel.nodes")
    @ConditionalOnMissingBean(RedissonClient.class)
    public RedissonClient redissonSentinelClient(
            @Value("${spring.data.redis.sentinel.nodes}") String sentinelNodes,
            @Value("${spring.data.redis.sentinel.master}") String masterName) {

        Config config = new Config();

        // 解析哨兵节点
        String[] nodes = sentinelNodes.split(",");
        String[] addresses = new String[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            addresses[i] = "redis://" + nodes[i].trim();
        }

        // 哨兵模式配置
        config.useSentinelServers()
                .setMasterName(masterName)
                .addSentinelAddress(addresses)
                .setDatabase(database)
                .setTimeout(getTimeout())
                .setMasterConnectionMinimumIdleSize(minIdle)
                .setMasterConnectionPoolSize(maxActive)
                .setSlaveConnectionMinimumIdleSize(minIdle)
                .setSlaveConnectionPoolSize(maxActive)
                .setIdleConnectionTimeout(10000)
                .setConnectTimeout(getTimeout())
                .setRetryAttempts(3)
                .setRetryInterval(1500);

        // 设置密码（如果有）
        if (StringUtils.hasText(password)) {
            config.useSentinelServers().setPassword(password);
        }

        // 设置编解码器
        config.setCodec(new org.redisson.codec.JsonJacksonCodec());

        // 设置线程池配置
        config.setThreads(16);
        config.setNettyThreads(32);

        // 设置锁的看门狗超时时间
        config.setLockWatchdogTimeout(30000);

        RedissonClient redissonClient = Redisson.create(config);

        log.info("✅ Redisson哨兵客户端初始化成功 - 主节点: {}, 哨兵节点: {}", masterName, sentinelNodes);

        return redissonClient;
    }
}
