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

















@Slf4j
@Configuration
@ConditionalOnClass(RedissonClient.class)
@ConditionalOnProperty(name = "cloud.redisson.enabled", havingValue = "true", matchIfMissing = true)
public class RedissonClientConfiguration {

    @Value("${spring.data.redis.host:127.0.0.1}")
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

    





    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(RedissonClient.class)
    public RedissonClient redissonClient() {
        Config config = new Config();

        
        if (StringUtils.hasText(clusterNodes)) {
            configureCluster(config);
        } else if (StringUtils.hasText(sentinelNodes) && StringUtils.hasText(sentinelMaster)) {
            configureSentinel(config);
        } else {
            configureSingle(config);
        }

        
        config.setThreads(threads);
        config.setNettyThreads(nettyThreads);

        
        if ("json".equalsIgnoreCase(codec)) {
            config.setCodec(new JsonJacksonCodec());
        }
        

        RedissonClient redissonClient = Redisson.create(config);

        


        return redissonClient;
    }

    


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
                .setPingConnectionInterval(1000)  
                .setKeepAlive(true);              

        if (StringUtils.hasText(password)) {
            config.useSingleServer().setPassword(password);
        }

        
    }

    


    private void configureCluster(Config config) {
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
                .setScanInterval(2000);  

        if (StringUtils.hasText(password)) {
            config.useClusterServers().setPassword(password);
        }

        
    }

    


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

        
    }

    


    private int parseTimeout() {
        if (!StringUtils.hasText(timeoutStr)) {
            return 3000;
        }

        String cleanValue = timeoutStr.replaceAll("ms$", "").replaceAll("s$", "").trim();

        try {
            int value = Integer.parseInt(cleanValue);
            
            if (timeoutStr.contains("s") && !timeoutStr.contains("ms")) {
                value *= 1000;
            }
            return value;
        } catch (NumberFormatException e) {
            log.warn("鈿狅笍 瑙ｆ瀽瓒呮椂鏃堕棿澶辫触: {}, 浣跨敤榛樿鍊?000ms", timeoutStr);
            return 3000;
        }
    }

    


    private String getMode() {
        if (StringUtils.hasText(clusterNodes)) {
            return "闆嗙兢妯″紡";
        } else if (StringUtils.hasText(sentinelNodes) && StringUtils.hasText(sentinelMaster)) {
            return "鍝ㄥ叺妯″紡";
        } else {
            return "鍗曟満妯″紡";
        }
    }
}
