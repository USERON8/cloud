package com.cloud.common.config;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;











@Slf4j
public abstract class RedissonConfig {

    protected String host = "127.0.0.1";
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
            log.warn("瑙ｆ瀽鏁存暟閰嶇疆澶辫触锛屼娇鐢ㄩ粯璁ゅ€? {} -> {}", value, defaultValue);
            return defaultValue;
        }
    }

    private long parseLongValue(String value, long defaultValue) {
        if (value == null || value.trim().isEmpty()) return defaultValue;
        String cleanValue = value.replaceAll("ms$", "").replaceAll("s$", "");
        try {
            return Long.parseLong(cleanValue);
        } catch (NumberFormatException e) {
            log.warn("瑙ｆ瀽闀挎暣鏁伴厤缃け璐ワ紝浣跨敤榛樿鍊? {} -> {}", value, defaultValue);
            return defaultValue;
        }
    }

    




    protected RedissonClient createRedissonClient() {
        Config config = new Config();

        
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

        
        if (password != null && !password.trim().isEmpty()) {
            config.useSingleServer().setPassword(password);
        }

        
        

        
        config.setThreads(16);
        config.setNettyThreads(32);

        
        RedissonClient redissonClient = Redisson.create(config);

        

        return redissonClient;
    }

    



    protected RedissonClient createRedissonClusterClient(String clusterNodes, int maxRedirects) {

        Config config = new Config();

        
        String[] nodes = clusterNodes.split(",");
        String[] addresses = new String[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            addresses[i] = "redis://" + nodes[i].trim();
        }

        
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

        
        if (password != null && !password.trim().isEmpty()) {
            config.useClusterServers().setPassword(password);
        }

        

        
        config.setThreads(16);
        config.setNettyThreads(32);

        RedissonClient redissonClient = Redisson.create(config);

        

        return redissonClient;
    }

    



    protected RedissonClient createRedissonSentinelClient(String sentinelNodes, String masterName) {

        Config config = new Config();

        
        String[] nodes = sentinelNodes.split(",");
        String[] addresses = new String[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            addresses[i] = "redis://" + nodes[i].trim();
        }

        
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

        
        if (password != null && !password.trim().isEmpty()) {
            config.useSentinelServers().setPassword(password);
        }

        

        
        config.setThreads(16);
        config.setNettyThreads(32);

        RedissonClient redissonClient = Redisson.create(config);

        

        return redissonClient;
    }
}
