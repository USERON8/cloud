package com.cloud.common.config;

import com.cloud.common.aspect.DistributedLockAspect;
import com.cloud.common.lock.RedissonLockManager;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 分布式锁自动配置类
 * 自动配置分布式锁相关的Bean和AOP切面
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass({RedissonClient.class})
@ConditionalOnProperty(name = "cloud.distributed-lock.enabled", havingValue = "true", matchIfMissing = true)
@EnableAspectJAutoProxy
public class DistributedLockAutoConfiguration {

    /**
     * 配置分布式锁AOP切面
     *
     * @param redissonClient Redisson客户端
     * @return 分布式锁切面
     */
    @Bean
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnMissingBean(DistributedLockAspect.class)
    public DistributedLockAspect distributedLockAspect(RedissonClient redissonClient) {
        log.info("✅ 配置分布式锁AOP切面");
        return new DistributedLockAspect(redissonClient);
    }

    /**
     * 配置Redisson锁管理器
     *
     * @param redissonClient Redisson客户端
     * @return Redisson锁管理器
     */
    @Bean
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnMissingBean(RedissonLockManager.class)
    public RedissonLockManager redissonLockManager(RedissonClient redissonClient) {
        log.info("✅ 配置Redisson锁管理器");
        return new RedissonLockManager(redissonClient);
    }
}
