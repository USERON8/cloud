package com.cloud.common.config;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Redisson客户端统一配置类
 * <p>
 * 提供Redisson客户端的统一配置,支持:
 * - 单机模式(默认)
 * - 集群模式
 * - 哨兵模式
 * <p>
 * 配置说明:
 * - 单机模式: 配置 spring.data.redis.host 和 spring.data.redis.port
 * - 集群模式: 配置 spring.redis.cluster.nodes
 * - 哨兵模式: 配置 spring.redis.sentinel.nodes 和 spring.redis.sentinel.master
 *
 * @author CloudDevAgent
 * @since 2025-10-12
 */
@Slf4j
@Configuration
@ConditionalOnClass(RedissonClient.class)
@ConditionalOnProperty(name = "cloud.redisson.enabled", havingValue = "true", matchIfMissing = true)
public class RedissonClientConfiguration {

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Value("${spring.data.redis.database:0}")
    private int database;

    @Value("${spring.data.redis.timeout:3000ms}")
    private String timeoutStr;

    @Value("${spring.data.redis.lettuce.pool.max-active:50}")
    private int maxActive;

    @Value("${spring.data.redis.lettuce.pool.max-idle:20}")
    private int maxIdle;

    @Value("${spring.data.redis.lettuce.pool.min-idle:5}")
    private int minIdle;

    @Value("${spring.redis.cluster.nodes:}")
    private String clusterNodes;

    @Value("${spring.redis.cluster.max-redirects:3}")
    private int maxRedirects;

    @Value("${spring.redis.sentinel.nodes:}")
    private String sentinelNodes;

    @Value("${spring.redis.sentinel.master:}")
    private String sentinelMaster;

    @Value("${cloud.redisson.threads:16}")
    private int threads;

    @Value("${cloud.redisson.netty-threads:32}")
    private int nettyThreads;

    @Value("${cloud.redisson.codec:json}")
    private String codec;

    /**
     * 创建RedissonClient Bean
     * 根据配置自动选择单机/集群/哨兵模式
     *
     * @return RedissonClient实例
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(RedissonClient.class)
    public RedissonClient redissonClient() {
        Config config = new Config();

        // 选择模式
        if (StringUtils.hasText(clusterNodes)) {
            configureCluster(config);
        } else if (StringUtils.hasText(sentinelNodes) && StringUtils.hasText(sentinelMaster)) {
            configureSentinel(config);
        } else {
            configureSingle(config);
        }

        // 通用配置
        config.setThreads(threads);
        config.setNettyThreads(nettyThreads);

        // 配置编解码器
        if ("json".equalsIgnoreCase(codec)) {
            config.setCodec(new JsonJacksonCodec());
        }
        // 默认使用Redisson的FstCodec(快速序列化)

        RedissonClient redissonClient = Redisson.create(config);

        log.info("✅ Redisson客户端初始化成功 - 模式: {}, 线程池: {}/{}",
                getMode(), threads, nettyThreads);

        return redissonClient;
    }

    /**
     * 配置单机模式
     */
    private void configureSingle(Config config) {
        String redisUrl = String.format("redis://%s:%d", host, port);

        config.useSingleServer()
                .setAddress(redisUrl)
                .setDatabase(database)
                .setTimeout(parseTimeout())
                .setConnectionMinimumIdleSize(minIdle)
                .setConnectionPoolSize(maxActive)
                .setIdleConnectionTimeout(10000)
                .setConnectTimeout(parseTimeout())
                .setRetryAttempts(3)
                .setRetryInterval(1500)
                .setPingConnectionInterval(1000)  // 心跳检测
                .setKeepAlive(true);              // 保持连接

        if (StringUtils.hasText(password)) {
            config.useSingleServer().setPassword(password);
        }

        log.info("🔧 Redisson单机模式配置 - Redis: {}:{}, DB: {}", host, port, database);
    }

    /**
     * 配置集群模式
     */
    private void configureCl uster(Config config) {
        String[] nodes = clusterNodes.split(",");
        String[] addresses = new String[nodes.length];

        for (int i = 0; i < nodes.length; i++) {
            String node = nodes[i].trim();
            addresses[i] = node.startsWith("redis://") ? node : "redis://" + node;
        }

        config.useClusterServers()
                .addNodeAddress(addresses)
                .setTimeout(parseTimeout())
                .setMasterConnectionMinimumIdleSize(minIdle)
                .setMasterConnectionPoolSize(maxActive)
                .setSlaveConnectionMinimumIdleSize(minIdle)
                .setSlaveConnectionPoolSize(maxActive)
                .setIdleConnectionTimeout(10000)
                .setConnectTimeout(parseTimeout())
                .setRetryAttempts(3)
                .setRetryInterval(1500)
                .setPingConnectionInterval(1000)
                .setKeepAlive(true)
                .setScanInterval(2000);  // 集群扫描间隔

        if (StringUtils.hasText(password)) {
            config.useClusterServers().setPassword(password);
        }

        log.info("🔧 Redisson集群模式配置 - 节点数: {}", addresses.length);
    }

    /**
     * 配置哨兵模式
     */
    private void configureSentinel(Config config) {
        String[] nodes = sentinelNodes.split(",");
        String[] addresses = new String[nodes.length];

        for (int i = 0; i < nodes.length; i++) {
            String node = nodes[i].trim();
            addresses[i] = node.startsWith("redis://") ? node : "redis://" + node;
        }

        config.useSentinelServers()
                .setMasterName(sentinelMaster)
                .addSentinelAddress(addresses)
                .setDatabase(database)
                .setTimeout(parseTimeout())
                .setMasterConnectionMinimumIdleSize(minIdle)
                .setMasterConnectionPoolSize(maxActive)
                .setSlaveConnectionMinimumIdleSize(minIdle)
                .setSlaveConnectionPoolSize(maxActive)
                .setIdleConnectionTimeout(10000)
                .setConnectTimeout(parseTimeout())
                .setRetryAttempts(3)
                .setRetryInterval(1500)
                .setPingConnectionInterval(1000)
                .setKeepAlive(true)
                .setScanInterval(2000);

        if (StringUtils.hasText(password)) {
            config.useSentinelServers().setPassword(password);
        }

        log.info("🔧 Redisson哨兵模式配置 - Master: {}, 哨兵数: {}", sentinelMaster, addresses.length);
    }

    /**
     * 解析超时时间
     */
    private int parseTimeout() {
        if (!StringUtils.hasText(timeoutStr)) {
            return 3000;
        }

        String cleanValue = timeoutStr.replaceAll("ms$", "").replaceAll("s$", "").trim();

        try {
            int value = Integer.parseInt(cleanValue);
            // 如果原始值包含"s"但不包含"ms",则转换为毫秒
            if (timeoutStr.contains("s") && !timeoutStr.contains("ms")) {
                value *= 1000;
            }
            return value;
        } catch (NumberFormatException e) {
            log.warn("⚠️ 解析超时时间失败: {}, 使用默认值3000ms", timeoutStr);
            return 3000;
        }
    }

    /**
     * 获取当前模式
     */
    private String getMode() {
        if (StringUtils.hasText(clusterNodes)) {
            return "集群模式";
        } else if (StringUtils.hasText(sentinelNodes) && StringUtils.hasText(sentinelMaster)) {
            return "哨兵模式";
        } else {
            return "单机模式";
        }
    }
}
