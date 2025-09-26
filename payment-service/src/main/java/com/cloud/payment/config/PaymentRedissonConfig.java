package com.cloud.payment.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 支付服务专用 Redisson 配置
 * 覆盖 common-module 的配置，避免配置冲突
 */
@Configuration
public class PaymentRedissonConfig {

    @Value("${redis.host:127.0.0.1}")
    private String host;
    
    @Value("${redis.port:6379}")
    private int port;
    
    @Value("${redis.password:}")
    private String password;
    
    @Value("${redis.database:1}")
    private int database;

    @Bean
    @Primary
    public RedissonClient redissonClient() {
        Config config = new Config();
        
        String address = "redis://" + host + ":" + port;
        config.useSingleServer()
                .setAddress(address)
                .setPassword(password.isEmpty() ? null : password)
                .setDatabase(database)
                .setConnectionPoolSize(32)
                .setConnectionMinimumIdleSize(8)
                .setIdleConnectionTimeout(10000)
                .setConnectTimeout(10000)
                .setTimeout(10000)
                .setRetryAttempts(3)
                .setRetryInterval(1000)
                .setSubscriptionsPerConnection(5)
                .setClientName("payment-service");
        
        return Redisson.create(config);
    }
}
