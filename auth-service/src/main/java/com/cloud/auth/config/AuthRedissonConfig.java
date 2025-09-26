package com.cloud.auth.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

/**
 * 认证服务专用 Redisson 配置
 * 覆盖 common-module 的配置，避免配置冲突
 *
 * @author what's up
 * @since 1.0.0
 */
@Configuration
public class AuthRedissonConfig {

    @Value("${spring.data.redis.host:127.0.0.1}")
    private String host;
    
    @Value("${spring.data.redis.port:6379}")
    private int port;
    
    @Value("${spring.data.redis.password:}")
    private String password;
    
    @Value("${spring.data.redis.database:0}")
    private int database;

    @Bean("authRedissonClient")
    @Primary
    public RedissonClient redissonClient() {
        Config config = new Config();
        
        String address = "redis://" + host + ":" + port;
        config.useSingleServer()
                .setAddress(address)
                .setPassword(StringUtils.hasText(password) ? password : null)
                .setDatabase(database)
                .setConnectionPoolSize(32)
                .setConnectionMinimumIdleSize(8)
                .setIdleConnectionTimeout(10000)
                .setConnectTimeout(10000)
                .setTimeout(10000)
                .setRetryAttempts(3)
                .setRetryInterval(1000)
                .setSubscriptionsPerConnection(5)
                .setClientName("auth-service");
        
        return Redisson.create(config);
    }
}
