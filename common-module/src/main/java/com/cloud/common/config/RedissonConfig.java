package com.cloud.common.config;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
// import org.springframework.util.StringUtils; // 不再使用，改为自定义检查

/**
 * Redisson基础配置类
 * 提供Redisson客户端的配置和初始化模板
 * 注意：此类仅用于继承，不作为Spring Bean
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Slf4j
public abstract class RedissonConfig {

    protected String host = "localhost";
    protected int port = 6379;
    protected String password = "";
    protected int database = 0;
    protected String timeoutStr = "3000";
    protected int maxActive = 8;
    protected int maxIdle = 8;
    protected int minIdle = 0;
    protected String maxWaitStr = "-1";
    
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
     * 创建Redisson客户端工厂方法
     *
     * @return RedissonClient实例
     */
    protected RedissonClient createRedissonClient() {
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
        if (password != null && !password.trim().isEmpty()) {
            config.useSingleServer().setPassword(password);
        }

        // 不设置 codec，使用 Redisson 默认的 JsonJackson 编解码器
        // 注意：Redisson 使用高效的编解码器处理数据序列化
        
        // 设置线程池配置
        config.setThreads(16);
        config.setNettyThreads(32);

        // 创建Redisson客户端
        RedissonClient redissonClient = Redisson.create(config);

        log.info("✅ Redisson客户端初始化成功 - Redis地址: {}, 数据库: {}", redisUrl, database);

        return redissonClient;
    }

    /**
     * 集群模式配置工厂方法
     * 当配置了集群节点时使用
     */
    protected RedissonClient createRedissonClusterClient(String clusterNodes, int maxRedirects) {

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
        if (password != null && !password.trim().isEmpty()) {
            config.useClusterServers().setPassword(password);
        }

        // 不设置 codec，使用 Redisson 默认的编解码器
        
        // 设置线程池配置
        config.setThreads(16);
        config.setNettyThreads(32);

        RedissonClient redissonClient = Redisson.create(config);

        log.info("✅ Redisson集群客户端初始化成功 - 集群节点: {}", clusterNodes);

        return redissonClient;
    }

    /**
     * 哨兵模式配置工厂方法
     * 当配置了哨兵节点时使用
     */
    protected RedissonClient createRedissonSentinelClient(String sentinelNodes, String masterName) {

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
        if (password != null && !password.trim().isEmpty()) {
            config.useSentinelServers().setPassword(password);
        }

        // 不设置 codec，使用 Redisson 默认的编解码器
        
        // 设置线程池配置
        config.setThreads(16);
        config.setNettyThreads(32);

        RedissonClient redissonClient = Redisson.create(config);

        log.info("✅ Redisson哨兵客户端初始化成功 - 主节点: {}, 哨兵节点: {}", masterName, sentinelNodes);

        return redissonClient;
    }
}