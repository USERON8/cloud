package com.cloud.common.config;

import com.cloud.common.aspect.DistributedLockAspect;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;









@Slf4j
@AutoConfiguration
@ConditionalOnClass({RedissonClient.class})
@ConditionalOnProperty(name = "cloud.distributed-lock.enabled", havingValue = "true", matchIfMissing = true)
@EnableAspectJAutoProxy
public class DistributedLockAutoConfiguration {

    





    @Bean
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnMissingBean(DistributedLockAspect.class)
    public DistributedLockAspect distributedLockAspect(RedissonClient redissonClient) {
        
        return new DistributedLockAspect(redissonClient);
    }
}
